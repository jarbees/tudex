//  Created on 04.05.25.

import SwiftUI
import Shared
import SFSafeSymbols

struct ExamView: View {
    @EnvironmentObject var tucanService: TucanService
    @EnvironmentObject var identityService: IdentityService
    @State private var isNavigating: Bool = false
    
    var body: some View {
        Group {
            if (tucanService.exams.isEmpty) {
                if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                    identityService.tuLoginStatus == .ssoUnconfirmed
                ) {
                    LoginLoadingView()
                } else if tucanService.examStatus == .loading {
                    RequestLoadingView(itemName: "Exams")
                } else {
                    EmptyView()
                }
            } else {
                RefreshableScrollView (
                    action: {
                        Task {
                            await tucanService.updateExams()
                        }
                    }, content: {
                        VStack {
                            // TODO: this
                            /*
                             HeaderText("GPA")
                             
                             HStack {
                             CommonActionWithNavigation(text: "Total", symbol: .calendar, view: {TimetableView()})
                             CommonActionWithNavigation(text: "Semester", symbol: .globe, view: {CourseView()})
                             }
                             .padding(.bottom)
                             .padding(.horizontal)
                             */
                            ForEach (tucanService.exams.filter { !$0.isEmpty }, id: \.first!.semester) { examList in
                                HeaderText("\(examList.first?.semester ?? "Unknown")")
                                GroupedList(padding: 12) {
                                    ForEach(examList, id: \.id) { exam in
                                        ExamListCell(exam: exam)
                                    }
                                }
                                .padding(.horizontal)
                                .padding(.bottom)
                            }
                        }
                        .animation(.default, value: tucanService.exams)
                    })
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .navigationTitle("Exams")
        .navigationTitleAccessory(accessory: { isLarge in
            Group {
                if !tucanService.exams.isEmpty {
                    if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                        identityService.tuLoginStatus == .ssoUnconfirmed ||
                        tucanService.examStatus == .loading
                    ) {
                        ProgressView()
                            .scaleEffect(isLarge ? 1.2 : 1)
                            .padding(.top, isLarge ? 0 : 2)
                            .padding(.leading, isLarge ? 8 : 4)
                    }
                }
            }
        })
            
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                NavigationLinkButton(action: {
                    Task {
                        await tucanService.checkToken()
                    }
                    isNavigating = true
                }, content: {
                    Text("Register")
                }, destination: {
                    ExamRegisterView()
                }, isNavigating: $isNavigating)
                .navButtonStyle(.plain)
            }
        }
    }
}

private struct ExamListCell: View {
    @EnvironmentObject var tucanService: TucanService
    @EnvironmentObject var identityService: IdentityService
    
    let exam: Exam
    
    @State private var isNavigating: Bool = false
    @State private var isLoading: Bool = false
    
    var body: some View {
        NavigationLinkButton(action: {
            if (exam.gradeStats == nil && exam.grade != "") {
                isLoading = true
                Task {
                    await tucanService.updateExamGradeStats(exam: exam)
                    isLoading = false
                    withAnimation {
                        isNavigating = true
                    }
                }
            }
            else {
                withAnimation {
                    isNavigating = true
                }
            }
        }, content: {
            CellContent(isLoading: $isLoading, exam: exam)
        }, destination: { ExamDetailView(exam: exam) }, isNavigating: $isNavigating)
    }
}

private struct CellContent: View {
    @Binding var isLoading: Bool
    
    let exam: Exam
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(exam.courseName)
                    .lineLimit(1)
                    .font(.subheadline)
                Text(exam.examType)
                    .lineLimit(1)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            if (exam.grade == "5,0" || exam.grade == "nb") {
                Image(systemSymbol: .xmarkSealFill)
                    .foregroundStyle(Color.red)
            }
            else if exam.grade == "" {
                ZStack {
                    Image(systemSymbol: .seal)
                    Image(systemSymbol: .questionmark)
                        .font(.system(size: 8, weight: .bold))
                }
            }
            else {
                Image(systemSymbol: .checkmarkSealFill)
                    .foregroundStyle(Color.green)
            }
            
            if (isLoading) {
                ProgressView()
                    .padding(.trailing, 12)
                    .padding(.leading, -6)
            } else {
                Image(systemSymbol: .chevronRight)
                    .foregroundStyle(Color(.tertiaryLabel))
                    .font(.system(size: 14, weight: .semibold))
                    .padding(.trailing)
            }
        }
        .foregroundStyle(.primary)
    }
}

#Preview {
    NavigationStack {
        ExamView()
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}
