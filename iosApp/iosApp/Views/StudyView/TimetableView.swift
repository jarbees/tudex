//  Created on 04.05.25.

import SwiftUI
import Shared

struct TimetableView: View {
    @EnvironmentObject var tucanService: TucanService
    @EnvironmentObject var identityService: IdentityService
    @EnvironmentObject var settingsService: SettingsService
    
    @State private var selectedDateOffset = 0
    @State private var displayedWeekOffset = 0
    
    let dateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateFormat = "MMMM yyyy"
        return formatter
    }()
    
    let cal = Calendar.current
    
    var body: some View {
        VStack {
            if (tucanService.datesListSortedWrapper == nil) {
                if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                    identityService.tuLoginStatus == .ssoUnconfirmed
                ) {
                    LoginLoadingView()
                } else if tucanService.dateStatus == .loading {
                    RequestLoadingView(itemName: "Dates")
                } else {
                    EmptyView()
                }
            } else if settingsService.timeViewType == .week {
                TimeBodyView(selectedDateOffset: $selectedDateOffset)
                    // Using safeAreaInset instead of VStack alignes ScrollViews indicator properly
                    .safeAreaInset(edge: .top, spacing: 0) {
                        VStack {
                            TimeHeaderView(selectedDateOffset: $selectedDateOffset, displayedWeekOffset: $displayedWeekOffset)
                            Divider()
                        }
                        .background(Color(.systemBackground))
                    }
                    .transition(.opacity)
            } else if settingsService.timeViewType == .list {
                TimeListView()
                    .transition(.opacity)
            }
        }
        .onChange(of: selectedDateOffset) { newOffset in
            let targetWeekOffset = weekOffset(for: newOffset)
            
            if displayedWeekOffset != targetWeekOffset {
                displayedWeekOffset = targetWeekOffset
            }
        }
        .frame(maxWidth: .infinity)         // Fixes navigationTitle appearing too late
        .animation(.default.speed(2), value: settingsService.timeViewType)
        .navigationBarTitleDisplayMode(.inline)
        .navigationTitle("Timetable")
        // Accessory can't be seen in iOS 26, because inline title loads too late
        .toolbar {
            ToolbarItem(placement: .principal) {
                let month = dateFormatter.string(from: cal.date(byAdding: .day, value: selectedDateOffset, to: Date())!)
                
                HStack(spacing: 4) {
                    Text(settingsService.timeViewType == .list ? " " : "\(month)")     // Using "" instead of " " breaks
                    
                    if tucanService.datesListSortedWrapper != nil {
                        if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                            identityService.tuLoginStatus == .ssoUnconfirmed ||
                            tucanService.dateStatus == .loading
                        ) {
                            if settingsService.timeViewType == .list {
                                Text("Syncing")
                            }
                            ProgressView()
                                .padding(.top, 2)
                        }
                    }
                }
            }
            
            ToolbarItem {
                let pickerBinding = Binding(
                    get: { settingsService.timeViewType },
                    set: { settingsService.updateTimeViewType(to: $0) }
                )
                
                Menu {
                    Picker("Options", selection: pickerBinding) {
                        Label("Week", systemSymbol: .calendarDayTimelineLeading).tag(TimeViewType.week)
                        Label("List", systemSymbol: .listBullet).tag(TimeViewType.list)
                    }
                } label: {
                    if settingsService.timeViewType == .week {
                        Label("", systemSymbol: .calendarDayTimelineLeading)
                    } else {
                        Label("", systemSymbol: .listBullet)
                    }
                }
            }
            
            if #available(iOS 26.0, *) {
                ToolbarSpacer()
            }
        }
    }
    
    private func weekOffset(for dateOffset: Int) -> Int {
        let weekday = cal.component(.weekday, from: Date())
        let diffToMonday = weekday == 1 ? -6 : 2 - weekday

        return Int(
            floor(Double(dateOffset - diffToMonday) / 7)
        )
    }
    
    static func getDateFormatter(locale: String) -> DateFormatter? {
        if locale == "EN" {
            let englishFormatter: DateFormatter = {
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "EEE, d. MMM yyyy"
                // By default, in en_US_POSIX, the months don't get a dot after them
                dateFormatter.shortMonthSymbols = ["Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.", "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."]
                dateFormatter.shortWeekdaySymbols = ["Sun", "Mon", "Tue", "Wed", "Th", "Fri", "Sat"]
                //dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
                dateFormatter.timeZone = TimeZone.current
                dateFormatter.locale = Locale(identifier: "en_US_POSIX")
                return dateFormatter
            }()
            
            return englishFormatter
        } else if locale == "DE" {
            let germanFormatter: DateFormatter = {
                let dateFormatter = DateFormatter()
                dateFormatter.dateFormat = "EEE, d. MMM yyyy"
                // By default, it would be "Mo." instead of "Mo"; week starts with Sunday in DateFormatter
                // Also by default, März, Juni, Juli wouldn't be abbreviated and September would be Sept. instead of Sep.
                dateFormatter.shortMonthSymbols = ["Jan.", "Feb.", "Mär.", "Apr.", "Mai", "Jun.", "Jul.", "Aug.", "Sep.", "Okt.", "Nov.", "Dez."]
                dateFormatter.shortWeekdaySymbols = ["So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"]
                //dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
                dateFormatter.timeZone = TimeZone.current
                dateFormatter.locale = Locale(identifier: "de_DE_POSIX")
                return dateFormatter
            }()
            
            return germanFormatter
        }
        
        return nil
    }
}

