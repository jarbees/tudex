//  Created on 03.05.25.

import SwiftUI
import Shared
import SFSafeSymbols

struct FoodView: View {
    @Namespace private var animation
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var mensaService: MensaService
    
    @State private var isTeaserFullscreen: Bool = false     // true only when no animation (and Teaser Fullscreen)
    @State private var isCardFullscreen: Bool = false       // true only when no animation (and Card Fullscreen)
    
    // iOS 16 needs these as CGFloat to get animationCompletionHandlers;
    // 1: fully fullscreen, 0: fully non-fullscreen - Value animates between the two
    @State private var cardFullscreenProgress: CGFloat = 0
    @State private var teaserFullscreenProgress: CGFloat = 0
    
    // Animation properties
    @State private var animatedCardID: String?
    @State private var animatedTeaserIndex: Int?
    @State private var selectedTeaserTab: Int = 0
    @State private var dragAmount: CGFloat = 0
    
    @State private var selectedDay: Int = 0
    @State private var weekdayUnixTime: [Int] = Array(repeating: 0, count: 5)
    
    private let shortCanteenName: [Int : String] = [
        1: "Stadtmitte",
        2: "Lichtwiese",
        3: "Schöfferstraße",
        4: "Dieburg",
        5: "Haardtring",
        7: "Schöffers"
    ]
    
    // Used to find out cornerRadius of devices
    @State private var fullscreenCornerRadius: CGFloat = 0
    
    // filteredMenuItems includes filtering by allergies/preferences, todayMenuItems are all for that day
    @State private var todayMenuItems: [MenuItem] = []
    @State private var filteredMenuItems: [MenuItem] = []
    @State private var filteredItemCount: Int = 0
    @State private var menuItemsUpdating: Bool = false
    
    @AppStorage("canteenDisabledFoodTypes") private var disabledFoodTypes: String = ""
    @AppStorage("canteenEnabledAllergies") private var enabledAllergies: String = ""
    
