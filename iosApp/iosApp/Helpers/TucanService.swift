//  Created on 14.06.25.

import SwiftUI
import Shared


class TucanService: ObservableObject {
    private let tucanManager = TucanManager.shared
    
    @Published private(set) var timeSlots: [TimeSlot]                       = TucanManager.shared.todayTimeSlots.value
    @Published private(set) var courses: [[[Course]]]                       = TucanManager.shared.courses.value
    
    // not initialzed, due to Conversion from KotlinPair<String, String> -> (String, String); taken care of in syncStates()
    //@Published private(set) var datesListSortedWrapper: (locale: String, datesListSorted: [[CourseDate]])? = nil
    @Published private(set) var datesListSortedWrapper: (locale: String, datesListSorted: [[EventDate]])? = nil
    
    @Published private(set) var exams: [[Exam]]                             = TucanManager.shared.exams.value
    @Published private(set) var examRegistrations: [[[ExamRegistration]]]   = TucanManager.shared.examRegistrations.value
    
    @Published private(set) var dateStatus: RequestStatus                   = TucanManager.shared.dateStatus.value
    @Published private(set) var timeSlotStatus: RequestStatus               = TucanManager.shared.timeSlotStatus.value
    @Published private(set) var courseStatus: RequestStatus                 = TucanManager.shared.courseStatus.value
    @Published private(set) var examStatus: RequestStatus                   = TucanManager.shared.examStatus.value
    @Published private(set) var examRegStatus: RequestStatus                = TucanManager.shared.examRegStatus.value
    
    init() {
        Task {
            await syncStates()
        }
    }
    
    @MainActor
    private func initializeStates() async {
        _ = try? await tucanManager.initializeStates()
    }
    
    // SwiftUI Refresh function; create Published Values from Kotlin Flows
    
    @MainActor
    private func syncStates() async {
        Task {
            for await tempTimeSlots in tucanManager.todayTimeSlots {
                self.timeSlots = tempTimeSlots
            }
        }
        Task {
            for await tempExams in tucanManager.exams {
                self.exams = tempExams
            }
        }
        Task {
            for await tempExamRegistrations in tucanManager.examRegistrations {
                self.examRegistrations = tempExamRegistrations
            }
        }
        Task {
            for await tempCourses in tucanManager.courses {
                self.courses = tempCourses
            }
        }
        Task {
            for await tempDatesList in tucanManager.datesList {
                if let locale = tempDatesList?.first,
                   let dateList = tempDatesList?.second {
                    //self.datesListSortedWrapper = (locale: String(locale), datesListSorted: dateList as! [[CourseDate]])
                    self.datesListSortedWrapper = (locale: String(locale), datesListSorted: dateList as! [[EventDate]])
                } else {
                    self.datesListSortedWrapper = nil
                }
            }
        }
        Task {
            for await tempDateStatus in tucanManager.dateStatus {
                self.dateStatus = tempDateStatus
            }
        }
        Task {
            for await tempTimeSlotStatus in tucanManager.timeSlotStatus {
                self.timeSlotStatus = tempTimeSlotStatus
            }
        }
        Task {
            for await tempCourseStatus in tucanManager.courseStatus {
                self.courseStatus = tempCourseStatus
            }
        }
        Task {
            for await tempExamStatus in tucanManager.examStatus {
                self.examStatus = tempExamStatus
            }
        }
        Task {
            for await tempExamRegStatus in tucanManager.examRegStatus {
                self.examRegStatus = tempExamRegStatus
            }
        }
    }
    
    // General functions
    
    func checkToken() async {
        try? await tucanManager.checkToken()
    }
    
    
    // Page specific functions
    
    func updateTodayTimeSlots() async {
        try? await tucanManager.updateTodayTimeSlots()
    }
    
    func updateViewedExam(exam: Exam?) {
        tucanManager.updateViewedExam(exam: exam)
    }
    
    func updateExams() async {
        try? await tucanManager.updateExams()
    }
    
    func updateExamGradeStats(exam: Exam) async {
        try? await tucanManager.updateExamGradeStats(exam: exam)
    }
    
    func updateExamRegistrations() async {
        try? await tucanManager.updateExamRegistrations()
    }
    
    func updateExamRegistrationDetail(examReg: ExamRegistration) async {
        try? await tucanManager.updateExamRegistrationDetail(examRegistration: examReg)
    }
    
    func updateCourses() async {
        try? await tucanManager.updateCourses()
    }
    
    func getExamModifyPrompt(isRegister: Bool, args: String) async -> String {
        let registerID = try? await tucanManager.getExamModifyPrompt(isRegister: isRegister, arguments: args)
        return registerID ?? ""
    }
    
    func confirmExamModify(registerID: String) async -> Bool {
        do {
            let success = Bool(truncating: try await tucanManager.confirmExamModify(registerID: registerID))
            return success
        } catch {
            return false
        }
    }
}
