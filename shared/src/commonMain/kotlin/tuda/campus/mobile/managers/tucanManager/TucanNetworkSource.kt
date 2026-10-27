package tuda.campus.mobile.managers.tucanManager

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import com.fleeksoft.ksoup.select.Elements
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.TucanManager
import tuda.campus.mobile.safeGet
import tuda.campus.mobile.safeRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.orEmpty

class TucanNetworkSource: TucanDataSource {
    private val client = HttpClient {
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 3)
            exponentialDelay()
        }
    }

    private data class Semester (
        val name: String,
        val id: String
    )

    // NOTE: All functions must when possible use getPage() for TUCaN requests.
    // The only (current) exceptions are getPage(), createToken() and confirmExamModify()

    // Helper Functions

    private suspend fun getPage(usesSessionID: Boolean, pagename: String, arguments: String): HttpResponse? {
        TucanManager.checkToken()

        val tucanCredentials = IdentityManager.getTucanCredentials()

        val response: HttpResponse =  client.safeGet("https://www.tucan.tu-darmstadt.de/scripts/mgrqispi.dll") {
            parameter("APPNAME", "CampusNet")
            parameter("PRGNAME", pagename)
            if (usesSessionID) {
                parameter("ARGUMENTS", "${tucanCredentials?.sessionId},$arguments")
            }
            else {
                parameter("ARGUMENTS", "arguments")
            }
            header("Cookie", "${tucanCredentials?.cookie?.name}=${tucanCredentials?.cookie?.value}")
        } ?: return null

        return response
    }

    private fun getSemesters(document: Document): Pair<Semester, List<Semester>> {
        val semesterSelector = document.getElementById("semester")
        val semesters = mutableListOf<Semester>()       // <Name, ID>
        var currentSemester = Semester("", "")

        semesterSelector?.let {
            val semesterOptions: Elements = it.select("option")

            for (option in semesterOptions) {
                val id = option.attr("value")
                val name = option.text()

                if (option.attr("selected") == "selected") {
                    currentSemester = Semester(name, id)
                }

                // 999 = All semesters option
                if (id != "999") {
                    semesters.add(Semester(name, id))
                }
            }
        }

        return Pair(currentSemester, semesters)
    }

    private data class ModuleNameInfo(
        val courseID: String,
        val name: String
    )

    private suspend fun getAllModuleNames(): List<ModuleNameInfo>? {
        val response: HttpResponse =
            getPage(true, "MYMODULES", "-N000000,-N999")
                ?: return null
        val document: Document = Ksoup.parse(response.bodyAsText())

        val tableRows = document.select("table tbody tr").filter { row ->
            row.hasClass("tbdata")
        }

        val names = mutableListOf<ModuleNameInfo>()

        for (row in tableRows) {
            val columns = row.select("td")
            if (columns.isNotEmpty() && columns.size > 1) {
                val module = ModuleNameInfo(
                    courseID = columns[1].text(),
                    name = columns[2].select("a").text()
                )

                names.add(module)
            }
        }

        return names
    }


    // General functions

    // The date must be in the format "dd.mm.yyyy" and day is weekday in full name
    override suspend fun getDaySchedule(date: String, day: String): List<TimeSlot>? {
        // usually "-N000000,-A$date,-A,-N1" is used to get an entire week.
        // Using N<other number> instead gives a single day while omitting ",-A,-N2" would remove exams
        val response: HttpResponse =
            getPage(true, "SCHEDULERPRINT", "-N000000,-A$date,-A,-N2")
                ?: return null

        val document: Document = Ksoup.parse(response.bodyAsText())
        val tableRows = document.select("table.tb tr")

        val newTimeSlots: MutableList<TimeSlot> = mutableListOf()

        if (tableRows.size <= 2) { // Only header and "Keine Veranstaltungen" row
            val message: String = tableRows[1].select("td").first()!!.text()
        } else {
            for (i in 2 until tableRows.size) {
                val row = tableRows[i]
                val columns = row.select("td")

                if (columns.isNotEmpty()) {
                    val timeSlot = TimeSlot(
                        courseID = columns[0].text(),
                        courseName = columns[1].text(),
                        instructor = columns[2].text(),
                        time = columns[3].text(),
                        room = columns[4].text(),
                        day = day
                    )
                    newTimeSlots.add(timeSlot)
                }
            }
        }

        return newTimeSlots
    }

    // List is used as 3D Structure: semester -> module -> course
    // if only one course per module or module per semester, it is still a list
    override suspend fun getCourses(currentCourses: List<List<List<Course>>>): List<List<List<Course>>>? {
        fun findCourses(document: Document, semesterName: String): List<Course> {
            val courses = mutableListOf<Course>()
            val tableRows = document.select("table tbody tr").filter { row ->
                row.hasClass("tbdata")
            }

            // TODO: similar pages show only one column when no exam is present, might be broken
            for (row in tableRows) {
                val columns = row.select("td")

                if (columns.isNotEmpty() && columns.size > 1) {
                    val course = Course(
                        semester = semesterName,
                        courseID = columns[1].text(),
                        courseName = columns[2].select("a").text(), // courseName is inside an <a></a>
                        detailLinkArgs = columns[2].select("a").attr("href").substringAfter("-N000000,")
                    )

                    courses.add(course)
                }
            }

            return courses
        }

        // semesters -> courses
        suspend fun getBasicCourses(): MutableList<List<Course>>? {
            // This page behaves abnormally. Normally passing no arguments loads the current semester
            // This page instead loads the last viewed semester in this session.
            // It also at seemingly random loads <All>-semester.
            // Default behavior is restored by passing nonsense -N1 argument
            val response: HttpResponse =
                getPage(true, "PROFCOURSES", "-N000000, -N1")
                    ?: return null
            val document: Document = Ksoup.parse(response.bodyAsText())

            val semesterResult = getSemesters(document)
            val currentSemester = semesterResult.first
            val semesters = semesterResult.second

            val currentCourses = findCourses(document, currentSemester.name)

            val courses = coroutineScope {
                val deferredFetchedCourses = semesters.map { semester ->
                    async {
                        if (semester == currentSemester) {
                            emptyList()
                        } else {
                            val asyncResponse =
                                getPage(true, "PROFCOURSES", "-N000000,-N${semester.id}")
                                    ?: return@async null
                            val asyncDocument: Document = Ksoup.parse(asyncResponse.bodyAsText())

                            findCourses(asyncDocument, semester.name)   // This is what is being mapped to
                        }
                    }
                }

                val fetchedCourses: MutableList<List<Course>> =
                    deferredFetchedCourses.awaitAll()
                        .map { it ?: return@coroutineScope null }
                        .toMutableList()

                val currentSemesterIndex = semesters.indexOf(currentSemester)
                fetchedCourses[currentSemesterIndex] = currentCourses

                val filteredCourses = fetchedCourses.filter { it.isNotEmpty() }.toMutableList()
                return@coroutineScope filteredCourses
            }

            return courses
        }

        suspend fun insertCourseDetails(course: Course): Course? {
            fun parseDetails(doc: Element): String {
                val table = doc.select("table caption:contains(Veranstaltungsdetails)")
                    .first()?.parent() ?: doc.select("table caption:contains(Course offering details)")
                    .first()?.parent() ?: return ""

                return table.select("p").joinToString("\n\n") { p ->
                    val bold = p.select("b").text()
                    val rest = p.text().removePrefix(bold).trim()
                    if (bold.isNotEmpty()) "$bold $rest" else p.text()
                }.trim()
            }

            fun parseGroupList(document: Document): List<CourseGroup> {
                val groupList = document.select(".tb .dl-ul-listview li")

                return groupList.map { li ->
                    CourseGroup(
                        title = li.select(".dl-ul-li-headline").text(),
                        teachers = li.select("p").drop(1).joinToString("\n") { it.text() }.trim(),
                        detailLinkArg = li.select(".dl-link a").attr("href")
                            .substringAfter("-N000000").takeIf { it.isNotBlank() }
                    )
                }
            }

            fun parseCourseRegDeadline(doc: Element): List<CourseRegDeadline> {
                val table = doc.select("table caption:contains(Anmeldefristen)")
                    .first()?.parent() ?: doc.select("table caption:contains(Registration periods)")
                    .first()?.parent() ?: return emptyList()

                val rows = table.select("tr").drop(1) // Skip header

                return rows.mapNotNull { tr ->
                    val tds = tr.select("td")
                    if (tds.size < 6) return@mapNotNull null

                    CourseRegDeadline(
                        phase = tds[0].text(),
                        block = tds[1].text(),
                        startDate = tds[2].text(),
                        endRegisterDate = tds[3].text(),
                        endDeregisterDate = tds[4].text(),
                        endListenerDate = tds[5].text()
                    )
                }
            }

            fun parseDates(doc: Element): List<EventDate.CourseDate>? {
                fun convertToMinutes(time: String): Int {
                    val parts = time.split(":")
                    if (parts.size != 2) throw IllegalArgumentException("Time must be in HH:mm format")

                    val hours = parts[0].toInt()
                    val minutes = parts[1].toInt()

                    return hours * 60 + minutes
                }

                val table = doc.select("table caption:contains(Termine)")
                    .first()?.parent() ?: doc.select("table caption:contains(Appointments)")
                    .first()?.parent() ?: return emptyList()

                val rows = table.select("tr").drop(1) // Skip header

                return rows.mapNotNull { tr ->
                    val tds = tr.select("td")
                    if (tds.size < 6) return@mapNotNull null

                    val date = tds[1].text()

                    if (date.endsWith("*")) return@mapNotNull null

                    val startTime = tds[2].text()
                    val endTime = tds[3].text()

                    EventDate.CourseDate(
                        courseName = course.courseName,
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        startTimeMins = convertToMinutes(startTime),
                        endTimeMins = convertToMinutes(endTime),
                        room = tds[4].select("a").text(),
                        teachers = tds[5].text()
                    )
                }.takeIf { it.isNotEmpty() }
            }

            fun parseModuleIDs(document: Element): List<String> {
                val table = document.select("table caption:contains(Enthalten in Modulen)")
                    .first()?.parent() ?: document.select("table caption:contains(Contained in modules)")
                    .first()?.parent() ?: return emptyList()

                val modules = table.select("td").filter { row ->
                    row.hasClass("tbdata")
                }

                return modules.mapNotNull { module ->
                    module.text().split(" ").firstOrNull()
                }.distinct()
            }

            val response: HttpResponse = getPage(true, "COURSEDETAILS", "-N000000,${course.detailLinkArgs}")
                ?: return null
            val document: Document = Ksoup.parse(response.bodyAsText())

            val optPlenaryElement = document.select(
                """a:matches((?i).*Show plenary course offering.*|.*Plenumsveranstaltung anzeigen.*)"""
            ).first()

            val plenaryDates: List<EventDate.CourseDate>? = optPlenaryElement?.let { plenaryElement ->
                val plenaryArgs = plenaryElement.attr("href")
                    .substringAfter("-N000000,")
                    .takeIf { it.isNotBlank() }
                    ?: return@let null

                val response: HttpResponse =
                    getPage(true, "COURSEDETAILS", "-N000000,$plenaryArgs")
                        ?: return@insertCourseDetails null
                val document = Ksoup.parse(response.bodyAsText())

                return@let parseDates(document)
            }

            val courseDetails = CourseDetails(
                details = parseDetails(document),
                groups = parseGroupList(document),
                regDeadlines = parseCourseRegDeadline(document),
                dates = parseDates(document),
                plenaryDates = plenaryDates,
                moduleIDs = parseModuleIDs(document)
            )

            return course.copy(courseDetails = courseDetails)
        }

        val courses: List<List<List<Course>>> = coroutineScope {
            val deferredCourses = async { getBasicCourses() }
            val deferredModuleNames = async { getAllModuleNames() }

            val plainCourses = deferredCourses.await() ?: return@coroutineScope null
            val moduleNames = deferredModuleNames.await() ?: return@coroutineScope null

            val detailedCourses: List<List<Course>> = plainCourses.mapIndexed { index, courses ->
                // Only update details for current semester, as others are seldom used
                // TODO: This will fail if someone has a course in a future semester
                if (index == 0) {
                    val deferredDetailedCourses = courses.map { course ->
                        async { insertCourseDetails(course) ?: course.copy() }
                    }

                    return@mapIndexed deferredDetailedCourses.awaitAll()
                    // For all other courses, check if their details already exists,
                    // If yes, copy them, if no, get new CourseDetails
                } else {
                    val existingCourseDetailsMap =
                        currentCourses
                            .flatten()
                            .flatten()
                            .mapNotNull { course ->
                                course.courseDetails?.let { details ->
                                    course.id to details
                                }
                            }
                            .toMap()

                    val deferredDetailedCourses = courses.map { course ->
                        async {
                            val existingDetails = existingCourseDetailsMap[course.id]
                            if (existingDetails != null) {
                                return@async course.copy(courseDetails = existingDetails)
                            } else {
                                return@async insertCourseDetails(course) ?: course.copy()
                            }
                        }
                    }

                    deferredDetailedCourses.awaitAll()
                }
            }

            val sortedCourses: List<List<List<Course>>> = detailedCourses.map { semesterCourses ->
                // This works by pairing every Course with a ModuleName it should be assigned to
                // First it tries to match by courseID than by the moduleIDs listed in courseDetails page.
                // If multiple entries match, arbitrarily take longest
                // If no module matches, pretend it belongs to a module named after itself
                val grouped: List<Pair<Course, String>> = semesterCourses.map { course ->
                    val idMatches = moduleNames.filter { module ->
                        val moduleID = module.courseID.takeWhile { it.isLetterOrDigit() || it == '-' }
                        course.courseID.startsWith(moduleID)
                    }

                    val chosenModule = idMatches.maxByOrNull { it.courseID.length }
                        ?: run {
                            course.courseDetails?.moduleIDs?.let { moduleIDs ->
                                val matches = moduleNames.filter { moduleIDs.contains(it.courseID) }
                                return@run matches.maxByOrNull { it.courseID.length }
                            }
                        }

                    chosenModule?.let { module ->
                        course.copy(moduleName = module.name) to module.name
                    } ?: run {
                        course.copy(moduleName = course.courseName) to course.courseName
                    }
                }

                grouped
                    .groupBy({ it.second }, { it.first })
                    .values
                    .filter { it.isNotEmpty() }
            }

            return@coroutineScope sortedCourses
        } ?: return null

        return courses
    }

    override suspend fun getExams(): List<List<Exam>>? {
        fun findExams(document: Document, semesterName: String): List<Exam> {
            val exams = mutableListOf<Exam>()
            val tableRows = document.select("table tbody tr")

            // TODO: similar pages show only one column when no exam is present, might be broken
            for (row in tableRows) {
                val columns = row.select("td")

                if (columns.isNotEmpty() && columns.size > 1) {
                    val exam = Exam(
                        semester = semesterName,
                        courseID = columns[0].text(),
                        courseName = columns[1].select("a").text(),     // courseName is inside an <a></a>
                        examType = columns[2].text(),
                        examinationDateTime = columns[3].text(),
                    )

                    exams.add(exam)
                }
            }

            return exams
        }

        data class GradeInfo(
            val courseID: String,
            val examNameType: String,
            var grade: String,
            var gradeName: String,
            var gradeSheetArgs: String?
        )

        fun findGrades(document: Document): List<GradeInfo> {
            val grades = mutableListOf<GradeInfo>()
            val tableRows = document.select("table tbody tr")

            // TODO: similar pages show only one column when no exam is present, might be broken
            for (row in tableRows) {
                val columns = row.select("td")

                if (columns.isNotEmpty() && columns.size > 1) {
                    val gradeInfo = GradeInfo(
                        courseID = columns[0].text().trim().substringBefore(" "),       // ID is together with courseName
                        examNameType = columns[0].text().substringAfter(" "),           // Name and Type are mixed together, this at least removes ID
                        grade = columns[2].text(),
                        gradeName = columns[3].text(),
                        gradeSheetArgs = columns[4].select("script").html()   // gives back -N0...0 for grade overview
                            .split(",").getOrNull(3)           // Some exams don't have gradeStats
                    )

                    grades.add(gradeInfo)
                }
            }

            return grades
        }

        suspend fun getGrades(): Map<String, List<GradeInfo>>? {
            val response: HttpResponse =
                getPage(true, "EXAMRESULTS", "")
                    ?: return null
            val document: Document = Ksoup.parse(response.bodyAsText())

            val semesterResult = getSemesters(document)
            val currentSemester = semesterResult.first
            val semesters = semesterResult.second

            val grades = coroutineScope {
                val deferredFetchedGrades = semesters.map { semester ->
                    async {
                        val result = if (semester == currentSemester) {
                            findGrades(document)
                        } else {
                            val asyncResponse =
                                getPage(true, "EXAMRESULTS", "-N000000,-N${semester.id}")
                                    ?: return@async null
                            val asyncDocument: Document = Ksoup.parse(asyncResponse.bodyAsText())

                            findGrades(asyncDocument)        // This is what is being mapped to
                        }

                        semester.name to result
                    }
                }

                val fetchedGrades: Map<String, List<GradeInfo>> = deferredFetchedGrades.awaitAll()
                    .map { it ?: return@coroutineScope null }
                    .toMap()

                return@coroutineScope fetchedGrades.filter { it.value.isNotEmpty() }
            }

            return grades
        }

        // TODO: this should but may not correctly sort future semesters
        suspend fun getBasicExamDetails(): Map<String, List<Exam>>? {
            val response: HttpResponse =
                getPage(true, "MYEXAMS", "")
                    ?: return null
            val document: Document = Ksoup.parse(response.bodyAsText())

            val semesterResult = getSemesters(document)
            val currentSemester = semesterResult.first
            val semesters = semesterResult.second

            val exams = coroutineScope {
                val deferredFetchedExams = semesters.map { semester ->
                    async {
                        val result = if (semester == currentSemester) {
                            findExams(document, currentSemester.name)
                        } else {
                            val asyncResponse =
                                getPage(true, "MYEXAMS", "-N000000,-N${semester.id}")
                                    ?: return@async null
                            val asyncDocument: Document = Ksoup.parse(asyncResponse.bodyAsText())

                            findExams(asyncDocument, semester.name)        // This is what is being mapped to
                        }

                        semester.name to result
                    }
                }

                val fetchedExams: Map<String, List<Exam>> = deferredFetchedExams.awaitAll()
                    .map { it ?: return@coroutineScope null }
                    .toMap()

                val filteredExams = fetchedExams.filter { it.value.isNotEmpty() }

                return@coroutineScope filteredExams
            }

            return exams
        }

        val exams: List<List<Exam>>? = coroutineScope {
            val deferredExams = async { getBasicExamDetails() }
            val deferredGrades = async { getGrades() }

            val examsBySemester = deferredExams.await() ?: return@coroutineScope null
            val gradeInfosBySemester = deferredGrades.await() ?: return@coroutineScope null

            val result: List<List<Exam>> =
                examsBySemester.map { (semester, examList) ->

                    val gradeInfosForSemester = gradeInfosBySemester[semester].orEmpty()
                    val gradeInfoByCourseID = gradeInfosForSemester.groupBy { it.courseID }

                    examList.map { exam ->
                        val gradeInfoList = gradeInfoByCourseID[exam.courseID]
                        val gradeInfo = gradeInfoList
                            ?.let { candidates ->
                                if (candidates.size == 1) {
                                    return@let candidates.first()
                                } else {
                                    val byName = candidates.filter { it.examNameType.contains(exam.courseName) }
                                    val byType = candidates.filter { it.examNameType.contains(exam.examType) }
                                    if (byType.size == 1) {
                                        return@let byType.first()
                                    } else if (byName.size == 1) {
                                        return@let byName.first()
                                    } else {
                                        return@let byName.intersect(byType.toSet()).firstOrNull()
                                    }
                                }
                            }

                        exam.copy(
                            grade = gradeInfo?.grade ?: "",
                            gradeName = gradeInfo?.gradeName ?: "",
                            gradeSheetArgs = gradeInfo?.gradeSheetArgs
                        )
                    }
                }

            return@coroutineScope result
        }

        return exams
    }

    override suspend fun updateExamGradeStats(exam: Exam) {
        if (exam.gradeSheetArgs == null) return

        val response: HttpResponse =
            getPage(true, "GRADEOVERVIEW", "-N000000,-AEXEV,${exam.gradeSheetArgs}")
                ?: return
        val document: Document = Ksoup.parse(response.bodyAsText())

        // Some exams may have grade stats but not actually a grade table (yes stupid, but does exist)
        // Typically such pages would not have a table but the additionalRow would show "noch nicht gesetzt"
        val tableRows = document.select("table.nb tbody tr")
        val additionalRows = document.select("div.tb div.tbdata")

        val gradeTable: MutableList<Pair<String, Int>> = mutableListOf()

        if (tableRows.size > 1) {
            val nameColumn = tableRows[0].select("td")
            val countColumn = tableRows[1].select("td")

            // The first column in TUCaN is labeling
            for (i in 1 until nameColumn.size) {
                val name = nameColumn[i].text()
                val count = when (val text = countColumn[i].text()) {
                    "---" -> 0     // When nobody has a grade
                    else -> text.toInt()
                }

                gradeTable.add(Pair(name, count))
            }
        }

        val gradeAvgRow = additionalRows.firstOrNull { it.text().contains(Regex("Average:|Durchschnitt:")) }
        val resultCountRow = additionalRows.firstOrNull { it.text().contains(Regex("Available Results:|Vorliegende Ergebnisse:")) }

        val gradeAvg = gradeAvgRow?.text()?.split(": ")?.getOrNull(1) ?: run {
            if (gradeTable.isNotEmpty()) {
                val multiplications = gradeTable.map { (grade, amount) ->
                    if (grade == "b") return@run "1,0"
                    if (grade == "nb") return@run "1,0"

                    val floatGrade = grade.replace(",", ".").toDouble()
                    return@map floatGrade * amount
                }
                val result100 = multiplications.sum() / gradeTable.sumOf { it.second } * 100
                val result = kotlin.math.round(result100) / 100
                return@run result.toString()
            } else {
                return@run null
            }
        }

        val resultCount: String? = resultCountRow?.text()?.split(": ")?.getOrNull(1) ?: run {
            if (gradeTable.isNotEmpty()) {
                return@run gradeTable.sumOf { it.second }.toString()
            } else {
                return@run null
            }
        }

        val otherCounts: MutableList<Pair<String, Int?>> = mutableListOf()

        for (row in additionalRows) {
            if (row == gradeAvgRow || row == resultCountRow) continue

            val content = row.text().split(": ")

            if (content.size == 2) {
                otherCounts.add(Pair(content[0], content[1].toInt()))
            } else {
                otherCounts.add(Pair(content[0], null))
            }
        }

        val gradeStats = GradeStats(
            grades = gradeTable,
            gradeAvg = gradeAvg,
            resultCount = resultCount,
            otherCounts = otherCounts
        )

        exam.gradeStats = gradeStats
    }


    // List is used as 3D Structure: semester -> course -> examRegistrations
    override suspend fun getExamRegistrations(): List<List<List<ExamRegistration>>>? {
        fun findExamRegs(document: Document, semesterName: String): List<List<ExamRegistration>> {
            val semesterCourseList = mutableListOf<List<ExamRegistration>>()
            var currentCourseName = ""
            var currentCourseId = ""
            var courseExamRegList = mutableListOf<ExamRegistration>()

            val tableRows = document.select("table tbody tr")

            for (row in tableRows) {
                val columns = row.select("td")
                when {
                    // entry is exam registration
                    row.hasClass("tbdata") -> {
                        // If the column spans five, it probably isn't a valid examReg entry (e.g. a "No Exams" entry)
                        if (columns[0].attr("colspan") != "5") {
                            var registerOption = RegistrationOption.UNAVAILABLE
                            var registerArgs = ""

                            // If the class is "img_arrowLeftRed", it means to deregister, if it is "register" then register
                            if (columns[4].select("a").hasClass("register")) {
                                registerOption = RegistrationOption.REGISTER
                                registerArgs =
                                    columns[4].select("a").attr("href").substringAfter("-N000000,")
                            } else if (columns[4].select("a").hasClass("img_arrowLeftRed")) {
                                registerOption = RegistrationOption.UNREGISTER
                                registerArgs = columns[4].select("a")
                                    .attr("href").substringAfter("-N000000,")
                            }

                            val examRegister = ExamRegistration(
                                semester = semesterName,
                                courseID = currentCourseId,
                                courseName = currentCourseName,
                                examType = columns[2].select("a")
                                    .text(),       // examType is inside an <a></a>
                                examinationDate = columns[3].text(),
                                registerOption = registerOption,
                                registerArgs = registerArgs,
                                additionalInfoArgs = columns[2].select("a").attr("href")
                                    .substringAfter("-N000000,").split(",").first()
                            )

                            courseExamRegList.add(examRegister)
                        }
                    }
                    // entry is title for module
                    row.hasClass("level02") -> {
                        currentCourseId = columns[0].text()
                        currentCourseName = columns[1].html().substringBefore("\n")
                        if (courseExamRegList.isNotEmpty()) {
                            semesterCourseList.add(courseExamRegList)
                            courseExamRegList = mutableListOf()
                        }
                    }
                    // entry is subtitle for module
                    row.hasClass("level03") -> {}
                }
            }

            semesterCourseList.add(courseExamRegList)

            return semesterCourseList
        }

        val response: HttpResponse =
            getPage(true, "EXAMREGISTRATION", "")
                ?: return null
        val document: Document = Ksoup.parse(response.bodyAsText())

        val semesterResult = getSemesters(document)
        val currentSemester = semesterResult.first
        val semesters = semesterResult.second

        //val currentExamRegs = mutableListOf<List<List<ExamRegistration>>>()
        //var examRegistrations = emptyList<List<List<ExamRegistration>>>()

        //val tableRows = document.select("table tbody tr")

        val examRegistrations = coroutineScope {
            val deferredFetchedExamRegs = semesters.map { semester ->
                async {
                    // doing this saves one network request, since the page needs to be loaded for eligible semesters
                    if (semester == currentSemester) {
                        //emptyList()     // This is what is being mapped to

                        findExamRegs(document, currentSemester.name)
                    }
                    else {
                        val asyncResponse =
                            getPage(true, "EXAMREGISTRATION", "-N000000,-N${semester.id}")
                                ?: return@async null
                        val asyncDocument: Document = Ksoup.parse(asyncResponse.bodyAsText())

                        findExamRegs(asyncDocument, semester.name)        // This is what is being mapped to
                    }
                }
            }

            val fetchedExamRegs: List<List<List<ExamRegistration>>> = deferredFetchedExamRegs.awaitAll()
                .map { it ?: return@coroutineScope null }

            // Remove semesters where no examRegs are available
            val filteredExamRegs = fetchedExamRegs.filter { layer2 -> layer2.any { it.isNotEmpty() } }

            //examRegistrations = filteredExamRegs
            return@coroutineScope filteredExamRegs
        }

        return examRegistrations
    }

    // Updates the ExamRegistration in-place
    override suspend fun updateExamRegistrationDetail(examRegistration: ExamRegistration) {
        val response: HttpResponse =
            getPage(true, "EXAMDETAILS", "-N000000, ${examRegistration.additionalInfoArgs}")
                ?: return
        val document: Document = Ksoup.parse(response.bodyAsText())

        val elements = document.select("table tbody tr.tbdata td b")

        val results = mutableMapOf<String, String>()

        for (element in elements) {
            val key = element.text()
            val value = element.nextSibling()?.toString()?.trim()?.removePrefix(":")?.trim() ?: "Not Available"
            results[key] = value
        }

        examRegistration.registerPeriod =  results["Anmeldezeitraum"] ?: results["Registration period"] ?: ""
        examRegistration.cancelPeriod = results["Abmeldezeitraum"] ?: results["Cancellation period"] ?: ""
        examRegistration.examiners = results["Prüfer"] ?: results["Examiners"] ?: ""

        val roomText = results["Raum"] ?: results["Room"]

        if (roomText != null) {
            if (roomText != "") {
                examRegistration.room = listOf(roomText)
            } else {
                val links = document.select("table tbody tr.tbdata td a")

                if (links.isNotEmpty()) {
                    val newRooms = mutableListOf<String>()
                    val newRoomArgs = mutableListOf<String>()

                    for (link in links) {
                        newRooms.add(link.text())
                        newRoomArgs.add(link.attr("href").substringAfter("ARGUMENTS="))
                    }

                    examRegistration.room = newRooms
                    examRegistration.roomArgs = newRoomArgs
                }
            }
        }
    }

    override suspend fun getExamModifyPrompt(isRegister: Boolean, arguments: String): String? {
        val pagename = if (isRegister) "REGEXAM" else "EXAMUNREG"

        val response: HttpResponse = getPage(true, pagename, "-N000000,$arguments") ?: return null
        val document: Document = Ksoup.parse(response.bodyAsText())

        val registerID = document.select("input[name=rgtr_id]").attr("value")

        return registerID
    }

    override suspend fun confirmExamModify(registerID: String): Boolean {
        val tucanCredentials = IdentityManager.getTucanCredentials() ?: return false

        val response: HttpResponse = client.safeRequest {
            submitForm(
                "https://www.tucan.tu-darmstadt.de/scripts/mgrqispi.dll",
                formParameters = parameters {
                    append("campusnet_submit", "")
                    append("APPNAME", "CampusNet")
                    append("PRGNAME", "SAVEREGISTRATION")
                    append("ARGUMENTS", "sessionno,menuid,rgtr_id,mode,timetable_id,location_id")
                    append("sessionno", tucanCredentials.sessionId.substringAfter("-N"))
                    append("menuid", "000318")
                    append("rgtr_id", registerID)
                    append("mode", "+++0")
                })
            {
                header("Cookie", "${tucanCredentials.cookie.name}=${tucanCredentials.cookie.value}")
            }
        } ?: return false

        val document = Ksoup.parse(response.bodyAsText())

        val success = document.select("span.note:containsOwn(Ihre Anmeldung war erfolgreich.)").isNotEmpty() ||
                document.select("span.note:containsOwn(Your registration has been successful.)").isNotEmpty() ||
                document.select("span.note:containsOwn(Ihre Abmeldung war erfolgreich.)").isNotEmpty() ||
                document.select("span.note:containsOwn(Your cancellation has been successful.)").isNotEmpty()

        return success
    }
}