    var body: some View {
        ZStack(alignment: .topTrailing) {
            NavigationStack {
                ScrollView {
                    ZStack(alignment: .topLeading) {
                        VStack {
                            TabView(selection: $selectedTeaserTab) {
                                ForEach(Array(mensaService.teasers.enumerated()), id: \.element.id) { index, teaser in
                                    TeaserHero(teaser: teaser, index: index, animation: animation)
                                        .tag(index)
                                        .padding(.horizontal)
                                        .padding(.bottom, 32)
                                        .onTapGesture {
                                            animatedTeaserIndex = index
                                            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                                                teaserFullscreenProgress = 1
                                            }
                                        }
                                        .opacity((animatedTeaserIndex == index &&
                                                  selectedTeaserTab == index) ||
                                                 teaserFullscreenProgress == 1 ? 0 : 1)
                                    // removes opacity animations
                                        .transaction { transaction in
                                            transaction.animation = nil
                                        }
                                    
                                }
                            }
                            .tabViewStyle(.page)
                            .frame(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.width * 2/3 + 32)
                            
                            DayPicker(selectedDay: $selectedDay)
                                .zIndex(1)  // Fixes LazyVGrid stealing tap gesture when not overlapping
                            
                            LazyVGrid(columns: [GridItem(.flexible(), alignment: .top), GridItem(.flexible(), alignment: .top)], spacing: 8) {
                                ForEach(filteredMenuItems, id: \.id) { menuItem in
                                    CardContent(menuItem: menuItem)
                                        .opacity(animatedCardID == menuItem.id ? 0 : 1)
                                        .anchorPreference(key: CardFramePreferenceKey.self, value: .bounds) { anchor in
                                            [menuItem.id: anchor] // Use index or teaser.id as the key
                                        }
                                        .onTapGesture {
                                            animatedCardID = menuItem.id
                                            dragAmount = 0
                                            
                                            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                                                cardFullscreenProgress = 1
                                            }
                                        }
                                }
                            }
                            .scaleEffect(menuItemsUpdating ? 0.999 : 1)
                            .padding(.horizontal)
                            .padding(.bottom)
                            
                            if filteredItemCount != 0 {
                                Text(.hiddenMenuItemsMessage(filteredItemCount))
                                    .multilineTextAlignment(.center)
                                    .fixedSize(horizontal: false, vertical: true)
                                    .padding(.bottom, 8)
                                Button("Show all") {
                                    filteredMenuItems = todayMenuItems
                                    filteredItemCount = 0
                                }
                            }
                        }
                        .padding(.bottom)
                        
                        
                        // Placeholder to determine position of canteen selector next to large NavigationTitle
                        // The canteen selector must be outside the NavigationStack to be clickable in front of NavigationTitle
                        Rectangle()
                            .fill(.clear)
                            .matchedGeometryEffect(id: "FoodViewDropdown", in: animation, properties: .position, anchor: .topLeading)
                            .padding(.top, -48)
                    }
                }
                .toolbarTitleMenu {
                    MensaPicker(selectedCanteen: mensaService.selectedCanteen)
                }
                .navigationTitleAdaptive(shortCanteenName[mensaService.selectedCanteen] ?? String(localized: "Select a canteen")) {
                    BetterMenu {
                        MensaPicker(selectedCanteen: mensaService.selectedCanteen)
                    } label: {
                        HStack(spacing: 0) {
                            Text(shortCanteenName[mensaService.selectedCanteen] ?? "Select a canteen")
                                .font(.largeTitle)
                                .bold()
                            Image(systemSymbol: .chevronDown)
                                .font(.callout)
                                .fontWeight(.semibold)
                                .foregroundStyle(Color(.secondaryLabel))
                                .padding(8)
                                .background {
                                    Circle()
                                        .fill(Color(.systemFill))
                                }
                                .padding(.leading, 4)
                                .padding(.top, 6)
                        }
                    }
                    .foregroundStyle(Color(.label))
                    .modify { view in
                        if #available(iOS 26.0, *) {
                            view.padding(.top, -12)
                                .padding(.bottom, 8)
                        } else {
                            view.padding(.leading)
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                }
                .toolbar {
                    ToolbarItem {
                        NavigationLinkButton {
                            Group {
                                if mensaService.cardBalance == "0.00" {
                                    if #available(iOS 26.0, *) {
                                        Image(systemSymbol: .ellipsis)
                                    } else {
                                        Image(systemSymbol: .ellipsisCircle)
                                    }
                                } else {
                                    Text("\(mensaService.cardBalance)€")
                                        .bold()
                                }
                            }
                        } destination: {
                            FoodExtras()
                        }
                        .navButtonStyle(.plain)
                    }
                }
                .background(Color(.systemGroupedBackground))
            }
            // Completion handlers
            .onAnimationCompleted(for: cardFullscreenProgress) {
                if cardFullscreenProgress == 0 {
                    animatedCardID = nil
                } else {
                    isCardFullscreen = true
                }
            }
            .onAnimationCompleted(for: teaserFullscreenProgress) {
                if teaserFullscreenProgress == 0 {
                    animatedTeaserIndex = nil
                } else {
                    isTeaserFullscreen = true
                }
            }
            
            // Blur layer
            .overlay {
                VisualEffectView(uiVisualEffect: UIBlurEffect(style: (colorScheme == .dark) ? .dark : .light))
                    .ignoresSafeArea()
                    .opacity(cardFullscreenProgress == 1 || teaserFullscreenProgress == 1 ? 1 : 0)
            }
            
            // Teaser fullscreen View
            .overlay {
                TeaserDetailView(animation: animation, teaserFullscreenProgress: $teaserFullscreenProgress, isTeaserFullscreen: $isTeaserFullscreen, selectedTeaserTab: $selectedTeaserTab, animatedTeaserIndex: $animatedTeaserIndex)
                    // This is required for reasons beyond me at the moment of writing
                    .opacity(teaserFullscreenProgress == 1 ? 1 : 0)
            }
            
