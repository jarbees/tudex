package tuda.campus.mobile.managers

import tuda.campus.mobile.dataStore
import tuda.campus.mobile.managers.tucanManager.Course
import tuda.campus.mobile.managers.tucanManager.EventDate
import tuda.campus.mobile.managers.tucanManager.Exam
import tuda.campus.mobile.managers.tucanManager.ExamRegistration
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.tucanManager.TimeSlot
import tuda.campus.mobile.managers.tucanManager.TucanDataSource
import tuda.campus.mobile.managers.tucanManager.TucanDummySource
import tuda.campus.mobile.managers.tucanManager.TucanNetworkSource
import tuda.campus.mobile.persistentStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.String
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock

object TucanManager {
    private var dataSource: TucanDataSource = TucanNetworkSource()
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    private val _todayTimeSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val todayTimeSlots = _todayTimeSlots.asStateFlow()

    private val _timeSlotStatus = MutableStateFlow(RequestStatus.READY)
    val timeSlotStatus = _timeSlotStatus.asStateFlow()

    private val _courses: MutableStateFlow<List<List<List<Course>>>> = dataStore.persistentStateFlow(keyName = "courses", defaultValue = emptyList())
    val courses = _courses.asStateFlow()

    private val _courseStatus = MutableStateFlow(RequestStatus.READY)
    val courseStatus = _courseStatus.asStateFlow()

    private val _viewedExam: MutableStateFlow<Exam?> = MutableStateFlow(null)

    private val _exams: MutableStateFlow<List<List<Exam>>> = dataStore.persistentStateFlow(keyName = "exams", defaultValue = emptyList())
    val exams = _exams.asStateFlow()

    private val _examStatus = MutableStateFlow(RequestStatus.READY)
    val examStatus = _examStatus.asStateFlow()


