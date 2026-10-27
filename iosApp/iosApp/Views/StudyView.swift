//  Created on 03.05.25.

import SwiftUI
import Shared
import SFSafeSymbols
import BetterSafariView

struct StudyView: View {
    @EnvironmentObject private var tucanService: TucanService
    @EnvironmentObject private var identityService: IdentityService
    
    @StateObject private var viewModel = OAuthViewModel()
    
    @State private var isLoginSheetPresented = false
    @State private var skipBasicLogin = false

    var body: some View {
        NavigationStack {
            RefreshableScrollView(
                isEnabled: identityService.tuLoginStatus == .sso ||
                            identityService.tuLoginStatus == .mfa ||
                            identityService.tuLoginStatus == .demo,
                action: {
                    Task {
                        await tucanService.updateTodayTimeSlots()
                    }
                }, content: {
                    VStack {
                        if identityService.tuLoginStatus == .none {
                            HeaderText("TUCaN Features")
                            LoginLockedCardView(isSheetPresented: $isLoginSheetPresented, skipBasicLogin: $skipBasicLogin)
                                .padding(.horizontal)
                                .padding(.bottom)
                            
                            
                            if (identityService.totpSecretExists) {
                                HeaderText("2FA")
                                CommonActionWithNavigation(text: "2FA Codes", symbol: .lockRotation, view: { TotpCodeView() })
                                    .padding(.horizontal)
                                    .padding(.bottom)
                            }
                                
                        } else {
                            CurrentEventsView()
                                .padding(.bottom)
                            
                            HeaderText("Frequently used")
                            FrequentlyUsedView()
                                .padding(.bottom)
                            
                            if (identityService.tuLoginStatus == .mfa ||
                                identityService.tuLoginStatus == .mfaUnconfirmed ||
                                identityService.tuLoginStatus == .mfaTucanError)
                            {
                                HeaderText("Identity")
                                HStack {
                                    CommonActionWithNavigation(text: "2FA Codes", symbol: .lockRotation, view: {TotpCodeView()})
                                    CommonActionWithNavigation(text: "Student ID", symbol: .personTextRectangle, view: {StudentIDView()})
                                }
                                .padding(.horizontal)
                                .padding(.bottom)
                            }
                            
                            HeaderText("TUCaN")
                            TucanActionsView()
                                .padding(.bottom)
                        }
                        
                        HeaderText("External Services")
                        ExternalLinksView()
                            .padding(.bottom)
                    }
                    .sheet(isPresented: $isLoginSheetPresented) { [skipBasicLogin] in
                        TULoginView(isPresented: $isLoginSheetPresented, skipBasicLogin: skipBasicLogin)
                    }
                })
            .scrollIndicators(ScrollIndicatorVisibility.hidden)
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Home")
            .navigationTitleAccessoryCleanup()
            .navigationBarTitleDisplayMode(.large)
        }
        .transition(.opacity)
    }
}

private struct IconTextPair: Identifiable {
    var id = UUID()
    var text: LocalizedStringKey
    var symbol: SFSymbol? = nil
    var view: AnyView? = nil
    //var view: Content
}


private struct CurrentEventsNewsView: View {
    let itemWidth: CGFloat
    
    var body: some View {
        VStack {
            HeaderText("News")
                .padding(.leading, -16)
            VStack {
                VStack {
                    Spacer()
                    Text("Coming soon!")
                    Spacer()
                    Spacer()
                }
                .padding([.top, .leading], 8)
            }
            .frame(height: 100)
            .frame(width: itemWidth)
            .background(Color(.secondarySystemGroupedBackground))
            .rounded(liquidCornerRadius: 20)
            //.cornerRadius(10)
        }
    }
}

private struct CurrentEventsItem: View {
    @EnvironmentObject private var tucanService: TucanService
    
    let timeSlot: TimeSlot
    let itemWidth: CGFloat
    
    @State private var isNavigating: Bool = false
    