            // Teaser transition View
            .overlay {
                // This placeholder is the animation since TabView sadly crops the view to its frame
                if let teaser = mensaService.teasers[safe: selectedTeaserTab],
                   let image = teaser.image.platformImage?.uiImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .rounded()
                        .overlay(alignment: .bottomLeading) {
                            Text(teaser.title)
                                .font(.title)
                                .bold()
                                .foregroundStyle(.white)
                                .shadow(radius: 20)
                                .padding(.bottom, 8)
                                .padding(.leading, 12)
                                .opacity(teaserFullscreenProgress == 1 ? 0 : 1)
                        }
                        .matchedGeometryEffect(id: "\(teaserFullscreenProgress == 1 ? "TeaserFullscreen" : "TeaserMinimized") \(animatedTeaserIndex ?? -1)", in: animation, isSource: false)
                        .padding(.bottom)
                        .allowsHitTesting(false)
                        .opacity(selectedTeaserTab != animatedTeaserIndex ||
                                 animatedTeaserIndex == nil ||
                                 isTeaserFullscreen ? 0.001 : 1)
                }
            }
            
            // Food fullscreen + transition helper
            .overlayPreferenceValue(CardFramePreferenceKey.self) { preferences in
                var animatedCard: MenuItem? {
                    guard let animatedCardID else { return nil }
                    return mensaService.menuItems.first { $0.id == animatedCardID }
                }
                
                GeometryReader { proxy in
                    if let animatedCard = animatedCard {
                        if (cardFullscreenProgress == 0) {
                            if let anchor = preferences[animatedCard.id] {
                                let frame = proxy[anchor]
                                CardContent(menuItem: animatedCard, animation: animation, fullscreenCornerRadius: fullscreenCornerRadius, dragAmount: dragAmount)
                                    .frame(width: frame.width, height: frame.height)
                                    .position(x: frame.midX, y: frame.midY)
                                    .transition(.offset())
                            }
                        } else {
                            FoodDetailView(menuItem: animatedCard, animation: animation, isCardFullscreen: $isCardFullscreen, cardFullscreenProgress: $cardFullscreenProgress, dragAmount: $dragAmount, fullscreenCornerRadius: $fullscreenCornerRadius)
                                .transition(.offset())
                        }
                    }
                }
            }
        }
        .onAppear {
            setWeekdays()
            processMenuItems(menuItems: mensaService.menuItems)
        }
        .onChange(of: mensaService.menuItems) { newItems in
            processMenuItems(menuItems: newItems)
        }
        .onChange(of: selectedDay) { newDay in
            withAnimation(.easeInOut(duration: 0.15)) {
                menuItemsUpdating = true
            }
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                processMenuItems(menuItems: mensaService.menuItems)
            }
            
            withAnimation(.easeInOut(duration: 0.15).delay(0.15)) {
                menuItemsUpdating = false
            }
        }
        
        .coordinateSpace(name: "ZStack")
        .toolbar((cardFullscreenProgress == 1 || teaserFullscreenProgress == 1) ? .hidden : .visible, for: .tabBar)
        // I dislike this visually, but fixes ScrollView making tabBar transparent;
        // doesn't matter for iOS 26, since the tabBar is always visible there
        .toolbarBackground(.visible, for: .tabBar)
        .statusBarHidden(cardFullscreenProgress == 1 || teaserFullscreenProgress == 1)
    }
    
    private func processMenuItems(menuItems: [MenuItem]) {
        let selectedEpochTime = weekdayUnixTime[selectedDay]
        todayMenuItems = menuItems.filter { $0.date == selectedEpochTime }
        
        let enabledAllergySet = Set(enabledAllergies.split(separator: ",").map(String.init))

        filteredMenuItems = todayMenuItems.filter { menuItem in
            if disabledFoodTypes.contains(menuItem.dish.type.displayName) {
                return false
            }
            
            let dishAllergies = FoodView.resolvedAllergics(for: menuItem)
            
            if dishAllergies.contains("A") && enabledAllergySet.contains(where: { $0.hasPrefix("A") }) {
                return false
            }
            if dishAllergies.contains("H") && enabledAllergySet.contains(where: { $0.hasPrefix("H") }) {
                return false
            }
                
            return enabledAllergySet.isDisjoint(with: dishAllergies)
        }
        
        filteredItemCount = todayMenuItems.count - filteredMenuItems.count
    }
    
    private func setWeekdays() {
        // INFO: In swift Saturday = 7 and Sunday = 1
        
        // Determine which day the picker starts at
        var calendar = Calendar.current
        let now = Date()
        
        let weekday = Calendar.current.component(.weekday, from: now)
        
        // If Saturday or Sunday, let picker show Monday of next week
        if weekday == 7 || weekday == 1 {
            selectedDay = 0
        } else {
            selectedDay = weekday - 2
        }
        
        // Determine Unix Epoch times for the current week (or next week if Sat/Sun)
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        
        var components = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: now)
        
        // Add 1 week to current date components to move to next week if Sat/Sun
        if weekday == 7 || weekday == 1 {
            if let nextWeekDate = calendar.date(byAdding: .weekOfYear, value: 1, to: now) {
                components = calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: nextWeekDate)
            }
        }
        
        for weekdayIndex in 2...6 {
            components.weekday = weekdayIndex
            
            // Get the date for Monday at 12:00 AM
            if let weekday = calendar.date(from: components) {
                let midnight = calendar.startOfDay(for: weekday)
                weekdayUnixTime[weekdayIndex - 2] = Int(midnight.timeIntervalSince1970)
            }
        }
    }
}


