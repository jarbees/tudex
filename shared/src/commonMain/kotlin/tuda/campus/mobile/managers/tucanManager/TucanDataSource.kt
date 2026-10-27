package tuda.campus.mobile.managers.tucanManager

interface TucanDataSource {
    // The date must be in the format "dd.mm.yyyy" and day is weekday in full name
    suspend fun getDaySchedule(date: String, day: String): List<TimeSlot>?

    // List is used as 3D Structure: semester -> module -> course
    // if only one course per module or module per semester, it is still a list
    suspend fun getCourses(currentCourses: List<List<List<Course>>>): List<List<List<Course>>>?

    suspend fun getExams(): List<List<Exam>>?

    suspend fun updateExamGradeStats(exam: Exam)


    // List is used as 3D Structure: semester -> course -> examRegistrations
    suspend fun getExamRegistrations(): List<List<List<ExamRegistration>>>?

    // Updates the ExamRegistration in-place
    suspend fun updateExamRegistrationDetail(examRegistration: ExamRegistration)

    suspend fun getExamModifyPrompt(isRegister: Boolean, arguments: String): String?

    suspend fun confirmExamModify(registerID: String): Boolean
}