    var body: some View {
        var courseDate: EventDate? {
            let locale = tucanService.datesListSortedWrapper?.locale ?? "DE"
            let dateFormatter = TimetableView.getDateFormatter(locale: locale)!     // Only null if locale != "DE" or "EN" which is impossible here
            let date = dateFormatter.string(from: .now)
            
            let timeSplit = timeSlot.time.replacing(" ", with: "").split(separator: "-")
            guard let startTime = timeSplit.first else { return nil }
            guard let endTime = timeSplit.last else { return nil }
            
            let startTimeSplit = startTime.split(separator: ":")
            let endTimeSplit = endTime.split(separator: ":")
            
            guard let startTimeHour = startTimeSplit.first else { return nil }
            guard let startTimeMin = startTimeSplit.last else { return nil }
            let startTimeMins = Int32(startTimeHour)! * 60 + Int32(startTimeMin)!
            
            guard let endTimeHour = endTimeSplit.first else { return nil }
            guard let endTimeMin = endTimeSplit.last else { return nil }
            let endTimeMins = Int32(endTimeHour)! * 60 + Int32(endTimeMin)!
            
            return EventDate.CourseDate(courseName: timeSlot.courseName, date: date, startTime: String(startTime), endTime: String(endTime), startTimeMins: startTimeMins, endTimeMins: endTimeMins, room: timeSlot.room, teachers: timeSlot.instructor)
        }
        
        NavigationLinkButton(action: {
            if courseDate != nil {
                isNavigating = true
            }
        }, content: {
            VStack {
                Text(timeSlot.courseName)
                //Text(timeSlot.id)
                    .frame(maxHeight: .infinity, alignment: .center)
                    .padding(.horizontal, 8)
                    .foregroundStyle(.primary)
                    .multilineTextAlignment(.center)
                    .font(.system(.body, weight: .bold))
                    .padding(.top, 8)
                    .font(.body)
                HStack {
                    Text("\(timeSlot.day),\n\(timeSlot.time.replacingOccurrences(of: " ", with: ""))")
                        .font(.caption)
                        .padding(.leading, 8)
                        .padding(.bottom, 8)
                        .foregroundStyle(.secondary)
                    
                    Spacer()
                    Text("\(timeSlot.courseID.split(separator: "-").last ?? "")\n\(timeSlot.room)")
                        .font(.caption)
                        .padding(.trailing, 8)
                        .padding(.bottom, 8)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.trailing)
                }
            }
            
        }, destination: {
            Group {
                if let date = courseDate {
                    TimetableDetailView(date: date)
                }
            }
        }, isNavigating: $isNavigating)
            .background(Color(.secondarySystemGroupedBackground))
            .rounded(liquidCornerRadius: 20)
            //.cornerRadius(10)
            .frame(width: itemWidth)
            .frame(height: 100)
    }
}

private struct CurrentEventsView: View {
    @EnvironmentObject private var identityService: IdentityService
    @EnvironmentObject private var tucanService: TucanService
    @State private var itemWidth: CGFloat = 160
    
    @State private var showTucanErrorSheet: Bool = false
    
    @State private var timeSlots: [TimeSlot] = []
    
    let minItemWidth = 160  // smallest screen (iPhone 12 mini) should fit two elements
    private var itemsCount: Int {
        timeSlots.count + 1
    }
    