extension FoodView {
    static func resolvedAllergics(for menuItem: MenuItem) -> [String] {
        guard let specificAllergics = menuItem.dish.specificAllergics else {
            return menuItem.dish.allergics
        }
        
        return menuItem.dish.allergics.flatMap { allergic in
            guard allergic == "A" || allergic == "H" else {
                return [allergic]
            }
            
            let specifics = specificAllergics.filter {
                $0.hasPrefix(allergic)
            }
            
            return specifics.isEmpty ? [allergic] : specifics
        }
    }
    
    static func formatDoubleToCurrency(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.locale = Locale(identifier: "de_DE") // Use German locale for comma as decimal separator
        formatter.currencySymbol = "€"
        
        return formatter.string(from: NSNumber(value: value)) ?? ""
    }
    
    static let maxDragStartX: CGFloat = 20
    static let visualDragStartDistance: CGFloat = 20
    static let minDismissDistance: CGFloat = 50
    static let maxDragDistance: CGFloat = 130
    
    enum MatchedID: Hashable {
        case image
        case price
        case labels
        case content
        case background
        case mask
    }
}

private extension View {
    @ViewBuilder
    func foodMatchedGeometry(
        id: FoodView.MatchedID,
        in animation: Namespace.ID?,
        properties: MatchedGeometryProperties = .frame,
        anchor: UnitPoint = .center,
        isSource: Bool = true
    ) -> some View {
        if let animation {
            matchedGeometryEffect(id: id, in: animation, properties: properties, anchor: anchor, isSource: isSource)
        } else {
            self
        }
    }
}

private struct CardFramePreferenceKey: PreferenceKey {
    static var defaultValue: [String: Anchor<CGRect>] = [:]
    
    static func reduce(
        value: inout [String: Anchor<CGRect>],
        nextValue: () -> [String: Anchor<CGRect>]
    ) {
        value.merge(nextValue(), uniquingKeysWith: { $1 })
    }
}

private struct TeaserHero: View {
    let teaser: Teaser
    let index: Int
    let animation: Namespace.ID
    
    init(teaser: Teaser, index: Int, animation: Namespace.ID) {
        self.teaser = teaser
        self.index = index
        self.animation = animation
    }
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            Group {
                if let image = teaser.image.platformImage?.uiImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                } else {
                    Text("Image not loaded.")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .aspectRatio(3/2, contentMode: .fit)
                        .background(Color(.secondarySystemGroupedBackground))
                }
            }
            .rounded()
            
            Text(teaser.title)
                .font(.title)
                .bold()
                .foregroundStyle(.white)
                .shadow(radius: 20)
                .padding(.bottom, 8)
                .padding(.leading, 12)
        }
        .matchedGeometryEffect(id: "TeaserMinimized \(index)", in: animation)
    }
}

