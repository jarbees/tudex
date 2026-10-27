//  Created on 04.05.25.

import SwiftUI
import Shared


struct CourseView: View {
    @EnvironmentObject var tucanService: TucanService
    @EnvironmentObject var identityService: IdentityService
    @State private var offset: CGFloat = 0
    
    var body: some View {
        Group {
            if (tucanService.courses.isEmpty) {
                if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                    identityService.tuLoginStatus == .ssoUnconfirmed
                ) {
                    LoginLoadingView()
                } else if tucanService.courseStatus == .loading {
                    RequestLoadingView(itemName: "Courses")
                } else {
                    EmptyView()
                }
            } else {
                RefreshableScrollView (
                    action: {
                        Task {
                            await tucanService.updateCourses()
                        }
                    }, content: {
                        ScrollViewContent()
                    })
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        .navigationTitle("Courses")
        .navigationTitleAccessory(accessory: { isLarge in
            Group {
                if !tucanService.courses.isEmpty {
                    if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                        identityService.tuLoginStatus == .ssoUnconfirmed ||
                        tucanService.courseStatus == .loading
                    ) {
                        ProgressView()
                            .scaleEffect(isLarge ? 1.2 : 1)
                            .padding(.top, isLarge ? 0 : 2)
                            .padding(.leading, isLarge ? 8 : 4)
                    }
                }
            }
        })
    }
}

private struct ScrollViewContent: View {
    @EnvironmentObject private var tucanService: TucanService
    
    var body: some View {
        VStack {
            ForEach(tucanService.courses, id: \.self) { moduleList in
                HeaderTextNotLocal(moduleList.first?.first?.semester ?? "")
                
                GroupedList(padding: 12) {
                    ForEach(moduleList, id: \.self) { courseList in
                        GroupDisclosureGroup(padding: 12) {
                            Text(courseList[0].moduleName)
                                .font(.subheadline)
                        } content: {
                            ForEach(courseList, id: \.id) { item in
                                CourseItem(course: item, padding: 32)
                            }
                        }
                    }
                }
                .padding(.bottom)
                .padding(.horizontal)
            }
        }
    }
}
    
private struct CourseItem: View {
    let course: Course
    let padding: CGFloat
    
    var body: some View {
        NavigationLinkButton {
            HStack {
                VStack(alignment: .leading) {
                    Text(course.courseName)
                        .lineLimit(1)
                        .font(.subheadline)
                    Text(course.courseID)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                
                Spacer()
                
                Image(systemSymbol: .chevronRight)
                    .foregroundStyle(Color(.tertiaryLabel))
                    .font(.system(size: 14, weight: .semibold))
                    .padding(.trailing)
                    .transition(.move(edge: .trailing))
            }
        } destination: {
            CourseDetailView(course: course)
        }
    }
}

#Preview {
    NavigationStack {
        CourseView()
            .environmentObject(TucanService())
            .environmentObject(IdentityService(previewMode: true))
    }
}
