//  Created on 04.05.25.

import SwiftUI
import Shared


struct CourseView: View {
    @EnvironmentObject var tucanService: TucanService
    @State private var offset: CGFloat = 0
    
    var body: some View {
        Group {
            if (tucanService.courses.isEmpty) {
                ContentStatusView(status: tucanService.courseStatus, itemName: "Courses", refresh: tucanService.updateCourses)
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
        .navigationLoadingTitle(
            title: "Courses",
            itemStatus: tucanService.courseStatus,
            itemIsEmpty: tucanService.courses.isEmpty,
            itemName: "Courses",
            refreshAction: tucanService.updateCourses
        )
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