private struct DayPicker: View {
    let daysOfWeek = ["Mon", "Tue", "Wed", "Thu", "Fri"]
    @Binding var selectedDay: Int
    
    var body: some View {
        Picker("Select a day", selection: $selectedDay) {
            ForEach(daysOfWeek.indices, id: \.self) { index in
                Text(daysOfWeek[index]).tag(index)
            }
        }
        .pickerStyle(SegmentedPickerStyle())
        .padding(.horizontal)
        .padding(.bottom)
    }
}

private struct MensaPicker: View {
    @EnvironmentObject private var mensaService: MensaService
    
    @State private var selectedCanteen: Int
    
    init(selectedCanteen: Int) {
        self.selectedCanteen = selectedCanteen
    }
    
    
    let canteens: [(index: Int, title: String)] = [
        (1, "Mensa Stadtmitte"),
        (2, "Mensa Lichtwiese"),
        (3, "Mensa Schöfferstraße"),
        (4, "Mensa Dieburg"),
        (5, "Bistro Haardtring"),
        (7, "Campusrestaurant Schöffers")
    ]
    
    var body: some View {
        Picker("", selection: $selectedCanteen) {
            ForEach(canteens, id: \.index) { canteen in
                Text(canteen.title).tag(canteen.index)
            }
        }
        .padding(.horizontal)
        .padding(.bottom)
        .onChange(of: selectedCanteen) { newCanteen in
            Task {
                await mensaService.updateSelectedCanteen(to: newCanteen)
            }
        }
    }
}

private struct CardContent: View {
    let menuItem: MenuItem
    let isTransitionCopy: Bool
    let animation: Namespace.ID?
    let fullscreenCornerRadius: CGFloat
    let dragAmount: CGFloat
    
    @State private var dragTransitionProgress: CGFloat = 0
    @State private var animCornerRadius: CGFloat = 0
    @State private var hasAppeared: Bool = false    // animates from false to true when card appears
    
    @ScaledMetric private var foodImageSize: CGFloat = 24
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var mensaService: MensaService
    