// Views for Week View
    
extension TimetableView {
    private struct TimeHeaderView: View {
        @Binding var selectedDateOffset: Int
        @Binding var displayedWeekOffset: Int
        
        private let today = Date()
        private let cal = Calendar.current
        
        var body: some View {
            let weekday = cal.component(.weekday, from: today)
            let diffToMonday = weekday == 1 ? -6 : (2 - weekday)
            
            InfinitePager(selection: $displayedWeekOffset) { weeksFromToday in
                let monday = cal.date(byAdding: .day, value: diffToMonday + 7 * weeksFromToday, to: today)!
                let week = (0..<7).compactMap {
                    cal.date(byAdding: .day, value: $0, to: monday)
                }
                
                return HStack {
                    let selectedDate = cal.date(byAdding: .day, value: selectedDateOffset, to: today)!
                    
                    // week is enumerated to provide stable identity so that the circle animates correctly
                    ForEach(Array(week.enumerated()), id: \.offset) { _, day in
                        
                        let isSelected = cal.isDate(day, inSameDayAs: selectedDate)
                        let isToday = cal.isDateInToday(day)
                        
                        VStack(spacing: 4) {
                            Text(day, format: .dateTime.weekday(.abbreviated))
                                .font(.footnote)
                                .foregroundStyle(Color(.secondaryLabel))
                            
                            VStack {
                                Circle()
                                    .fill(isSelected
                                          ? isToday ? Color.accentColor : Color(.label)
                                          : .clear)
                                    .scaleEffect(isSelected ? 1 : 0)
                                    .padding(4)
                            }
                            .overlay {
                                Text(day, format: .dateTime.day())
                                    .font(.headline)
                                    .foregroundStyle(isSelected ? Color(.systemBackground)
                                                     : isToday ? Color.accentColor : Color(.label))
                            }
                            .animation(.default.speed(2.5), value: isSelected)
                            .onTapGesture {
                                if let offset = cal.dateComponents([.day], from: today, to: day).day {
                                    withAnimation(.default.speed(2.5)) {
                                        selectedDateOffset = offset
                                    }
                                }
                            }
                        }
                    }
                }
            }
            .padding(.horizontal)
            .padding(.bottom, 8)
        }
    }
    
    private struct TimeBodyView: View {
        @Binding var selectedDateOffset: Int
        @State private var metrics = TimeGridMetrics()
        
        @EnvironmentObject var tucanService: TucanService
        @ScaledMetric private var rowPadding: CGFloat = 12
        
        @State private var hasAppeared = false   // Prevents view resetting when navigating back to it
        