    // Supposed to be Pair<locale, day -> dates>
    val datesList: StateFlow<Pair<String, List<List<EventDate>>>?> =
        combine(_courses, _exams) { courses, exams ->
            try {
                calculateDates(courses, exams)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // TODO: Log exception
                null
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val dateStatus: StateFlow<RequestStatus> =
        combine(courseStatus, examStatus) { course, exam ->
            when {
                course == RequestStatus.LOADING || exam == RequestStatus.LOADING -> RequestStatus.LOADING
                course == RequestStatus.FAILED || exam == RequestStatus.FAILED -> RequestStatus.FAILED
                else -> RequestStatus.READY
            }
        }.stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = RequestStatus.READY
        )


    private val _examRegistrations = MutableStateFlow<List<List<List<ExamRegistration>>>>(emptyList())
    val examRegistrations = _examRegistrations.asStateFlow()

    private val _examRegStatus = MutableStateFlow(RequestStatus.READY)
    val examRegStatus = _examRegStatus.asStateFlow()


    // General Functions

    suspend fun checkToken() {
        if (IdentityManager.isTucanCredentialsExpired()) {
            _examRegistrations.value = emptyList()
            if (IdentityManager.tuLoginStatus.value != TULoginState.DEMO) {
                IdentityManager.updateTucanCredentials()
            }
            coroutineScope.launch {
                updateExamRegistrations()
            }
        }
    }

    suspend fun initializeStates() = coroutineScope {
        if (IdentityManager.tuLoginStatus.value == TULoginState.DEMO) {
            dataSource = TucanDummySource()
        }

        launch { updateTodayTimeSlots() }
        launch { updateExams() }
        launch { updateExamRegistrations() }
        launch { updateCourses() }
    }

    fun resetStates() {
        _todayTimeSlots.value = emptyList()
        _courses.value = emptyList()
        _exams.value = emptyList()
        _examRegistrations.value = emptyList()

        _courseStatus.value = RequestStatus.READY
        _timeSlotStatus.value = RequestStatus.READY
        _examStatus.value = RequestStatus.READY
        _examRegStatus.value = RequestStatus.READY
    }

    private suspend fun <T : Any> refresh(
        status: MutableStateFlow<RequestStatus>,
        fetch: suspend () -> T?,
        publish: (T) -> Unit
    ) {
        status.value = RequestStatus.LOADING

        try {
            val result = fetch()

            if (result == null) {
                status.value = RequestStatus.FAILED
                return
            }

            publish(result)
            status.value = RequestStatus.READY
        } catch (e: CancellationException) {
            status.value = RequestStatus.FAILED
            throw e
        } catch (e: Exception) {
            // TODO: Log exceptions
            status.value = RequestStatus.FAILED
        }
    }


    // Specific functions


    // The date must be in the format "dd.mm.yyyy" and day is weekday in full name
    private suspend fun getDaySchedule(date: String, day: String): List<TimeSlot>? = dataSource.getDaySchedule(date, day)

    suspend fun updateTodayTimeSlots() {
        refresh(
            status = _timeSlotStatus,
            fetch = {
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                val day = now.day.toString().padStart(2, '0')
                val month = now.month.number.toString().padStart(2, '0')
                val year = now.year.toString()

                val date = "$day.$month.$year"   // "dd.MM.yyyy"

                val weekDay = now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

                getDaySchedule(date, weekDay)
            },
            publish = { timeSlots ->
                if (timeSlots != _todayTimeSlots.value) {
                    _todayTimeSlots.value = timeSlots
                }
            }
        )
    }



    // List is used as 3D Structure: semester -> module -> course
    // if only one course per module or module per semester, it is still a list
    private suspend fun getCourses(): List<List<List<Course>>>? = dataSource.getCourses(currentCourses = courses.value)

    suspend fun updateCourses() {
        refresh(
            status = _courseStatus,
            fetch = ::getCourses,
            publish = { potentialCourses ->
                if (potentialCourses != _courses.value) {
                    _courses.value = potentialCourses
                }
            }
        )
    }


    private fun calculateDates(courses: List<List<List<Course>>>, exams: List<List<Exam>>): Pair<String, List<List<EventDate>>>? {
        fun detectLocale(date: String): String {
            val germanPrefixes = listOf("Mo,", "Di,", "Mi,", "Do,", "Fr,", "Sa,", "So,")
            val englishPrefixes = listOf("Mon,", "Tue,", "Wed,", "Thu,", "Fri,", "Sat,", "Sun,")

            return when {
                germanPrefixes.any { date.startsWith(it) } -> "DE"
                englishPrefixes.any { date.startsWith(it) } -> "EN"
                else -> throw IllegalArgumentException("Unknown date format: $date")
            }
        }

        fun String.toLocalDate(): LocalDate? {
            // specifically, for May/Mai, there is no "." after the month
            val parts = this.removeSuffix(".").split(", ", ". ", " ")
            if (parts.size != 4) return null

            //val weekday = parts[0]
            val day = parts[1]
            val monthStr = parts[2]
            val year = parts[3]

            val month = when (monthStr.lowercase()) {
                "jan" -> 1
                "feb" -> 2
                "mar" -> 3
                "mär" -> 3
                "apr" -> 4
                "may" -> 5
                "mai" -> 5
                "jun" -> 6
                "jul" -> 7
                "aug" -> 8
                "sep" -> 9
                "oct" -> 10
                "okt" -> 10
                "nov" -> 11
                "dec" -> 12
                "dez" -> 12
                else -> error("Invalid month: $monthStr")
            }

            return LocalDate(
                year = year.toInt(),
                month = month,
                day = day.toInt()
            )
        }

        fun updateDatesList(lists: List<List<EventDate>>): Pair<String, List<List<EventDate>>>? {
            val firstNonEmptyList = lists.firstOrNull { it.isNotEmpty() } ?: return null

            val locale = detectLocale(firstNonEmptyList[0].date)

            fun <T> Iterator<T>.nextOrNull(): T? = if (hasNext()) next() else null

            val iterators = lists.map { it.iterator() }
            val current = iterators.map { it.nextOrNull() }.toMutableList()
            val result = mutableListOf<List<EventDate>>()

            while (true) {
                // TODO: implement some kind of way to display those EventDates where EventDate.date is not a correctly formatted date

                val minDate = current
                    .filterNotNull()
                    .mapNotNull { it.date.toLocalDate() }
                    .minOfOrNull { it }
                    ?: break

                val minIndex = current.indexOfFirst { it?.date?.toLocalDate() == minDate }
                if (minIndex == -1) break

                // Current date for grouping
                val currentDate = current[minIndex]!!.date

                // Collect all CourseDates that have this date
                val group = mutableListOf<EventDate>()
                for (i in current.indices) {
                    while (current[i] != null && current[i]!!.date == currentDate) {
                        group += current[i]!!
                        current[i] = iterators[i].nextOrNull()
                    }
                }

                // Sort inside group by time
                val sortedGroup = group.sortedWith(
                    compareBy<EventDate> { it.startTime }
                        .thenBy { it.endTime }
                )

                result += sortedGroup
            }

            return Pair(locale, result)
        }

        // Only build dates for newest semester
        val courseDates: List<List<EventDate>> =
            courses                                 // List<List<List<Course>>> or Semesters>Modules>Courses
                .firstOrNull()                      // -> List<List<Course>>    or -> Modules>Courses
                ?.flatten()                         // -> List<Course>          or -> Courses
                ?.flatMap { course ->               // -> List<List<Date>>      or -> DatesLists>Dates
                    val dates: List<EventDate>? = course.courseDetails?.dates
                    val plenaryDates: List<EventDate>? = course.courseDetails?.plenaryDates

                    listOfNotNull(dates, plenaryDates)
                }
                ?: emptyList()

        val examDates: List<EventDate> = exams
            .firstOrNull()
            ?.mapNotNull { exam ->
                // The assumption here is that containing digit implies valid date, which is probably not true
                if (exam.examinationDateTime.any { it.isDigit() }) {
                    val parts = exam.examinationDateTime.split(" ")

                    val date = parts.dropLast(1).joinToString(" ")

                    val time = parts.last()
                    val startTime = time.split("-").first()
                    val endTime = time.split("-").last()

                    val startTimeMins =
                        startTime.split(":").first().toInt() * 60 + startTime.split(":").last()
                            .toInt()
                    val endTimeMins =
                        endTime.split(":").first().toInt() * 60 + endTime.split(":").last().toInt()

                    return@mapNotNull EventDate.ExamDate(
                        courseName = exam.courseName,
                        examType = exam.examType,
                        courseID = exam.courseID,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        startTimeMins = startTimeMins,
                        endTimeMins = endTimeMins
                    )

                } else {
                    return@mapNotNull null
                }
            }
            ?: emptyList()

        val groupedExamDates: MutableMap<String, List<EventDate>> = examDates
            .groupBy { it.date }
            .mapValues { (_, dates) ->
                dates.sortedWith(compareBy<EventDate> { it.startTimeMins }.thenBy { it.endTimeMins })
            }.toMutableMap()

        val dates: MutableList<List<EventDate>> = courseDates.mapNotNull { dates ->
            val dayDate = dates.firstOrNull()?.date ?: return@mapNotNull null

            groupedExamDates[dayDate]?.let { examDate ->
                val mergedDay = (dates + examDate)
                    .sortedWith(compareBy<EventDate> { it.startTimeMins }.thenBy { it.endTimeMins })

                groupedExamDates.remove(dayDate)

                return@mapNotNull mergedDay
            }

            return@mapNotNull dates
        }.toMutableList()

        for (entry in groupedExamDates) {
            entry.value.firstOrNull()?.date?.toLocalDate()?.let { date ->
                val firstIndexAfterDay = dates.indexOfFirst { dayDates ->
                    dayDates.firstOrNull()?.date?.toLocalDate()?.let { dayDate ->
                        dayDate > date
                    } ?: run {
                        false
                    }
                }

                if (firstIndexAfterDay != -1) {
                    dates.add(firstIndexAfterDay, entry.value)
                } else {
                    dates.add(entry.value)
                }
            }
        }

        return updateDatesList(dates)
    }


    private suspend fun getExams(): List<List<Exam>>? = dataSource.getExams()

    fun updateViewedExam(exam: Exam?) {
        _viewedExam.value = exam
    }

    suspend fun updateExams() {
        refresh(
            status = _examStatus,
            fetch = ::getExams,
            publish = { potentialExams ->
                val viewedPotentialExam = potentialExams.flatten().find { it.id == _viewedExam.value?.id }
                viewedPotentialExam?.gradeStats = _viewedExam.value?.gradeStats

                if (potentialExams != _exams.value) {
                    _exams.value = potentialExams
                }
            }
        )
    }

    suspend fun updateExamGradeStats(exam: Exam) = dataSource.updateExamGradeStats(exam)



    // List is used as 3D Structure: semester -> course -> examRegistrations
    private suspend fun getExamRegistrations(): List<List<List<ExamRegistration>>>? = dataSource.getExamRegistrations()

    // provides additionalInfo but not the vars of ExamRegistration
    suspend fun updateExamRegistrations() {
        _examRegistrations.value = emptyList()
        refresh(
            status = _examRegStatus,
            fetch = ::getExamRegistrations,
            publish = { registrations ->
                _examRegistrations.value = registrations
            }
        )
    }



    // Updates the ExamRegistration in-place
    suspend fun updateExamRegistrationDetail(examRegistration: ExamRegistration) = dataSource.updateExamRegistrationDetail(examRegistration)

    suspend fun getExamModifyPrompt(isRegister: Boolean, arguments: String): String? = dataSource.getExamModifyPrompt(isRegister, arguments)

    suspend fun confirmExamModify(registerID: String): Boolean = dataSource.confirmExamModify(registerID)
}