    private var cardCornerRadius: CGFloat {
        if #available(iOS 26.0, *) {
            18
        } else {
            8
        }
    }
    
    private var transitionScale: CGFloat {
        guard isTransitionCopy else { return 1 }
        return 1 - dragTransitionProgress * 0.20
    }
    
    private var transitionMaskScaleY: CGFloat {
        guard isTransitionCopy else { return 1 }
        return 1 - dragTransitionProgress * 0.30
    }
    
    init(menuItem: MenuItem, animation: Namespace.ID, fullscreenCornerRadius: CGFloat, dragAmount: CGFloat) {
        self.menuItem = menuItem
        self.isTransitionCopy = true
        self.animation = animation
        self.fullscreenCornerRadius = fullscreenCornerRadius
        self.dragAmount = dragAmount
    }
    
    init(menuItem: MenuItem) {
        self.menuItem = menuItem
        self.isTransitionCopy = false
        self.animation = nil
        self.fullscreenCornerRadius = 0
        self.dragAmount = 0
    }
    
    var body: some View {
        VStack(spacing: 0) {
            VStack {
                var optImage: UIImage? {
                    if let image = menuItem.dish.image {
                        if image.__typename == "NoImageExists" {
                            return UIImage(named: "UnknownFood")!
                        } else {
                            return image.platformImage?.uiImage
                        }
                    } else {
                        return nil
                    }
                }
                
                ZStack(alignment: .topLeading) {
                    // Image
                    Group {
                        if let image = optImage {
                            FoodImage(image)
                        } else {
                            ZStack {
                                ProgressView()
                                
                                Spacer()
                                    .frame(maxWidth: .infinity)
                                    .aspectRatio(16/9, contentMode: .fit)
                            }
                        }
                    }
                    .foodMatchedGeometry(id: .image, in: animation)
                    
                    // PriceTag
                    HStack {
                        HStack(spacing: 4) {
                            let foodType = menuItem.dish.type
                            if foodType != .unknown {
                                FoodTypeIcon(type: foodType)
                                    .frame(width: foodImageSize, height: foodImageSize)
                                    .padding(.vertical, -2)
                            }
                            
                            Text(FoodView.formatDoubleToCurrency(menuItem.dish.price))
                                .font(.subheadline)
                                .bold()
                        }
                        .padding(4)
                        .padding(.bottom, 4)
                        .padding(.trailing, 4)
                        .padding(.leading, 2)
                        .fixedSize(horizontal: true, vertical: false)
                        .background {
                            UnevenRoundedRectangle(bottomTrailingRadius: cardCornerRadius + 2)
                                .fill(Color(.secondarySystemGroupedBackground))
                        }
                        .foodMatchedGeometry(id: .price, in: animation)
                    }
                    VStack {
                        Spacer()
                        
                        HStack {
                            if menuItem.dish.bio {
                                Image("FoodBio")
                                    .resizable()
                                    .scaledToFit()
                                    .frame(height: foodImageSize + 4)
                                    .padding(.vertical, -2)
                            }
                            if menuItem.dish.mensa_vital {
                                if let vitalLogo = mensaService.vitalLogo {
                                    SVGView(source: vitalLogo, alignment: .leading)
                                        .frame(height: foodImageSize)
                                        .shadow(color: .white, radius: 16)
                                        .shadow(color: .white, radius: 24)
                                        .shadow(color: .white, radius: 24)
                                        .modify { view in
                                            if colorScheme == .light {
                                                view.shadow(color: .white, radius: 12)
                                            }
                                        }
                                } else {
                                    Text("MensaVital")
                                }
                            }
                        }
                        .foodMatchedGeometry(id: .labels, in: animation)
                        .padding(.leading, 4)
                        .padding(.bottom, 4)
                    }
                }
                
                // Content
                VStack {
                    RatingStarsView(rating: .constant(menuItem.dish.rating))
                        .font(.title3)
                        .padding(.bottom, 4)
                    
                    Text(menuItem.dish.name)
                        .lineLimit(3)
                        .font(.subheadline)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom, 4)
                        .padding(.horizontal, 4)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .foodMatchedGeometry(id: .content, in: animation, properties: .position, anchor: .center)
                .opacity(hasAppeared ? 1 : 0)
                
                // Keep Text size unaffected
                .scaleEffect(1 / transitionScale)
            }
            
            Spacer()    // this makes two cards next to each other equal in height
        }
        .scaleEffect(transitionScale)
        .compositingGroup()     // Prevents layers artifacting/shimmering through at corners
        
        .background(
            Rectangle()
                .fill(Color(.secondarySystemGroupedBackground))
                .foodMatchedGeometry(id: .background, in: animation)
        )
        .mask {
            Rectangle()
                .cornerRadius(animCornerRadius)
                .scaleEffect(x: transitionScale, y: transitionMaskScaleY)
                .foodMatchedGeometry(id: .mask, in: animation)
                .ignoresSafeArea()
        }
        .padding(4)
        .background(
            Rectangle()
                .fill(Color(.secondarySystemGroupedBackground))
                .rounded(liquidCornerRadius: 20)
                .scaleEffect(x: (hasAppeared || !isTransitionCopy) ? 1 : 0.8,
                             y: (hasAppeared || !isTransitionCopy) ? 1 : 0.8)
        )
        
        .onAppear {
            if (isTransitionCopy) {
                let dragProgress = dragAmount / FoodView.maxDragDistance
                animCornerRadius = fullscreenCornerRadius + (cardCornerRadius - fullscreenCornerRadius) * dragProgress
                
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    animCornerRadius = cardCornerRadius
                }
                
                dragTransitionProgress = dragProgress
                
                withAnimation {
                    dragTransitionProgress = 0
                    hasAppeared = true
                }
            } else {
                animCornerRadius = cardCornerRadius
                hasAppeared = true
            }
        }
    }
}

#Preview {
    FoodView()
        .environmentObject(MensaService(previewMode: true))
}
