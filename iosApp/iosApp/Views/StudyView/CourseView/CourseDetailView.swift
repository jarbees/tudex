//  Created on 17.11.25.

import SwiftUI
import Shared

struct CourseDetailView: View {
    let course: Course
    
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var tabId: Int = 0
    
    var body: some View {
        // In theory, every course should have courseDetails.
        // It is nullable due to the way it is created.
        if let courseDetails = course.courseDetails {
            ScrollView {
                VStack {
                    Picker("Course Tabs", selection: $tabId) {
                        Text("Description").tag(0)
                        
                        if !courseDetails.groups.isEmpty {
                            Text("Groups").tag(1)
                        }
                        
                        if !courseDetails.regDeadlines.isEmpty {
                            Text("Deadlines").tag(2)
                        }
                        
                        if courseDetails.dates != nil || courseDetails.plenaryDates != nil {
                            Text("Dates").tag(3)
                        }
                    }.pickerStyle(.segmented)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical)
                .padding(.horizontal)

                VStack {
                    Group {
                        if tabId == 0 {
                            Text(courseDetails.details)
                                .padding(.bottom)
                        } else if tabId == 1 {
                            VStack(alignment: .leading) {
                                ForEach(courseDetails.groups, id: \.self ) { group in
                                    VStack(alignment: .leading) {
                                        Text(group.title)
                                            .font(.subheadline)
                                        Text(group.teachers)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                    .padding(.vertical, 4)
                                    .padding(.horizontal)
                                    
                                    if (group != courseDetails.groups.last) {
                                        Divider()
                                            .padding(.leading)
                                            .dividerListPadding()
                                    }
                                }
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Color(.secondarySystemGroupedBackground))
                            .rounded()
                            .padding(.bottom)
                        } else if tabId == 2 {
                            VStack {
                                ForEach(courseDetails.regDeadlines, id: \.self ) { regDeadline in
                                    VStack(alignment: .leading) {
                                        Text("Phase: \(regDeadline.phase)")
                                        Text("Block: \(regDeadline.block)")
                                        Text("Start: \(regDeadline.startDate)")
                                        Text("End registration: \(regDeadline.endRegisterDate)")
                                        Text("End cancellation: \(regDeadline.endDeregisterDate)")
                                        Text("Deadline for audit: \(regDeadline.endListenerDate)")
                                    }
                                    .padding(.vertical, 4)
                                    .padding(.horizontal)
                                    
                                    if (regDeadline != courseDetails.regDeadlines.last) {
                                        Divider()
                                            .padding(.leading)
                                            .dividerListPadding()
                                    }
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.vertical, 12)
                            .background(Color(.secondarySystemGroupedBackground))
                            .rounded()
                            .padding(.bottom)
                        } else if tabId == 3 {
                            VStack {
                                if let dates = courseDetails.dates {
                                    HeaderText("Group")
                                        .padding(.leading, -16)
                                    
                                    VStack {
                                        ForEach(dates, id: \.self ) { date in
                                            HStack {
                                                Text("\(date.date), \(date.startTime)-\(date.endTime)")
                                                    .font(.subheadline)
                                                
                                                Spacer()
                                                
                                                Text("\(date.room)")
                                                    .font(.subheadline)
                                            }
                                            .padding(.vertical, 4)
                                            .padding(.horizontal)
                                            
                                            if (date != dates.last) {
                                                Divider()
                                                    .padding(.leading)
                                                    .dividerListPadding()
                                            }
                                        }
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color(.secondarySystemGroupedBackground))
                                    .rounded()
                                    .padding(.bottom)
                                }
                                
                                if let plenaryDates = courseDetails.plenaryDates {
                                    HeaderText("Plenum")
                                        .padding(.leading, -16)
                                    VStack {
                                        ForEach(plenaryDates, id: \.self ) { date in
                                            HStack {
                                                Text("\(date.date), \(date.startTime)-\(date.endTime)")
                                                    .font(.subheadline)
                                                
                                                Spacer()
                                                
                                                Text("\(date.room)")
                                                    .font(.subheadline)
                                            }
                                            .padding(.vertical, 4)
                                            .padding(.horizontal)
                                            
                                            if (date != plenaryDates.last) {
                                                Divider()
                                                    .padding(.leading)
                                                    .dividerListPadding()
                                            }
                                        }
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                    .background(Color(.secondarySystemGroupedBackground))
                                    .rounded()
                                    .padding(.bottom)
                                }
                            }
                        }
                    }
                    .transition(.opacity.combined(with: .scale(scale: 0.995)))
                    .id(tabId)
                }
                .animation(.easeOut(duration: 0.125), value: tabId)
                .frame(maxWidth: .infinity)
                .padding(.horizontal)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitleAdaptive(course.courseName) {
                VStack {
                    Text("\(course.courseID) - \(course.semester)")
                        .font(.callout)
                        .foregroundStyle(Color(.secondaryLabel))
                    Text("\(course.courseName)")
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(Color(.label))
                        .fixedSize(horizontal: false, vertical: true)
                }
                .frame(maxWidth: .infinity, alignment: .center)
                .modify { view in
                    if #available(iOS 26.0, *) {
                        view.padding(.top, -16)
                    } else {
                        view
                    }
                }
            }
            .navigationTitleAccessoryCleanup()
    
            /*
            List {
                Section {
                    NavigationLink("Details") {
                        Text(courseDetails.details)
                    }
                    if !courseDetails.groups.isEmpty {
                        NavigationLink("Groups") {
                            List {
                                ForEach(courseDetails.groups, id: \.self ) { group in
                                    VStack(alignment: .leading) {
                                        Text(group.title)
                                        //.lineLimit(1)
                                            .font(.subheadline)
                                        Text(group.teachers)
                                        //.lineLimit(1)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                            }
                        }
                    }
                    
                    if !courseDetails.regDeadlines.isEmpty {
                        NavigationLink("Deadlines") {
                            Text(courseDetails.regDeadlines.first?.startDate ?? "")
                        }
                    }
                    
                    if !courseDetails.dates.isEmpty {
                        NavigationLink("Dates") {
                            List {
                                ForEach(courseDetails.dates, id: \.self ) { date in
                                    HStack {
                                        Text("\(date.date), \(date.startTime)-\(date.endTime)")
                                            .font(.subheadline)
                                        
                                        Spacer()
                                        
                                        Text("\(date.room)")
                                            .font(.subheadline)
                                    }
                                }
                            }
                        }
                    }
                    
                    Section {
                        if tabId == 0 {
                            Text(courseDetails.details)
                        }
                        if tabId == 1 {
                            List {
                                ForEach(courseDetails.groups, id: \.self ) { group in
                                    VStack(alignment: .leading) {
                                        Text(group.title)
                                        //.lineLimit(1)
                                            .font(.subheadline)
                                        Text(group.teachers)
                                        //.lineLimit(1)
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                    }
                                }
                            }
                        }
                        
                        if tabId == 2 {
                            Text(courseDetails.regDeadlines.first?.startDate ?? "")
                        }
                        
                        if tabId == 3 {
                            List {
                                ForEach(courseDetails.dates, id: \.self ) { date in
                                    HStack {
                                        Text("\(date.date), \(date.startTime)-\(date.endTime)")
                                            .font(.subheadline)
                                        
                                        Spacer()
                                        
                                        Text("\(date.room)")
                                            .font(.subheadline)
                                    }
                                }
                            }
                        }
                    }
                } header: {
                    VStack {
                        Text("\(course.courseID) - \(course.semester)")
                            .font(.callout)
                            .foregroundStyle(Color(.secondaryLabel))
                        Text("\(course.courseName)")
                            .fontWeight(.bold)
                            .multilineTextAlignment(.center)
                            .foregroundStyle(Color(.label))
                        Picker("Course Tabs", selection: $tabId) {
                            Text("Description").tag(0)
                            if !courseDetails.groups.isEmpty {
                                Text("Groups").tag(1)
                            }
                            
                            if !courseDetails.regDeadlines.isEmpty {
                                Text("Deadlines").tag(2)
                            }
                            
                            if !courseDetails.dates.isEmpty {
                                Text("Dates").tag(3)
                            }
                        }.pickerStyle(.segmented)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.bottom)
                }
            }
             */
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
