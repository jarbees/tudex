package tuda.campus.mobile.managers.tucanManager

import korlibs.time.seconds
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.collections.listOf
import kotlin.time.Clock

class TucanDummySource: TucanDataSource {
    private fun LocalDate.formatGerman(): String {
        val weekdays = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
        val months = listOf(
            "Jan.", "Feb.", "März", "Apr.", "Mai", "Jun.",
            "Jul.", "Aug.", "Sep.", "Okt.", "Nov.", "Dez."
        )

        val weekday = weekdays[this.dayOfWeek.isoDayNumber - 1]
        val month = months[month.number - 1]

        return "$weekday, $day. $month ${this.year}"
    }

    fun convertToMinutes(time: String): Int {
        val parts = time.split(":")
        if (parts.size != 2) throw IllegalArgumentException("Time must be in HH:mm format")

        val hours = parts[0].toInt()
        val minutes = parts[1].toInt()

        return hours * 60 + minutes
    }

    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val monday = today.minus(
        today.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber,
        DateTimeUnit.DAY
    )

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getDaySchedule(date: String, day: String): List<TimeSlot>? {
        delay(4.seconds)
        return when (today.dayOfWeek) {
            DayOfWeek.MONDAY -> listOf(
                TimeSlot(
                    courseID = "04-9090-10-iv",
                    courseName = "Foundations of Applied Mathematics",
                    instructor = "Adrian Keller",
                    day = "Monday",
                    time = "08:00 - 09:40",
                    room = "S101|A1"
                ),
                TimeSlot(
                    courseID = "16-0040-71-vl",
                    courseName = "Principles of Chemistry",
                    instructor = "Hannah Clarke",
                    day = "Monday",
                    time = "08:30 - 11:00",
                    room = "S101|A01"
                ),
                TimeSlot(
                    courseID = "04-9090-10-iv",
                    courseName = "Foundations of Applied Mathematics",
                    instructor = "Adrian Keller",
                    day = "Monday",
                    time = "09:50 - 11:30",
                    room = "S10|317"
                ),
                TimeSlot(
                    courseID = "06-1050-13-tt",
                    courseName = "Academic Writing",
                    instructor = "Laura Bennett",
                    day = "Monday",
                    time = "12:30 - 14:10",
                    room = "S310 14"
                )
            )

            DayOfWeek.TUESDAY -> listOf(
                TimeSlot(
                    courseID = "20-2040-62-iv",
                    courseName = "Computational Thinking",
                    instructor = "Elena Morris",
                    day = "Tuesday",
                    time = "08:00 - 09:40",
                    room = "S101|A1"
                ),
                TimeSlot(
                    courseID = "20-2040-62-iv",
                    courseName = "Computational Thinking",
                    instructor = "Elena Morris",
                    day = "Tuesday",
                    time = "12:30 - 14:10",
                    room = "S110|17"
                )
            )

            DayOfWeek.WEDNESDAY -> listOf(
                TimeSlot(
                    courseID = "16-0040-71-tt",
                    courseName = "Principles of Chemistry",
                    instructor = "Hannah Clarke",
                    day = "Wednesday",
                    time = "09:50 - 11:30",
                    room = "S401|1+2"
                ),
                TimeSlot(
                    courseID = "16-0040-33-vl",
                    courseName = "Mechanics & Physical Interaction",
                    instructor = "Daniel Fischer",
                    day = "Wednesday",
                    time = "14:20 - 16:00",
                    room = "S206|30"
                )
            )

            DayOfWeek.THURSDAY -> listOf(
                TimeSlot(
                    courseID = "12-1240-17-vl",
                    courseName = "Environmental Systems & Geography",
                    instructor = "Markus Engel",
                    day = "Thursday",
                    time = "08:00 - 09:40",
                    room = "S206|30"
                ),
                TimeSlot(
                    courseID = "05-0040-33-ue",
                    courseName = "Mechanics & Physical Interaction",
                    instructor = "Daniel Fischer",
                    day = "Thursday",
                    time = "16:00 - 17:40",
                    room = "S101|A3"
                )
            )

            DayOfWeek.FRIDAY -> listOf(
                TimeSlot(
                    courseID = "16-0040-71-ue",
                    courseName = "Principles of Chemistry",
                    instructor = "Hannah Clarke",
                    day = "Friday",
                    time = "09:50 - 11:30",
                    room = "S103|17"
                ),
                TimeSlot(
                    courseID = "12-1240-17-ue",
                    courseName = "Environmental Systems & Geography",
                    instructor = "Markus Engel",
                    day = "Friday",
                    time = "16:00 - 17:40",
                    room = "S103|12"
                )
            )

            DayOfWeek.SATURDAY -> emptyList()

            DayOfWeek.SUNDAY -> emptyList()
        }
    }

