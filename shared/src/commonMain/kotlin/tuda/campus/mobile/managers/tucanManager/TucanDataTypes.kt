package tuda.campus.mobile.managers.tucanManager

import kotlinx.serialization.Serializable

/*
data class Module(
    val semester: String,
    val moduleID: String,
    val name: String,
    val detailLinkArgs: String,
    val teachers: String,
    val creditPoints: Int,
    val courses: List<Course>,
    val exams: List<Exam>
)
*/

/*
data class CourseDescription(
    val instructors: String,
    val eventType: String,
    val orgUnit: String,
    val timetableName: String,
    val subject: String,
    val creditingFor: String,
    val weekHours: Int?,
    val instructionLang: String,
    val minMaxParticipants: String,
    val courseContents: String
)
*/

@Serializable
data class Course(
    val semester: String,
    val courseID: String,
    val courseName: String,
    val moduleName: String = "",
    val detailLinkArgs: String,
    val courseDetails: CourseDetails? = null
) {
    val id = listOf(semester, courseID, courseName, moduleName, detailLinkArgs).hashCode()
}

@Serializable
data class CourseDetails(
    val details: String,
    val groups: List<CourseGroup>,
    val regDeadlines: List<CourseRegDeadline>,
    val dates: List<EventDate.CourseDate>?,
    val plenaryDates: List<EventDate.CourseDate>?,
    val moduleIDs: List<String>
)

@Serializable
data class CourseGroup(
    val title: String,
    val teachers: String,
    val detailLinkArg: String?,
)

@Serializable
data class CourseRegDeadline(
    val phase: String,
    val block: String,
    val startDate: String,
    val endRegisterDate: String,
    val endDeregisterDate: String,
    val endListenerDate: String
)


sealed class EventDate {
    abstract val courseName: String
    abstract val date: String
    abstract val startTime: String
    abstract val endTime: String
    abstract val startTimeMins: Int
    abstract val endTimeMins: Int

    @Serializable
    data class CourseDate(
        override val courseName: String,
        override val date: String,
        override val startTime: String,
        override val endTime: String,
        override val startTimeMins: Int,
        override val endTimeMins: Int,
        val room: String,
        val teachers: String
    ) : EventDate()

    data class ExamDate(
        override val courseName: String,
        override val date: String,
        override val startTime: String,
        override val endTime: String,
        override val startTimeMins: Int,
        override val endTimeMins: Int,
        val examType: String,
        val courseID: String,
        //val room: String,
        //val teachers: String
    ) : EventDate()
}


data class TimeSlot (
    val courseID: String,
    val courseName: String,
    val instructor: String,
    val day: String,
    val time: String,
    val room: String,
) {
    val id = listOf(courseID, courseName, instructor, time, room, day).hashCode()
}

@Serializable
data class GradeStats(
    val grades: List<Pair<String, Int>>,
    val gradeAvg: String?,
    val resultCount: String?,
    val otherCounts: List<Pair<String, Int?>>
) {
    val id = listOf(grades, gradeAvg, resultCount, otherCounts).hashCode()
}

@Serializable
data class Exam(
    val semester: String,
    val courseID: String,
    val courseName: String,
    val examType: String,
    val examinationDateTime: String,

    var grade: String = "",
    var gradeName: String = "",
    var gradeSheetArgs: String? = null,

    var gradeStats: GradeStats? = null
) {
    val id = listOf(semester, courseID, courseName, examType, examinationDateTime).hashCode()
}

enum class RegistrationOption {
    REGISTER,
    UNREGISTER,
    UNAVAILABLE
}

enum class RequestStatus {
    READY,
    //REFRESHING,     // This should be used when user starts a refresh after content has loaded
    LOADING,        // This should be used when the app loads content by itself
    //SLOW,
    FAILED,
    //NO_INTERNET
}

// ExamRegistrations are only valid for a given Tucan Session and must be regenerated
data class ExamRegistration(
    val semester: String,
    val courseID: String,
    val courseName: String,
    val examType: String,
    val examinationDate: String,
    val registerOption: RegistrationOption,
    val registerArgs: String,

    // These attributes are loaded on-Demand since they require an additional request to TUCaN
    var additionalInfoArgs: String,
    var registerPeriod: String = "",
    var cancelPeriod: String = "",
    var examiners: String = "",
    var room: List<String> = emptyList(),
    var roomArgs: List<String> = emptyList()
){
    val id = listOf(semester, courseID, courseName, examType, examinationDate, registerOption, registerArgs).hashCode()
}