    var body: some View {
        TimelineView(.everyMinute) { context in
            ScrollView(.horizontal, showsIndicators: false) {
                HStack {
                    Spacer()
                        .frame(width: 16)
                    
                    CurrentEventsNewsView(itemWidth: itemWidth)
                    ZStack {
                        VStack {
                            ProgressView()
                            Text("Loading today's time slots")
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal)
                        }
                        .frame(width: itemWidth)
                        .padding(.top, 32)
                        .opacity(tucanService.timeSlotStatus == .loading ? 1 : 0)
                        
                        
                        VStack {
                            ProgressView()
                            Text("Logging in with TU ID")
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal)
                        }
                        .frame(width: itemWidth)
                        .padding(.top, 32)
                        .opacity((identityService.tuLoginStatus == .mfaUnconfirmed ||
                                  identityService.tuLoginStatus == .ssoUnconfirmed) ? 1 : 0)
                        
                        VStack {
                            Text("TUCaN Login Failed")
                                .multilineTextAlignment(.center)
                                .foregroundStyle(.secondary)
                                .padding(.horizontal)
                            
                            Button("Retry") {
                                Task {
                                    await identityService.tuRefreshLogin()
                                }
                            }
                            Button("Login again") {
                                let oathHelper = OAuthViewModel()
                                oathHelper.startLogin(exchangeCodeForTokens: { code, verifier, nonce in
                                    Task {
                                        await identityService.tucanLogin(code: code, codeVerifier: verifier, nonce: nonce)
                                    }
                                })
                            }
                        }
                        .frame(width: itemWidth)
                        .padding(.top, 32)
                        .opacity((identityService.tuLoginStatus == .ssoInvalid) ? 1 : 0)
                        
                        // This is needed for when a MFA Login fails logging in to tucan after padding SSO
                        // Can happen seemingly at random and is fixed by logging into the web service
                        VStack {
                            Button("TUCaN Login Failed.\nClick for more info") {
                                showTucanErrorSheet = true
                            }
                            .multilineTextAlignment(.center)
                            .foregroundStyle(.secondary)
                            .padding(.horizontal)
                        }
                        .frame(width: itemWidth)
                        .padding(.top, 32)
                        .sheet(isPresented: $showTucanErrorSheet) {
                            TucanLoginFailSheet(isPresented: $showTucanErrorSheet)
                        }
                        .opacity((identityService.tuLoginStatus == .mfaTucanError) ? 1 : 0)
                        
                        if (itemsCount > 1) {
                            VStack(alignment: .leading) {
                                HeaderText("Up Next")
                                    .padding(.leading, -16)
                                HStack {
                                    ForEach(timeSlots, id: \.id) { timeSlot in
                                        CurrentEventsItem(timeSlot: timeSlot, itemWidth: itemWidth)
                                            .transition(.asymmetric(insertion: .move(edge: .trailing).combined(with: .opacity), removal: .scale))
                                    }
                                }
                            }
                            .transition(.move(edge: .trailing))
                        }
                    }
                    
                    Spacer()
                        .frame(width: 16)
                }
                .animation(.default, value: tucanService.timeSlots)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                GeometryReader { geometry in
                    Color.clear
                        .onChange(of: geometry.size.width) { newWidth in
                            updateItemWidth(newWidth)
                        }
                        .onAppear {
                            updateItemWidth(geometry.size.width)
                        }
                }
            )
            .onAppear {
                updateTimeSlots()
            }
            .onChange(of: tucanService.timeSlots) { _ in
                updateTimeSlots()
            }
            .onChange(of: context.date) { date in
                updateTimeSlots()
            }
        }
    }
    
    private func updateItemWidth(_ width: CGFloat) {
        let screenWidth = width - 32 + 8
        let numberOfItemsThatFit = Int(screenWidth / CGFloat(minItemWidth + 8))
        itemWidth = screenWidth / CGFloat(numberOfItemsThatFit) - 8
    }
    
    private func updateTimeSlots() {
        let date = Date()
        let calendar = Calendar.current
        let hour = calendar.component(.hour, from: date)
        let minute = calendar.component(.minute, from: date)
        
        withAnimation {
            timeSlots = tucanService.timeSlots.filter { timeSlot in
                let time = timeSlot.time.split(separator: " - ").last?.split(separator: ":")
                guard let timeSlotMinuteStr = time?.last else { return true }
                guard let timeSlotHourStr = time?.first else { return true }
                
                guard let timeSlotMinute = Int(timeSlotMinuteStr) else { return true }
                guard let timeSlotHour = Int(timeSlotHourStr) else { return true }
                
                return timeSlotHour > hour || (timeSlotHour == hour && timeSlotMinute > minute)
            }
        }
    }
}

private struct TucanLoginFailSheet: View {
    @Binding var isPresented: Bool
    
    @EnvironmentObject var identityService: IdentityService
    
    var body: some View {
        NavigationView {
            VStack {
                Image(systemSymbol: .exclamationmarkTriangle)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 58, height:  58)
                    .padding(.top, 10)
                    .foregroundStyle(Color.accentColor)
                Text("Couldn't login with TUCaN")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 10)
                    .padding(.bottom)
                
                VStack(alignment: .leading) {
                    Text("First try the retry button.\n\nIf that didn't work try the following:")
                    
                    HStack(alignment: .top) {
                        Text("  1.")
                        Text("Open Safari (not private/incognito mode) and login to TUCaN")
                    }
                    HStack(alignment: .top) {
                        Text("  2.")
                        Text("Log out again")
                    }
                    HStack(alignment: .top) {
                        Text("  3.")
                        Text("Try the retry button again")
                    }
                    
                    Text("\nIf this also didn't work, TUCaN is doing something weird. You can only wait and try again later.")
                }
                .frame(maxWidth: .infinity)
                
                Spacer()
            }
            .padding(.horizontal)
//            .navigationBarItems(leading: Button("", systemSymbol: .xmark) {
//                isPresented = false
//            })
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("", systemSymbol: .xmark) {
                        isPresented = false
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Retry") {
                        Task {
                            isPresented = false
                            _ = await identityService.tuRefreshLogin()
                        }
                    }
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}


