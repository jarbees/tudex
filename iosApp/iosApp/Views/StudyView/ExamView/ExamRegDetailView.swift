//  Created on 22.06.25.

import SwiftUI
import Shared

struct ExamRegDetailView: View {
    @EnvironmentObject private var tucanService: TucanService
    
    @State var examReg: ExamRegistration

    // I am writing this as I implement the Android equivalent, so this may be slightly inaccurate.
    // I suspect in hindsight, that the View hadn't refreshed without this, since tucanService.updateExamRegistrationDetail(examReg: examReg)
    // is a mutating function, meaning the tucanService.examRegistrations property isn't changed.
    // This also means, the screen doesn't react to external changes to examRegistration (which currently don't exist)
    // The real fix would be to remove the mutability of ExamRegistration to ensure that an entirely new object is created.
    @State private var refresh = false
    
    var body: some View {
        List {
            Section {
                ListCell("Semester", examReg.semester)
                ListCell("Course", examReg.courseID + " - " + examReg.courseName)
                ListCell("Exam Type", examReg.examType)
            }

            Section {
                ListCell("Registration Period", examReg.registerPeriod)
                ListCell("Cancellation Period", examReg.cancelPeriod)
            }

            Section {
                ListCell("Examination Date", examReg.examinationDate)
                ListCell("Examiners", examReg.examiners)
                ListCell("Rooms", examReg.room.joined(separator: ", "))
            }

            // There exists an attribute examReg.roomArgs which provides a link to Tucans Room Overview
        }
        .animation(.default, value: refresh)
        .background(Color.clear.disabled(refresh))
        .navigationTitle("Details")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            Task {
                await tucanService.updateExamRegistrationDetail(examReg: examReg)
                refresh.toggle()
            }
        }
    }
}

extension ExamRegDetailView {
    struct ListCell: View {
        
        init(_ headline: LocalizedStringKey, _ text: String) {
            self.headline = headline
            self.text = text
        }
        
        let headline: LocalizedStringKey
        let text: String
        
        var body: some View {
            if (text != "") {
                VStack(alignment: .leading) {
                    Text(headline)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text(text)
                        .font(.subheadline)
                }
                .padding(.vertical, 1)
            }
        }
    }
}

#Preview {
    let examReg = ExamRegistration(semester: "SoSe 2026", courseID: "40-0000-01", courseName: "Introduction to Computer", examType: "Oral/Written Exam", examinationDate: "Mi, 29. Jul. 2026 09:00-13:00", registerOption: .register, registerArgs: "", additionalInfoArgs: "", registerPeriod: "01.06.26 00:00 - 04.07.26 23:59", cancelPeriod: "01.06.26 00:00 - 04.08.26 23:59", examiners: "Prof. Tuffel", room: ["S101/A1", "S101/A01"], roomArgs: ["", ""])
    
    NavigationStack {
        ExamRegDetailView(examReg: examReg)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}