    // List is used as 3D Structure: semester -> module -> course
    // if only one course per module or module per semester, it is still a list
    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getCourses(currentCourses: List<List<List<Course>>>): List<List<List<Course>>>? {
        delay(3.seconds)
        val details: (teacher: String, subject: String, courseID: String) -> String = { teacher, subject, courseID ->
        """Lehrende: Prof. $teacher
        Veranstaltungsart: Vorlesung und Übung
        Orga-Einheit: FB04 $subject
        Anzeige im Stundenplan: $courseID
        Anrechenbar für:
        Semesterwochenstunden: 3
        Unterrichtssprache: Deutsch
        Min. | Max. Teilnehmeranzahl: - | 102
        Lehrinhalte: Numerische Lösung linearer Gleichungssysteme, Grenzwerte, Analysis, ...
        Literatur: Mathematik für Anfänger, Platzhirsch
        Vorraussetzungen: empfohlen: Mathematik I bis V
        Online-Angebote: moodle
        """.trimMargin() }

        val courseGroups: (teacher: String, subject: String) -> List<CourseGroup> = { teacher, subject ->
            List(9) { index ->
                CourseGroup(
                    title = "$subject Übung $index",
                    teachers = "Prof. $teacher",
                    detailLinkArg = null
                )
            }
        }

        val regDeadlines = listOf(
            CourseRegDeadline(
                phase = "Direkte Zulassung",
                block = "Vorlesungszeit",
                startDate = "01.03.2026 00:00",
                endRegisterDate = "31.08.2026 23:59",
                endDeregisterDate = "31.08.2026 23:59",
                endListenerDate = "31.08.2026 23:59"
            )
        )

        val datesList: (
            weekday: DayOfWeek, room: String, startTime: String, endTime: String, subject: String, teacher: String
        ) -> List<EventDate.CourseDate> = { weekday, room, startTime, endTime, subject, teacher ->
            List(10) { index ->
                EventDate.CourseDate(
                    courseName = subject,
                    date = (monday.plus(weekday.isoDayNumber - 1 + (index - 5) * 7, DateTimeUnit.DAY)).formatGerman(),
                    startTime = startTime,
                    endTime = endTime,
                    startTimeMins = convertToMinutes(startTime),
                    endTimeMins = convertToMinutes(endTime),
                    room = room,
                    teachers = "Prof. $teacher"
                )
            }
        }

        val courseDetails: (
            subject: String, teacher: String, moduleID: String, type: String, room: String, weekday: DayOfWeek, startTime: String, endTime: String, plenary: Boolean, plenWeekday: DayOfWeek?, plenRoom: String?, plenStart: String?, plenEnd: String?
        ) -> CourseDetails = { subject, teacher, moduleID, type, room, weekday, startTime, endTime, plenary, plenWeekday, plenRoom, plenStart, plenEnd ->
            CourseDetails(
                details = details(teacher, subject, "$moduleID-$type"),
                groups = courseGroups(teacher, subject),
                regDeadlines = regDeadlines,
                dates = datesList(
                    weekday,
                    room,
                    startTime,
                    endTime,
                    subject,
                    teacher
                ),
                plenaryDates = if (plenary) datesList(
                    plenWeekday!!,
                    plenRoom!!,
                    plenStart!!,
                    plenEnd!!,
                    subject,
                    teacher
                ) else null,
                moduleIDs = listOf(moduleID)
            )
        }

        val course: (
            semester: String, subject: String, teacher: String, moduleID: String, type: String, room: String, weekday: DayOfWeek, startTime: String, endTime: String, plenary: Boolean, plenRoom: String?, plenWeekday: DayOfWeek?, plenStart: String?, plenEnd: String?
        ) -> Course = { semester, subject, teacher, moduleID, type, room, weekday, startTime, endTime, plenary, plenRoom, plenWeekday, plenStart, plenEnd ->
            Course(
                semester = semester,
                courseID = "$moduleID-$type",
                courseName = subject,
                moduleName = subject,
                detailLinkArgs = "",
                courseDetails = courseDetails(subject, teacher, moduleID, type, room, weekday, startTime, endTime, plenary, plenWeekday, plenRoom, plenStart, plenEnd)
            )
        }

        return listOf(
            listOf(
                listOf(
                    course("SoSe 2026", "Foundations of Applied Mathematics", "Adrian Keller", "04-9090-10", "iv", "S101|A1", DayOfWeek(1), "8:00", "9:40", true, "S103|17", DayOfWeek(1), "9:50", "11:30"),
                ),
                listOf(
                    course("SoSe 2026", "Principles of Chemistry", "Hannah Clarke", "16-0040-71", "vl", "S101|A01", DayOfWeek(1), "8:30", "11:00", false, null, null, null, null),
                    course("SoSe 2026", "Principles of Chemistry", "Hannah Clarke", "16-0040-71", "ue", "S103|17",DayOfWeek(5), "9:50", "11:30", false, null, null, null, null),
                    course("SoSe 2026", "Principles of Chemistry", "Hannah Clarke", "16-0040-71", "tt", "S401|1+2",DayOfWeek(3),  "9:50", "11:30", false, null, null, null, null),
                ),
                listOf(
                    course("SoSe 2026", "Academic Writing", "Laura Bennett", "06-1050-13", "tt", "S310|14", DayOfWeek(1), "12:30", "14:10", false, null, null, null, null),
                ),
                listOf(
                    course("SoSe 2026", "Mechanics & Physical Interaction", "Daniel Fischer", "05-0040-33", "vl", "S206|30", DayOfWeek(3), "14:20", "16:00", false, null, null, null, null),
                    course("SoSe 2026", "Mechanics & Physical Interaction", "Daniel Fischer", "05-0040-33", "ue", "S101|A3", DayOfWeek(4), "16:00", "17:40", false, null, null, null, null),
                ),
                listOf(
                    course("SoSe 2026", "Computational Thinking", "Elena Morris", "20-2040-62", "iv", "S101|A1", DayOfWeek(2), "8:00", "9:40", false, "S110|17",DayOfWeek(2),  "12:30", "14:10"),
                ),
                listOf(
                    course("SoSe 2026", "Environmental Systems & Geography", "Markus Engel", "12-1240-17", "vl", "S206|30", DayOfWeek(4), "08:00", "09:40", false, null, null, null, null),
                    course("SoSe 2026", "Environmental Systems & Geography", "Markus Engel", "12-1240-17", "ue", "S103|12",DayOfWeek(5),  "16:00", "17:40", false, null, null, null, null),
                )
            ),
            listOf(
                listOf(
                    course("WiSe 2025/26", "Introduction to Cognitive Psychology", "Ethan Brooks", "12-5839-94", "iv", "S101|A1", DayOfWeek(1), "8:00", "9:40", true, "S103|17", DayOfWeek(1), "9:50", "11:30"),
                ),
                listOf(
                    course("WiSe 2025/26", "Statistical Methods", "Isabelle Meyer", "04-0150-71", "vl", "S101|A01", DayOfWeek(1), "8:30", "11:00", false, null, null, null, null),
                    course("WiSe 2025/26", "Statistical Methods", "Isabelle Meyer", "04-0150-71", "ue", "S103|17",DayOfWeek(5), "9:50", "11:30", false, null, null, null, null),
                    course("WiSe 2025/26", "Statistical Methods", "Isabelle Meyer", "ß4-0150-71", "tt", "S401|1+2",DayOfWeek(3),  "9:50", "11:30", false, null, null, null, null),
                ),
                listOf(
                    course("WiSe 2025/26", "Network Systems & Security", "Lena Hoffmann", "20-3405-16", "tt", "S310|14", DayOfWeek(1), "12:30", "14:10", false, null, null, null, null),
                ),
                listOf(
                    course("WiSe 2025/26", "Renewable Energy Technologies", "Miriam Vogel", "18-5670-44", "vl", "S206|30", DayOfWeek(3), "14:20", "16:00", false, null, null, null, null),
                    course("WiSe 2025/26", "Renewable Energy Technologies", "Miriam Vogel", "18-5670-44", "ue", "S101|A3", DayOfWeek(4), "16:00", "17:40", false, null, null, null, null),
                ),
                listOf(
                    course("WiSe 2025/26", "Applied Linguistics", "Sophia Mercer", "03-1820-32", "iv", "S101|A1", DayOfWeek(2), "8:00", "9:40", false, "S110|17",DayOfWeek(2),  "12:30", "14:10"),
                ),
                listOf(
                    course("WiSe 2025/26", "Foundations of Microeconomics", "Victor Lang", "01-4052-17", "vl", "S206|30", DayOfWeek(4), "08:00", "09:40", false, null, null, null, null),
                    course("WiSe 2025/26", "Foundations of Microeconomics", "Victor Lang", "01-4052-17", "ue", "S103|12",DayOfWeek(5),  "16:00", "17:40", false, null, null, null, null),
                )
            )
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getExams(): List<List<Exam>>? {
        // gradeSheetArgs are "" instead of null so that gradeStats are fetched

        delay(5.seconds)
        return listOf(
            listOf(
                Exam("SoSe 2026","16-0040-71","Principles of Chemistry","Written/Oral Exam","Mi, 4. Mär. 2026 12:00-14:00","3,7","ausreichend","",null),
                Exam("SoSe 2026","06-1050-13","Academic Writing","Studienleistung mündlich/schriftlich","Fr, 30. Jan. 2026 0:00-0:00","b","bestanden","",null),
                Exam("SoSe 2026","05-0040-33","Mechanics & Physical Interaction","Written/Oral Exam","Fr, 30. Jan. 2026 0:00-0:00","2,3","gut","",null),
                Exam("SoSe 2026","04-9090-10","Foundations of Applied Mathematics","Written/Oral Exam","Mi, 25. Feb. 2026 08:00-10:00","5,0","mangelhaft","",null),
                Exam("SoSe 2026","20-2040-62","Computational Thinking","Written/Oral Exam","Fr, 30. Jan. 2026 0:00-0:00","1,0","sehr gut","",null),
                Exam("SoSe 2026","12-1240-17","Environmental Systems & Geography","Written/Oral Exam","Fr, 30. Jan. 2026 0:00-0:00","1,7","gut","",null),
            ),
            listOf(
                Exam("WiSe 2025/26","12-5839-94","Introduction to Cognitive Psychology","Written/Oral Exam","Mi, 25. Feb. 2026 08:00-10:00","1,3","sehr gut","",null),
                Exam("WiSe 2025/26","04-0150-71","Statistical Methods","Written/Oral Exam","Mi, 4. Mär. 2026 12:00-14:00","4,0","ausreichend","",null),
                Exam("WiSe 2025/26","20-3405-16","Network Systems & Security","Written/Oral Exam","Fr, 30. Jan. 2026 0:00-0:00","2,3","gut","",null),
                Exam("WiSe 2025/26","18-5670-44","Renewable Energy Technologies","Written/Oral Exam","Fr, 30. Jan. 2026 0:00-0:00","2,7","befriedigend","",null),
                Exam("WiSe 2025/26","03-1820-32","Applied Linguistics","Studienleistung mündlich/schriftlich","Fr, 30. Jan. 2026 0:00-0:00","b","bestanden","",null),
                Exam("WiSe 2025/26","01-4052-17","Foundations of Microeconomics","Written/Oral Exam","Fr, 30. Jan. 2026 0:00-0:00","3,3","befriedigend","",null),
            )
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun updateExamGradeStats(exam: Exam) {
        println("Getting Exam")
        delay(1.seconds)
        exam.gradeStats =
                when(exam.courseID) {
                    // SoSe 2026
                    "04-9090-10" -> // Foundations of Applied Mathematics
                        GradeStats(
                            grades = listOf(
                                "1,0" to 2,
                                "1,3" to 5,
                                "1,7" to 9,
                                "2,0" to 14,
                                "2,3" to 18,
                                "2,7" to 21,
                                "3,0" to 17,
                                "3,3" to 12,
                                "3,7" to 10,
                                "4,0" to 5,
                                "5,0" to 7
                            ),
                            gradeAvg = "2,8",
                            resultCount = "120",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 3,
                                "Fehlend (Krankschreibung)" to 5,
                                "Fehlend (Ohne Grund)" to 6,
                                "Fehlend (Mündliche Ergänzungsprüfung)" to 2
                            )
                        )
                    "16-0040-71" -> // Principles of Chemistry
                        GradeStats(
                            grades = listOf(
                                "1,0" to 6,
                                "1,3" to 11,
                                "1,7" to 15,
                                "2,0" to 20,
                                "2,3" to 24,
                                "2,7" to 21,
                                "3,0" to 18,
                                "3,3" to 12,
                                "3,7" to 9,
                                "4,0" to 4,
                                "5,0" to 3
                            ),
                            gradeAvg = "2,3",
                            resultCount = "143",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 2,
                                "Fehlend (Krankschreibung)" to 4,
                                "Fehlend (Ohne Grund)" to 5
                            )
                        )
                    "06-1050-13" -> // Academic Writing
                        GradeStats(
                            grades = listOf(
                                "b" to 178,
                                "nb" to 14
                            ),
                            gradeAvg = "1,0",
                            resultCount = "192",
                            otherCounts = listOf(
                                "Fehlend (Krankschreibung)" to 3,
                                "Fehlend (Ohne Grund)" to 4
                            )
                        )
                    "05-0040-33" -> // Mechanics & Physical Interaction
                        GradeStats(
                            grades = listOf(
                                "1,0" to 14,
                                "1,3" to 21,
                                "1,7" to 27,
                                "2,0" to 31,
                                "2,3" to 28,
                                "2,7" to 20,
                                "3,0" to 12,
                                "3,3" to 7,
                                "3,7" to 4,
                                "4,0" to 2,
                                "5,0" to 1
                            ),
                            gradeAvg = "2,0",
                            resultCount = "167",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 4,
                                "Fehlend (Krankschreibung)" to 3,
                                "Fehlend (Ohne Grund)" to 2
                            )
                        )
                    "20-2040-62" -> // Computational Thinking
                        GradeStats(
                            grades = listOf(
                                "1,0" to 58,
                                "1,3" to 41,
                                "1,7" to 24,
                                "2,0" to 15,
                                "2,3" to 7,
                                "2,7" to 4,
                                "3,0" to 2,
                                "3,3" to 1
                            ),
                            gradeAvg = "1,4",
                            resultCount = "152",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 1,
                                "Fehlend (Krankschreibung)" to 2,
                                "Fehlend (Ohne Grund)" to 2
                            )
                        )
                    "12-1240-17" -> // Environmental Systems & Geography
                        GradeStats(
                            grades = listOf(
                                "1,0" to 19,
                                "1,3" to 24,
                                "1,7" to 31,
                                "2,0" to 28,
                                "2,3" to 22,
                                "2,7" to 15,
                                "3,0" to 10,
                                "3,3" to 6,
                                "3,7" to 3,
                                "4,0" to 1
                            ),
                            gradeAvg = "1,9",
                            resultCount = "159",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 3,
                                "Fehlend (Krankschreibung)" to 4,
                                "Fehlend (Ohne Grund)" to 3
                            )
                        )

                    // WiSe 2025/26
                    "12-5839-94" -> // Introduction to Cognitive Psychology
                        GradeStats(
                            grades = listOf(
                                "1,0" to 33,
                                "1,3" to 41,
                                "1,7" to 29,
                                "2,0" to 18,
                                "2,3" to 11,
                                "2,7" to 5,
                                "3,0" to 2,
                                "3,3" to 1
                            ),
                            gradeAvg = "1,6",
                            resultCount = "140",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 2,
                                "Fehlend (Krankschreibung)" to 3,
                                "Fehlend (Ohne Grund)" to 1
                            )
                        )
                    "04-0150-71" -> // Statistical Methods
                        GradeStats(
                            grades = listOf(
                                "1,0" to 3,
                                "1,3" to 6,
                                "1,7" to 10,
                                "2,0" to 14,
                                "2,3" to 17,
                                "2,7" to 22,
                                "3,0" to 24,
                                "3,3" to 20,
                                "3,7" to 18,
                                "4,0" to 9,
                                "5,0" to 16
                            ),
                            gradeAvg = "3,0",
                            resultCount = "159",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 5,
                                "Fehlend (Krankschreibung)" to 6,
                                "Fehlend (Ohne Grund)" to 11,
                                "Fehlend (Mündliche Ergänzungsprüfung)" to 4
                            )
                        )
                    "20-3405-16" -> // Network Systems & Security
                        GradeStats(
                            grades = listOf(
                                "1,0" to 10,
                                "1,3" to 17,
                                "1,7" to 23,
                                "2,0" to 27,
                                "2,3" to 25,
                                "2,7" to 19,
                                "3,0" to 12,
                                "3,3" to 8,
                                "3,7" to 5,
                                "4,0" to 2
                            ),
                            gradeAvg = "2,1",
                            resultCount = "148",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 2,
                                "Fehlend (Krankschreibung)" to 3,
                                "Fehlend (Ohne Grund)" to 4
                            )
                        )
                    "18-5670-44" -> // Renewable Energy Technologies
                        GradeStats(
                            grades = listOf(
                                "1,0" to 7,
                                "1,3" to 12,
                                "1,7" to 18,
                                "2,0" to 24,
                                "2,3" to 28,
                                "2,7" to 25,
                                "3,0" to 16,
                                "3,3" to 10,
                                "3,7" to 6,
                                "4,0" to 3
                            ),
                            gradeAvg = "2,4",
                            resultCount = "149",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 3,
                                "Fehlend (Krankschreibung)" to 4,
                                "Fehlend (Ohne Grund)" to 3
                            )
                        )
                    "03-1820-32" -> // Applied Linguistics
                        GradeStats(
                            grades = listOf(
                                "b" to 121,
                                "nb" to 19
                            ),
                            gradeAvg = "1,0",
                            resultCount = "140",
                            otherCounts = listOf(
                                "Fehlend (Krankschreibung)" to 4,
                                "Fehlend (Ohne Grund)" to 5
                            )
                        )
                    "01-4052-17" -> // Foundations of Microeconomics
                        GradeStats(
                            grades = listOf(
                                "1,0" to 4,
                                "1,3" to 8,
                                "1,7" to 13,
                                "2,0" to 19,
                                "2,3" to 24,
                                "2,7" to 26,
                                "3,0" to 23,
                                "3,3" to 17,
                                "3,7" to 11,
                                "4,0" to 6,
                                "5,0" to 4
                            ),
                            gradeAvg = "2,6",
                            resultCount = "155",
                            otherCounts = listOf(
                                "Ergebnisse mit abweichendem BWS:" to 4,
                                "Fehlend (Krankschreibung)" to 5,
                                "Fehlend (Ohne Grund)" to 5
                            )
                        )

                    else -> null
                }
    }

    override suspend fun getExamRegistrations(): List<List<List<ExamRegistration>>>? {
        delay(3.seconds)
        return listOf(
            // WiSe 2025/26
            listOf(

                // Module 1
                listOf(
                    ExamRegistration(
                        semester = "WiSe 2025/26",
                        courseID = "11-2040-81",
                        courseName = "Introduction to Robotics",
                        examType = "Written/Oral Exam",
                        examinationDate = "Mo, 16. Mär. 2026 09:00-11:00",
                        registerOption = RegistrationOption.REGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    ),
                    ExamRegistration(
                        semester = "WiSe 2025/26",
                        courseID = "11-2040-81",
                        courseName = "Introduction to Robotics",
                        examType = "Oral Re-Examination",
                        examinationDate = "Do, 9. Apr. 2026 10:00-10:30",
                        registerOption = RegistrationOption.UNAVAILABLE,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 2
                listOf(
                    ExamRegistration(
                        semester = "WiSe 2025/26",
                        courseID = "07-1122-54",
                        courseName = "Media Ethics & Society",
                        examType = "Written Exam",
                        examinationDate = "Mi, 25. Mär. 2026 14:00-16:00",
                        registerOption = RegistrationOption.UNREGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 3
                listOf(
                    ExamRegistration(
                        semester = "WiSe 2025/26",
                        courseID = "14-8871-22",
                        courseName = "Advanced Materials Science",
                        examType = "Written/Oral Exam",
                        examinationDate = "Fr, 17. Apr. 2026 08:00-10:00",
                        registerOption = RegistrationOption.UNAVAILABLE,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                )
            ),

            // SoSe 2026
            listOf(

                // Module 1
                listOf(
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "09-5510-72",
                        courseName = "Human-Computer Interaction",
                        examType = "Written Exam",
                        examinationDate = "Mo, 17. Aug. 2026 10:00-12:00",
                        registerOption = RegistrationOption.REGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 2
                listOf(
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "02-4407-91",
                        courseName = "International Trade Law",
                        examType = "Written/Oral Exam",
                        examinationDate = "Mi, 26. Aug. 2026 13:00-15:00",
                        registerOption = RegistrationOption.UNREGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    ),
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "02-4407-91",
                        courseName = "International Trade Law",
                        examType = "Oral Re-Examination",
                        examinationDate = "Di, 15. Sep. 2026 09:30-10:00",
                        registerOption = RegistrationOption.REGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 3
                listOf(
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "18-2031-66",
                        courseName = "Biophysics",
                        examType = "Written Exam",
                        examinationDate = "Fr, 4. Sep. 2026 08:00-10:00",
                        registerOption = RegistrationOption.UNAVAILABLE,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 4
                listOf(
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "05-7712-40",
                        courseName = "Urban Mobility Systems",
                        examType = "Project Presentation",
                        examinationDate = "Do, 27. Aug. 2026 15:00-17:00",
                        registerOption = RegistrationOption.REGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 5
                listOf(
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "13-6044-18",
                        courseName = "Digital Signal Processing",
                        examType = "Written/Oral Exam",
                        examinationDate = "Mo, 7. Sep. 2026 12:00-14:00",
                        registerOption = RegistrationOption.UNREGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                ),

                // Module 6
                listOf(
                    ExamRegistration(
                        semester = "SoSe 2026",
                        courseID = "21-3308-77",
                        courseName = "Comparative Political Systems",
                        examType = "Written Exam",
                        examinationDate = "Mi, 2. Sep. 2026 10:00-12:00",
                        registerOption = RegistrationOption.REGISTER,
                        registerArgs = "",
                        additionalInfoArgs = ""
                    )
                )
            )
        )
    }

    override suspend fun updateExamRegistrationDetail(examRegistration: ExamRegistration) {
        delay(1.seconds)

        examRegistration.registerPeriod = "01.03.26 00:00 - 30.06.26 23:59"
        examRegistration.cancelPeriod = "01.03.26 00:00 - 30.06.26 23:59"
        examRegistration.examiners = "Prof. Hanz Tuffel"
        examRegistration.room = listOf("S101|A1", "L102|1+2")
        examRegistration.roomArgs = emptyList()
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun getExamModifyPrompt(isRegister: Boolean, arguments: String): String? {
        delay(2.seconds)
        return "RealThingTM"
    }

    @OptIn(ExperimentalStdlibApi::class)
    override suspend fun confirmExamModify(registerID: String): Boolean {
        delay(2.seconds)
        return true
    }
}