private struct CommonAction: View {
    var text: LocalizedStringKey
    var symbol: SFSymbol
    
    var body: some View {
        HStack {
            Image(systemSymbol: symbol)
                .padding(.vertical, 8)
                .padding(.leading)
            Text(text)
                .padding(.vertical)
                .font(.subheadline)
            Spacer()
            Image(systemSymbol: .chevronRight)
                .foregroundStyle(Color(.tertiaryLabel))
                .font(.system(size: 14, weight: .semibold))
                .padding(.trailing)
        }
        .frame(maxWidth: .infinity, maxHeight: 50)
    }
}

private struct CommonActionWithNavigation<Content: View>: View {
    var text: LocalizedStringKey
    var symbol: SFSymbol
    var view: () -> Content
    
    var body: some View {
        NavigationLinkButton {
            CommonAction(text: text, symbol: symbol)
        } destination: {
            view()
        }
        .background(Color(.secondarySystemGroupedBackground))
        .rounded()
    }
}


private struct FrequentlyUsedView: View {
    @EnvironmentObject private var identityService: IdentityService
    
    var body: some View {
        VStack {
            HStack {
                if #available(iOS 18.0, *) {
                    CommonActionWithNavigation(text: "Courses", symbol: .insetFilledRectangleAndPersonFilled, view: {CourseView()})
                } else {
                    CommonActionWithNavigation(text: "Courses", symbol: .rectangleInsetFilledAndPersonFilled, view: {CourseView()})
                }
                CommonActionWithNavigation(text: "Timetable", symbol: .calendar, view: {TimetableView()})
            }
            HStack {
                CommonActionWithNavigation(text: "Exams", symbol: .checkmarkSeal, view: {ExamView()})
                
                if (identityService.tuLoginStatus == .sso ||
                    identityService.tuLoginStatus == .ssoUnconfirmed) &&
                    identityService.totpSecretExists
                {
                    CommonActionWithNavigation(text: "2FA Codes", symbol: .lockRotation, view: {TotpCodeView()})
                } else {
                    Spacer()
                        .frame(maxWidth: .infinity)
                        .padding(.leading, 4)
                }
            }
        }
        .padding(.horizontal)
    }
}


private struct TucanAction: View {
    var item: IconTextPair
    
    var body: some View {
        VStack {
            Image(systemSymbol: item.symbol ?? .globe)
                .imageScale(.large)
                .padding(16)
                .frame(width: 60, height: 60)
                .background(Color.accentColor)
                .rounded(liquidCornerRadius: 12)
                .foregroundStyle(.white)
                .font(.system(.body, weight: .medium))
            Spacer()
                .frame(height: 4)
            Text(item.text)
                .font(.caption)
                .multilineTextAlignment(.center)
        }
    }
}

private struct TucanActionsView: View {
    private var tucanActions: [IconTextPair] {
        var actions: [IconTextPair] = [
            IconTextPair(text: "Personal Data", symbol: .person),
            IconTextPair(text: "Messages", symbol: .bellBadge),
            IconTextPair(text: "Forms", symbol: .textBubble),
            IconTextPair(text: "Application", symbol: .squareTextSquare),
            //IconTextPair(text: "Identitätsmanagement", symbol: .personCropCircle),
            IconTextPair(text: "Help", symbol: .infoCircle)
        ]
        
        // symbols renamed in iOS 18
        if #available(iOS 18, *) {
            actions.insert(contentsOf: [
                IconTextPair(text: "Documents", symbol: .document),
                IconTextPair(text: "Electives", symbol: .arrowTriangleheadBranch)
            ], at: 1)
        } else {
            actions.insert(contentsOf: [
                IconTextPair(text: "Documents", symbol: .doc),
                IconTextPair(text: "Electives", symbol: .arrowTriangleBranch)
            ], at: 1)
        }
        
        return actions
    }
    
    private let columns = Array(
        repeating: GridItem(.flexible(), spacing: 0, alignment: .top),
        count: 4
    )
    
    var body: some View {
        LazyVGrid(columns: columns, spacing: 16) {
            ForEach(tucanActions) { item in
                TucanAction(item: item)
            }
        }
        .padding(.vertical)
        // Temporary while unimplemented
            //.background(Color(.secondarySystemGroupedBackground))     // Uncomment when implemented
            .blur(radius: 2.25)
            .overlay {
                Color(.systemGroupedBackground).opacity(0.5)
            }
            .overlay {
                Text("Coming in a\nfuture update")
                    .font(.title)
                    .fontWeight(.semibold)
                    .multilineTextAlignment(.center)
                    .rotationEffect(.degrees(24))
            }
        .rounded()
        .padding(.horizontal)
    }
    
}