        var body: some View {
            let cal = Calendar.current
            let today = Date()
            
            // In iOS 18, this could be done with ScrollPosition (should it though?)
            return ScrollViewReader { proxy in
                ScrollView(.vertical) {
                    TimeGridView(metrics: $metrics)
                        .hidden()
                        .overlay {
                            InfinitePager(selection: $selectedDateOffset) { daysFromToday in
                                TimeGridView()
                                    .overlay(alignment: .top) {
                                        ZStack(alignment: .top) {
                                            if daysFromToday == 0 {
                                                TimeMarker(
                                                    topOffset: metrics.topOffset,
                                                    rowHeight: metrics.rowHeight
                                                )
                                                .padding(.leading, -8)
                                                .padding(.trailing, -16)
                                            }
                                            
                                            if let datesWrapper = tucanService.datesListSortedWrapper,
                                               let dateFormatter = TimetableView.getDateFormatter(locale: datesWrapper.locale)
                                            {
                                                let date = cal.date(byAdding: .day, value: daysFromToday, to: today)!
                                                let dayString = dateFormatter.string(from: date)
                                                
                                                let sortedDatesLists = datesWrapper.datesListSorted
                                                
                                                if let dates = sortedDatesLists.first(where: {
                                                    $0.first?.date == dayString
                                                }) {
                                                    TimeSlotViewCoordinator(
                                                        dates: dates,
                                                        topOffset: metrics.topOffset,
                                                        rowHeight: metrics.rowHeight
                                                    )
                                                    .padding(.leading, metrics.leadingPadding + 16)
                                                }
                                            }
                                        }
                                    }
                                    .padding(.horizontal)
                            }
                        }
                    
                    Spacer(minLength: 12)
                }
                .onAppear {
                    if !hasAppeared {
                        hasAppeared = true
                        
                        var hour = Calendar.current.component(.hour, from: Date())
                        
                        if hour >= 1 {
                            hour = hour - 1
                        }
                        
                        DispatchQueue.main.async {
                            proxy.scrollTo(hour, anchor: .top)
                        }
                    }
                }
                .toolbar {
                    ToolbarItem {
                        Button("", systemSymbol: .calendarBadgeClock) {
                            selectedDateOffset = 0
                            
                            var hour = Calendar.current.component(.hour, from: Date())
                            
                            if hour >= 1 {
                                hour = hour - 1
                            }
                            
                            withAnimation {
                                proxy.scrollTo(hour, anchor: .top)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
    }
    
    struct TimeGridMetrics: Equatable {
        var topOffset: CGFloat = 0
        var rowHeight: CGFloat = 0
        var leadingPadding: CGFloat = 0
    }
    
    struct TimeGridView: View {
        @ScaledMetric private var rowPadding: CGFloat = 12
        
        @State private var gridHeight: CGFloat = 0
        var metrics: Binding<TimeGridMetrics>? = nil

        var body: some View {
            Grid(alignment: .trailing) {
                ForEach(0...24, id: \.self) { hour in
                    GridRow {
                        Text(String(format: "%02d:00", hour))
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .modify { view in
                                if let metrics, hour == 0 {
                                    view.onGeometryChange(for: CGSize.self) { geo in
                                        geo.size
                                    } action: { size in
                                        if size.height > 0 {
                                            let newTopOffset = size.height / 2 + rowPadding
                                            
                                            metrics.wrappedValue.topOffset = newTopOffset
                                            metrics.wrappedValue.rowHeight = (gridHeight - (2 * newTopOffset)) / 24
                                        }
                                        if size.width > 0 {
                                            metrics.wrappedValue.leadingPadding = size.width
                                        }
                                    }
                                }
                            }

                        Divider()
                            .padding(.trailing, -4)
                    }
                    .padding(.vertical, rowPadding)
                    .id(hour)
                }
            }
            .modify { view in
                if let metrics {
                    view.onGeometryChange(for: CGFloat.self) { geo in
                        geo.size.height
                    } action: { newGridHeight in
                        if newGridHeight > 0 {
                            gridHeight = newGridHeight
                            metrics.wrappedValue.rowHeight = (newGridHeight - (2 * metrics.wrappedValue.topOffset)) / 24
                        }
                    }
                }
            }
        }
    }
    
    struct TimeMarker: View {
        let topOffset: CGFloat
        let rowHeight: CGFloat
        
        var body: some View {
            TimelineView(.everyMinute) { context in
                let hour = Calendar.current.component(.hour, from: context.date)
                let minute = Calendar.current.component(.minute, from: context.date)
                let minuteOfToday = CGFloat(hour * 60 + minute)
                
                HStack(spacing: 0) {
                    Text(context.date, style: .time)
                        .font(.subheadline)
                        .foregroundStyle(.white)
                        .bold()
                        .padding(.vertical, 2)
                        .padding(.horizontal, 8)
                        .background(Color.accentColor)
                        .cornerRadius(10)
                    
                    Rectangle()
                        .fill(Color.accentColor)
                        .frame(height: 2)
                }
                .offset(y: topOffset / 2 + (rowHeight * minuteOfToday/60))
            }
        }
    }
    
    // Theoretically, should two dates be identical, the layout would break since the ForEachs use self as id
    struct TimeSlotViewCoordinator: View {
        let dates: [EventDate]
        let selectedDate: EventDate?       // This is used to mark a Date as selected in DetailView
        
        let topOffset: CGFloat
        let rowHeight: CGFloat
        
        init(dates: [EventDate], topOffset: CGFloat, rowHeight: CGFloat, selectedDate: EventDate? = nil) {
            self.dates = dates
            self.topOffset = topOffset
            self.rowHeight = rowHeight
            self.selectedDate = selectedDate
        }
        
        var body: some View {
            let sortedDates = dates.sorted(by: {
                if $0.startTimeMins == $1.startTimeMins {
                    return $0.endTimeMins < $1.endTimeMins
                }
                return $0.startTimeMins < $1.startTimeMins
            })
            
            let groupedDateExts: [(date: EventDate, spacesBefore: Int, spacesAfter: Int)] = determineTimeGroupings(for: sortedDates)
            
            ForEach(groupedDateExts, id: \.date) { dateExt in
                HStack(spacing: 4) {
                    ForEach(0..<dateExt.spacesBefore, id: \.self) { index in
                        Spacer()
                            .layoutPriority(0)
                    }
                    
                    let isSelected = dateExt.date == selectedDate
                    
                    TimeSlotView(date: dateExt.date, isSelected: isSelected, topOffset: topOffset, rowHeight: rowHeight)
                    
                    ForEach(0..<dateExt.spacesAfter, id: \.self) { index in
                        Spacer()
                            .layoutPriority(0)
                    }
                }
            }
        }
        
        /// This requires the Array to be sorted by StartTime
        func determineTimeGroupings(for dates: [EventDate]) -> [(date: EventDate, spacesBefore: Int, spacesAfter: Int)] {

            var result: [(date: EventDate, spacesBefore: Int, spacesAfter: Int)] = []
            var group: [(date: EventDate, column: Int)] = []
            var columnEnds: [Int] = []      // current end time per column

            func flushGroup() {
                let columnCount = columnEnds.count

                for item in group {
                    result.append((
                        date: item.date,
                        spacesBefore: item.column,
                        spacesAfter: columnCount - item.column - 1
                    ))
                }

                group.removeAll()
                columnEnds.removeAll()
            }

            for date in dates {
                let start = Int(date.startTimeMins)
                let end = Int(date.endTimeMins)

                if !group.isEmpty && columnEnds.allSatisfy({ $0 <= start }) {
                    flushGroup()
                }

                if let column = columnEnds.firstIndex(where: { $0 <= start }) {
                    group.append((date, column))
                    columnEnds[column] = end
                } else {
                    group.append((date, columnEnds.count))
                    columnEnds.append(end)
                }
            }

            flushGroup()
            return result
        }
    }
    
    private struct TimeSlotView: View {
        let date: EventDate
        let isSelected: Bool
        
        let topOffset: CGFloat
        let rowHeight: CGFloat
        
        var body: some View {
            let height = (CGFloat(date.endTimeMins - date.startTimeMins) / 60) * rowHeight - 2
            
            TimeSlotNavigationLink(content: {
                HStack(spacing: 4) {
                    Rectangle()
                        .frame(width: 4)
                        .cornerRadius(4)
                        .padding(6)
                    
                    VStack(alignment: .leading) {
                        Text(date.courseName)
                            .lineLimit(2)
                            .bold()
                            .font(.subheadline)
                        
                        let metadataLayout = height <= 70
                            ? AnyLayout(HStackLayout(alignment: .top))
                            : AnyLayout(VStackLayout(alignment: .leading))
                        
                        metadataLayout {
                            if let examDate = date as? EventDate.ExamDate {
                                Text(examDate.examType)
                                    .lineLimit(1)
                                    .font(.footnote)
                            }
                            
                            HStack(alignment: .top, spacing: 4) {
                                Image(systemSymbol: .clock)
                                Text("\(date.startTime)-\(date.endTime)")
                            }
                            .font(.footnote)
                            
                            if let courseDate = date as? EventDate.CourseDate {
                                HStack(alignment: .top, spacing: 4) {
                                    Image(systemSymbol: .mappinAndEllipse)
                                    Text(courseDate.room)
                                }
                                .font(.footnote)
                            }
                        }
                        
                        Spacer()
                            .frame(minHeight: 0)
                    }
                    .padding(.vertical, 8)
                }
                .frame(height: height <= 0 ? 1 : height)
                .frame(maxWidth: .infinity, alignment: .leading)
                .cornerRadius(8)
            }, destination: {
                TimetableDetailView(date: date)
            }, isSelected: isSelected, isExam: date is EventDate.ExamDate)
            .offset(y: topOffset + rowHeight * (CGFloat(date.startTimeMins)/60))
        }
    }
    
    private struct TimeSlotNavigationLink<Destination: View, Content: View>: View {
        let content: () -> Content
        let destination: () -> Destination
        
        let isSelected: Bool
        let isExam: Bool
        
        @State private var isNavigating: Bool = false
        
        var body: some View {
            NavigationLinkButton(action: {
                if !isSelected {
                    isNavigating = true
                }
            }, content: content, destination: destination, isNavigating: $isNavigating)
            .navButtonStyle(.plain)
            .buttonStyle(TimeSlotNavigationLinkStyle(isSelected: isSelected, isExam: isExam, isNavigating: $isNavigating))
        }
    }
    
    private struct TimeSlotNavigationLinkStyle: ButtonStyle {
        let isSelected: Bool
        let isExam: Bool
        
        @Binding var isNavigating: Bool
        
        func makeBody(configuration: Configuration) -> some View {
            let isActive = configuration.isPressed || isNavigating || isSelected
            
            let accentColor = isExam ? Color.orange : Color.accentColor
            
            return configuration.label
                .foregroundStyle(isActive ? Color(.white) : accentColor)
                .background {
                    Rectangle()
                        .modify { view in
                            if isActive {
                                view.fill(accentColor)
                            } else {
                                if #available(iOS 17.0, *) {
                                    view.fill(accentColor.quaternary)
                                } else {
                                    view.fill(accentColor.opacity(0.5))
                                }
                            }
                        }
                        .cornerRadius(8)
                }
                .animation(.default, value: configuration.isPressed)
                .animation(.default.speed(2), value: isNavigating)
            /*
             .background(Color((configuration.isPressed || isNavigating) ? .quaternaryLabel : .secondarySystemGroupedBackground))
             .animation(.default, value: configuration.isPressed)
             .animation(.default.speed(2), value: isNavigating)
             */
        }
    }
    
    // Views for List View
    
    private struct TimeListView: View {
        @EnvironmentObject var tucanService: TucanService
        
        @State private var hasAppeared = false   // Prevents view resetting when navigating back to it
        
        var body: some View {
            if let datesListSortedWrapper = tucanService.datesListSortedWrapper,
               let dateFormatter = TimetableView.getDateFormatter(locale: datesListSortedWrapper.locale)
            {
                ScrollViewReader { proxy in
                    List {
                        let datesListSorted = datesListSortedWrapper.datesListSorted
                        let todayDateString = dateFormatter.string(from: .now)
                        
                        // Add an empty List representing today, if today isnt in datesListSorted
                        var datesListWithToday: [[EventDate]] {
                            let today = Date()
                            var lists = datesListSorted
                            
                            if let firstIndexAfterToday = lists.firstIndex(where: { dateList in
                                if let dateString = dateList.first?.date,
                                   let date = dateFormatter.date(from: dateString)
                                {
                                    if date > today {
                                        return true
                                    }
                                }
                                
                                return false
                            }) {
                                if firstIndexAfterToday >= 1 {
                                    let potentialToday = lists[firstIndexAfterToday - 1].first?.date
                                    
                                    if potentialToday == todayDateString {
                                        return lists
                                    }
                                }
                                
                                lists.insert([], at: firstIndexAfterToday)
                            } else {
                                lists.append([])
                            }
                            
                            return lists
                        }
                        
                        ForEach(datesListWithToday, id: \.self) { dates in
                            let date = dates.first?.date ?? todayDateString

                            Section {
                                if dates.isEmpty {
                                    Text("No events today")
                                        .font(.subheadline)
                                        .bold()
                                        .padding(.leading, 8)
                                } else {
                                    ForEach(dates, id: \.self) { event in
                                        TimeListSlot(date: event)
                                    }
                                }
                            } header: {
                                Text(date)
                                    .foregroundStyle(
                                        date == todayDateString ? Color.accentColor : .primary
                                    )
                                    .background {
                                        Color(.systemBackground)
                                            .blur(radius: 8)
                                            .padding(.horizontal, -8)
                                    }
                            }
                            .id(date)
                            .listRowInsets(.init(top: 8, leading: 8, bottom: 8, trailing: 16))
                            .listSectionSeparator(.hidden)
                        }
                        
                        Spacer(minLength: 16)
                            .listRowSeparator(.hidden)
                    }
                    .opacity(hasAppeared ? 1 : 0)
                    .environment(\.defaultMinListRowHeight, 0)
                    .listStyle(.inset)
                    .onAppear {
                        if !hasAppeared {
                            Task {
                                proxy.scrollTo(dateFormatter.string(from: .now), anchor: .top)
                                hasAppeared = true
                            }
                        }
                    }
                    .toolbar {
                        Button("", systemSymbol: .calendarBadgeClock) {
                            withAnimation {
                                proxy.scrollTo(dateFormatter.string(from: .now), anchor: .top)
                            }
                        }
                        .padding()
                    }
                }
            }
        }
    }
    
    private struct TimeListSlot: View {
        let date: EventDate
        
        var body: some View {
            NavigationLink(destination: TimetableDetailView(date: date), label: {
                HStack(spacing: 4) {
                    Rectangle()
                        .fill(date is EventDate.ExamDate ? Color.orange : Color.accentColor)
                        .frame(width: 4)
                        .cornerRadius(4)
                        .padding(.horizontal, 6)
                    
                    VStack(alignment: .leading) {
                        Text(date.courseName)
                            .lineLimit(1)
                            .bold()
                            .font(.subheadline)
                        
                        if let examDate = date as? EventDate.ExamDate {
                            Text(examDate.examType)
                                .lineLimit(1)
                                .font(.footnote)
                        }
                        
                        HStack {
                            HStack(alignment: .top, spacing: 4) {
                                Image(systemSymbol: .clock)
                                Text("\(date.startTime)-\(date.endTime)")
                            }
                            .font(.footnote)
                            
                            if let courseDate = date as? EventDate.CourseDate {
                                HStack(alignment: .top, spacing: 4) {
                                    Image(systemSymbol: .mappinAndEllipse)
                                    Text(courseDate.room)
                                }
                                .font(.footnote)
                            }
                        }
                        .foregroundStyle(.secondary)
                    }
                    .padding(.vertical, 2)
                }
            })
        }
    }
}


#Preview("Timetable") {
    NavigationStack {
        TimetableView()
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
    .environmentObject(SettingsService())
}