private struct ExternalLinkContent: View {
    var item: (title: String, symbol: SFSymbol, url: URL?)
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemSymbol: item.symbol)
                .frame(width: 40, alignment: .center)
            Text(item.title)
                .font(.subheadline)
            Spacer()
            Image(systemSymbol: .arrowUpForwardSquare)
                .symbolRenderingMode(.monochrome)
                .padding(.trailing)
        }
    }
}

// This is used to make URLs Identifiable
private struct SafariURL: Identifiable {
    let id = UUID()
    let url: URL
}

private struct ExternalLinksView: View {
    var externalLinks: [(title: String, symbol: SFSymbol, url: URL?)] {
        var symbol: SFSymbol {
            if #available(iOS 18, *) {
                .chevronUp2
            } else if #available(iOS 17, *) {
                .arrowshapeUp
            } else {
                .arrowUp
            }
        }
        
        let links: [(String, SFSymbol, URL?)] = [
            (String(localized: "Uni-Sports"), .dumbbell, URL(string: "https://online-anmeldung.usz.tu-darmstadt.de/sportarten/aktueller_zeitraum/index.html")),
            (String(localized: "Deutschlandticket"), .qrcode, URL(string: "https://abo.ride-ticketing.de/app/login?partnerId=4343e8d5f524ae7d70fad06e2488d83a")),
            (String(localized: "Identity Management"), .personCropCircle, URL(string: "https://idm.tu-darmstadt.de")),
            (String(localized: "Tu4You"), symbol, URL(string: "https://tu4you.tu-darmstadt.de/"))
        ]
        
        return links
    }
    
    @State private var currentLink: SafariURL? = nil
    
    var body: some View {
        GroupedList(style: .plain) {
            ForEach(externalLinks.indices, id: \.self ) { index in
                let item = externalLinks[index]
                VStack {
                    Button(action: {
                        if let url = item.url {
                            currentLink = SafariURL(url: url)
                        }
                    }, label: {
                        ExternalLinkContent(item: item)
                            .padding(.leading, -48)
                            .foregroundStyle(Color(.label))
                    })
                }
            }
        }
        .safariView(item: $currentLink) { link in
            SafariView(
                url: link.url,
                configuration: SafariView.Configuration(
                    entersReaderIfAvailable: false,
                    barCollapsingEnabled: true
                )
            )
            .preferredBarAccentColor(.clear)
            .preferredControlAccentColor(.accentColor)
            .dismissButtonStyle(.cancel)
        }
        .padding(.leading, 44)
        .background(Color(.secondarySystemGroupedBackground))
        .rounded()
        .padding(.horizontal)
    }
}

private struct LoginLockedCardView: View {
    @Binding var isSheetPresented: Bool
    @Binding var skipBasicLogin: Bool
    
    var body: some View {
        VStack {
            Image(systemSymbol: .lockFill)
                .resizable()
                .scaledToFit()
                .frame(width: 44, height: 44)
                .padding(.top, 32)
                .padding(.bottom)
             
            Text("This section requires TU login")
                .font(.title2)
                .fontWeight(.semibold)
                .padding(.bottom)
            
            TULoginController(isSheetPresented: $isSheetPresented, skipBasicLogin: $skipBasicLogin) { loginAction, _ in
                BigBlueButton("Login with TU ID") {
                    loginAction()
                }
            }
            .padding(.bottom)
        }
        .padding(.horizontal)
        .background(Color(.secondarySystemGroupedBackground))
        .rounded()
    }
}


#Preview("Login Locked") {
    VStack {
        Spacer()
        LoginLockedCardView(isSheetPresented: Binding(get: {true}, set: {_ in}), skipBasicLogin: Binding(get: {false}, set: {_ in}))
            .padding(.horizontal)
        Spacer()
    }
    .frame(maxWidth: .infinity)
    .background(Color(.systemGroupedBackground))
}

#Preview("Study View") {
    StudyView()
        .environmentObject(TucanService())
        .environmentObject(IdentityService(previewMode: true))
}
