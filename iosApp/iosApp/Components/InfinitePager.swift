//  Created on 23.11.25.

import SwiftUI

/// Here you may feast upon another UIKit component which I cannot implement properly in SwiftUI.
///
/// I have tried many times (see the preserved experiments below), primarily using `TabView` and paged `ScrollView`.
/// Both eventually fail when trying to make their finite content infinite.
///
/// `TabView` does not provide the control necessary to lazily create and reuse a small number of changing pages.
/// Loading a large range of pages is very laggy, while (re)using a fixed set of pages interferes with native paging gestures
///
/// A paged `ScrollView` is more flexible, but inserting pages at the beginning changes the scroll content underneath the current position.
/// Its selection also cannot be redirected reliably midway through an active swipe.
///
/// Fully custom implementations using offsets, `ZStack`, `DragGesture`, etc. can avoid those limitations,
/// but no longer reproduce native paging behavior, in other words momentum and interruption usually feel off.
///
/// `InfinitePager` therefore uses `UIPageViewController` for the actual infinitely paged view.
///
/// The selected page shall be the page occupying the majority of the pager, matching the behavior in `TabView`.
/// Deriving this from `UIPageViewController` using transition callbacks or scroll state is unreliable.
/// Annoyingly, `UIPageViewController`'s current controller is not necessarily the page currently visible.
///
/// Instead, the positions of the hosted page views are continuously measured relative to the pager itself.
/// This determines the physically visible page independently of `UIPageViewController`'s own controller state.
///
/// "Fun fact": after a programmatic page animation, `UIPageViewController` briefly places all involved controllers at the center (observed on iOS 26.2).
/// This is why exactly centered page positions are deliberately ignored below.
///
/// UIKit also handles external changes to `selection` as well as programmatic page transitions.
/// While such a transition is active, the physically visible page continues to be observed but is not reported back to `selection`,
/// as doing so would create a feedback loop and interrupt the requested navigation.
///
/// `UIPageViewController` can abandon a programmatic transition when its underlying `UIScrollView` is touched without reliably reporting that cancellation. The display link therefore also observes `UIScrollView.isTracking` so that user interaction can immediately cancel programmatic ownership and report the actually visible page.



struct InfinitePager<Content: View>: View {
    private let externalSelection: Binding<Int>?
    @State private var internalSelection = 0

    private let content: (Int) -> Content

    private var selection: Binding<Int> {
        externalSelection ?? $internalSelection
    }

    init(selection: Binding<Int>, @ViewBuilder content: @escaping (Int) -> Content) {
        self.externalSelection = selection
        self.content = content
    }

    init(@ViewBuilder content: @escaping (Int) -> Content) {
        self.externalSelection = nil
        self.content = content
    }

    var body: some View {
        UIKitInfinitePager(
            selection: selection.wrappedValue,
            onSelectionChanged: { newSelection in
                guard selection.wrappedValue != newSelection else { return }
                selection.wrappedValue = newSelection
            },
            content: content
        )
    }
}


// UIKit pager

private struct UIKitInfinitePager<Content: View>:UIViewControllerRepresentable {
    let selection: Int
    let onSelectionChanged: (Int) -> Void
    let content: (Int) -> Content
    
    func makeCoordinator() -> Coordinator {
        Coordinator(
            content: content,
            onSelectionChanged: onSelectionChanged
        )
    }

    func makeUIViewController(context: Context) -> PagerViewController {
        PagerViewController(initialIndex: selection, coordinator: context.coordinator)
    }

    func updateUIViewController(_ viewController: PagerViewController, context: Context) {
        context.coordinator.content = content
        context.coordinator.onSelectionChanged = onSelectionChanged

        viewController.update(selection: selection)
    }

    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiViewController: PagerViewController,
        context: Context
    ) -> CGSize? {
        uiViewController.sizeThatFits(proposal)
    }
}


// Coordinator

private extension UIKitInfinitePager {
    @MainActor
    final class Coordinator {
        var content: (Int) -> Content
        var onSelectionChanged: (Int) -> Void

        init(
            content: @escaping (Int) -> Content,
            onSelectionChanged: @escaping (Int) -> Void
        ) {
            self.content = content
            self.onSelectionChanged = onSelectionChanged
        }

        func makePage(index: Int) -> Page {
            Page(index: index, content: content(index))
        }
    }
}


// Page

private extension UIKitInfinitePager {
    @MainActor
    final class Page: UIHostingController<Content> {
        let index: Int

        init(
            index: Int,
            content: Content
        ) {
            self.index = index

            super.init(rootView: content)

            view.backgroundColor = .clear
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError()
        }
    }
}


// Pager controller

private extension UIKitInfinitePager {
    @MainActor
    final class PagerViewController: UIPageViewController, UIPageViewControllerDataSource {
        private let coordinator: Coordinator

        /// Page currently occupying the majority of the pager.
        private var currentlyShown: Int

        /// Destination of the programmatic transition currently executing.
        private var animationTarget: Int?
        
        /// Identifies the newest programmatic transition. An older completion must not end ownership of a newer request.
        private var animationGeneration = 0
        
        private weak var pagingScrollView: UIScrollView?
        private var displayLink: CADisplayLink?

        
        init(initialIndex: Int, coordinator: Coordinator) {
            self.coordinator = coordinator
            self.currentlyShown = initialIndex

            super.init(transitionStyle: .scroll, navigationOrientation: .horizontal)

            dataSource = self

            let initialPage = coordinator.makePage(index: initialIndex)

            setViewControllers([initialPage], direction: .forward, animated: false)
        }
        
        @available(*, unavailable)
        required init?(coder: NSCoder) { fatalError() }
        
        deinit { displayLink?.invalidate() }
        
        
        override func viewDidLoad() {
            super.viewDidLoad()

            pagingScrollView = view.subviews.compactMap { $0 as? UIScrollView }.first
            
            startMonitoring()
        }
        
        // Infinite pages

        func pageViewController(
            _ pageViewController: UIPageViewController,
            viewControllerBefore viewController: UIViewController
        ) -> UIViewController? {
            guard let page = viewController as? Page else {
                return nil
            }

            return coordinator.makePage(
                index: page.index - 1
            )
        }

        func pageViewController(
            _ pageViewController: UIPageViewController,
            viewControllerAfter viewController: UIViewController
        ) -> UIViewController? {
            guard let page = viewController as? Page else {
                return nil
            }

            return coordinator.makePage(
                index: page.index + 1
            )
        }
        
        
        // SwiftUI updates
        
        func update(selection: Int) {
            refreshPageContent()
            
            guard selection != currentlyShown else { return }

            // The requested transition is already executing.
            guard animationTarget != selection else { return }

            navigate(to: selection)
        }

        private func refreshPageContent() {
            for case let page as Page in children {
                page.rootView = coordinator.content(page.index)
            }
        }
        
        
        // Interaction monitoring (for programmatic transition cancel)
        
        private func startMonitoring() {
            guard displayLink == nil else {
                return
            }

            let monitor = CADisplayLink(
                target: self,
                selector: #selector(updatePagerState)
            )

            monitor.add(to: .main, forMode: .common)

            displayLink = monitor
        }
        
        @objc
        private func updatePagerState() {
            updateCurrentlyShown()
            guard animationTarget != nil else { return }
            guard pagingScrollView?.isTracking == true else { return }
            interruptProgrammaticNavigation()
        }
        
        private func updateCurrentlyShown() {
            let pagerMidX = view.bounds.midX
            
            for case let page as Page in children {
                guard page.isViewLoaded else {
                    continue
                }
                
                let frame = page.view.convert(
                    page.view.bounds,
                    to: view
                )
                
                // UIPageViewController briefly centers multiple controllers when
                // settling a programmatic transition.
                guard
                    frame.minX <= pagerMidX,
                    frame.maxX >= pagerMidX,
                    frame.midX != pagerMidX
                else {
                    continue
                }
                
                guard currentlyShown != page.index else {
                    continue
                }
                
                currentlyShown = page.index
                
                guard animationTarget == nil else {
                    continue
                }
                
                reportSelection()
            }
        }

        private func reportSelection() {
            coordinator.onSelectionChanged(
                currentlyShown
            )
        }
        
        
        // Programmatic navigation
        
        private func navigate(to index: Int) {
            guard currentlyShown != index else {
                return
            }
            
            animationGeneration += 1
            let generation = animationGeneration
            
            animationTarget = index
            
            let direction: NavigationDirection = (index > currentlyShown) ? .forward : .reverse
            let targetPage = coordinator.makePage(index: index)
            
            setViewControllers([targetPage], direction: direction, animated: true) { [weak self] _ in
                guard let self else { return }
                guard generation == self.animationGeneration else { return }
                self.completeProgrammaticNavigation()
            }
        }

        
        private func completeProgrammaticNavigation() {
            animationTarget = nil
            reportSelection()
        }

        private func interruptProgrammaticNavigation() {
            animationGeneration += 1
            completeProgrammaticNavigation()
        }

        
        // Sizing

        func sizeThatFits(_ proposal: ProposedViewSize) -> CGSize? {
            guard let page = viewControllers?.first as? Page else {
                return nil
            }
            
            let width = proposal.width ?? view.bounds.width
            let height = proposal.height ?? UIView.layoutFittingCompressedSize.height
            
            return page.sizeThatFits(in: CGSize(width: width, height: height))
        }
    }
}



// MARK: - Previews

@available(iOS 17.0, *)
#Preview("InfinitePager Stress Test") {
    @Previewable @State var index = 0
    @Previewable @State var isSpamming = false

    VStack(spacing: 16) {
        HStack {
            Button("-1") {
                index -= 1
            }

            Spacer()

            VStack {
                Text("Binding")
                    .font(.caption)
                    .foregroundStyle(.secondary)

                Text("\(index)")
                    .font(.title.monospacedDigit())
            }

            Spacer()

            Button("+1") {
                index += 1
            }
        }
        .padding(.horizontal)

        InfinitePager(selection: $index) { page in
            ZStack {
                Color(
                    hue: Double((page % 10 + 10) % 10) / 10,
                    saturation: 0.6,
                    brightness: 0.9
                )

                VStack(spacing: 8) {
                    Text("Page")
                        .font(.headline)

                    Text("\(page)")
                        .font(.system(size: 64, weight: .bold))
                        .monospacedDigit()
                }
                .foregroundStyle(.white)
            }
        }

        HStack {
            Button("−10") {
                index -= 10
            }

            Button("0") {
                index = 0
            }

            Button("+10") {
                index += 10
            }
        }

        Button(isSpamming ? "Stop" : "Spam Back & Forth") {
            isSpamming.toggle()
        }
        .buttonStyle(.borderedProminent)
    }
    .padding(.vertical)
    .task(id: isSpamming) {
        guard isSpamming else {
            return
        }

        var direction = 1

        while !Task.isCancelled && isSpamming {
            index += direction
            direction *= -1

            try? await Task.sleep(
                for: .milliseconds(50)
            )
        }
    }
}

struct InfinitePager_Previews: PreviewProvider {
    static var previews: some View {
        PreviewWrapper()
    }
    
    struct PreviewWrapper: View {
        @State private var index: Int = 0
        @State private var generation: Int = 0  // For testing view updates
        
        var body: some View {
            NavigationStack {
                ScrollView {
                    InfinitePager(selection: $index) { idx in
                        VStack {
                            Spacer()
                            Text("\(index)")
                            Text("Page \(idx)")
                                .font(.largeTitle)
                                .foregroundColor(.white)
                            
                            Spacer()
                            Spacer()
                            
                            GeometryReader { proxy in
                                let frame = proxy.frame(in: .global)

                                VStack {
                                    Text("minX: \(frame.minX)")
                                    Text("midX: \(frame.midX)")
                                    Text("maxX: \(frame.maxX)")
                                }
                                .monospacedDigit()
                                .foregroundColor(.white)
                            }
                        }
                        //.frame(height: 1000)
                        .frame(height: 700)
                        .frame(maxWidth: .infinity)
                        .background(Color(hue: Double((idx % 10)) / 10.0, saturation: 0.6, brightness: 0.9))
                    }
                    .overlay {
                        VStack {
                            Spacer()
                            Spacer()
                            Text("\(index)")
                                .font(.title)
                                .bold()
                                .foregroundStyle(.white)
                            HStack {
                                Button("Decrement") {
                                    index -= 10
                                }
                                .buttonStyle(.borderedProminent)
                                Button("Increment") {
                                    index += 10
                                }
                                .buttonStyle(.borderedProminent)
                            }
                            Button("Update SwiftUI Views") {
                                generation += 1
                            }
                            .buttonStyle(.borderedProminent)
                            Spacer()
                        }
                    }
                }
                .safeAreaInset(edge: .top, spacing: 0) {
                    VStack {
                        Text("Testing")
                        Text("Stuff")
                        Divider()
                    }
                    .background(Color(.secondarySystemGroupedBackground))
                }
            }
        }
    }
}

struct TabViewPager_Previews: PreviewProvider {
    static var previews: some View {
        PreviewWrapper()
    }
    
    struct PreviewWrapper: View {
        @State private var index: Int = 0
        
        var body: some View {
            NavigationStack {
                TabView(selection: $index) {
                    ForEach(1..<100) { index in
                        VStack {
                            Spacer()
                            Text("\(index)")
                            Text("Page \(index)")
                                .font(.largeTitle)
                                .foregroundColor(.white)
                            
                            Spacer()
                            Spacer()
                        }
                        //.frame(height: 1000)
                        .frame(height: 1000)
                        .frame(maxWidth: .infinity)
                        .background(Color(hue: Double((index % 10)) / 10.0, saturation: 0.6, brightness: 0.9))
                    }
                }
                .tabViewStyle(.page)
                .frame(maxHeight: .infinity)
                .overlay {
                    VStack {
                        Spacer()
                        Spacer()
                        Text("\(index)")
                            .font(.title)
                            .bold()
                            .foregroundStyle(.white)
                        HStack {
                            Button("Decrement") {
                                withAnimation {
                                    index -= 1
                                }
                            }
                            .buttonStyle(.borderedProminent)
                            Button("Increment") {
                                withAnimation {
                                    index += 1
                                }
                            }
                            .buttonStyle(.borderedProminent)
                        }
                        Spacer()
                    }
                }
                .safeAreaInset(edge: .top, spacing: 0) {
                    VStack {
                        Text("Testing")
                        Text("Stuff")
                        Divider()
                    }
                    .background(Color(.secondarySystemGroupedBackground))
                }
            }
        }
    }
}

struct InfinitePagerTest_Previews: PreviewProvider {
    static var previews: some View {
        PreviewWrapper()
    }
    
    struct PreviewWrapper: View {
        @State private var index: Int = 0
        
        var body: some View {
            //NavigationStack {
                //ScrollView {
                    InfinitePager(selection: $index) { idx in
                        VStack {
                            //Spacer()
                            Text("\(index)")
                            Text("Page \(idx)")
                                .font(.largeTitle)
                                .foregroundColor(.white)
                            
                            //Spacer()
                            //Spacer()
                        }
                        //.frame(height: 500)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color(hue: Double((idx % 10)) / 10.0, saturation: 0.6, brightness: 0.9))
                    }
                    .overlay {
                        VStack {
                            Spacer()
                            Spacer()
                            Text("\(index)")
                                .font(.title)
                                .bold()
                                .foregroundStyle(.white)
                            /*
                            HStack {
                                Button("Decrement") {
                                    index -= 1
                                }
                                .buttonStyle(.borderedProminent)
                                Button("Increment") {
                                    index += 1
                                }
                                .buttonStyle(.borderedProminent)
                            }
                            */
                            Spacer()
                        }
                    //}
                //}
            }
        }
    }
}

@available(iOS 17.0, *)
#Preview("Linked InfinitePagers") {
    @Previewable @State var selectedDay = 0
    @Previewable @State var displayedWeek = 0

    VStack(spacing: 0) {
        // Header pager
        InfinitePager(selection: $displayedWeek) { week in
            PreviewTimeHeaderPage(
                week: week,
                selectedDay: selectedDay
            )
        }
        .padding(.horizontal)
        .padding(.bottom, 8)

        Divider()

        // Body pager
        InfinitePager(selection: $selectedDay) { day in
            ZStack {
                Color(
                    hue: Double((day % 10 + 10) % 10) / 10,
                    saturation: 0.6,
                    brightness: 0.9
                )

                VStack(spacing: 12) {
                    Text("Page \(day)")
                        .font(.largeTitle)

                    Text("selectedDay \(selectedDay)")
                        .monospacedDigit()
                }
                .foregroundStyle(.white)
            }
        }

        Divider()

        VStack(spacing: 8) {
            Text("Day: \(selectedDay)")
            Text("Week: \(displayedWeek)")

            HStack {
                Button("Day −1") {
                    selectedDay -= 1
                }

                Button("Day +1") {
                    selectedDay += 1
                }
            }
        }
        .monospacedDigit()
        .padding()
    }
    .onChange(of: selectedDay) { day in
        let week = previewWeekOffset(for: day)

        if displayedWeek != week {
            displayedWeek = week
        }
    }
}

private struct PreviewTimeHeaderPage: View {
    let week: Int
    let selectedDay: Int

    private let today = Date()
    private let cal = Calendar.current

    var body: some View {
        let weekday = cal.component(.weekday, from: today)
        let diffToMonday = weekday == 1 ? -6 : 2 - weekday

        let monday = cal.date(
            byAdding: .day,
            value: diffToMonday + 7 * week,
            to: today
        )!

        let days = (0..<7).compactMap {
            cal.date(
                byAdding: .day,
                value: $0,
                to: monday
            )
        }

        HStack {
            let selectedDate = cal.date(
                byAdding: .day,
                value: selectedDay,
                to: today
            )!

            ForEach(
                Array(days.enumerated()),
                id: \.offset
            ) { _, day in
                let isSelected = cal.isDate(
                    day,
                    inSameDayAs: selectedDate
                )

                let isToday = cal.isDateInToday(day)

                VStack(spacing: 4) {
                    Text(
                        day,
                        format: .dateTime.weekday(.abbreviated)
                    )
                    .font(.footnote)
                    .foregroundStyle(
                        Color(.secondaryLabel)
                    )

                    VStack {
                        Circle()
                            .fill(
                                isSelected
                                    ? isToday
                                        ? Color.accentColor
                                        : Color(.label)
                                    : .clear
                            )
                            .scaleEffect(
                                isSelected ? 1 : 0
                            )
                            .padding(4)
                    }
                    .overlay {
                        Text(
                            day,
                            format: .dateTime.day()
                        )
                        .font(.headline)
                        .foregroundStyle(
                            isSelected
                                ? Color(.systemBackground)
                                : isToday
                                    ? Color.accentColor
                                    : Color(.label)
                        )
                    }
                    .animation(
                        .default.speed(2.5),
                        value: isSelected
                    )
                }
            }
        }
    }
}

private func previewWeekOffset(for dayOffset: Int) -> Int {
    let cal = Calendar.current
    let weekday = cal.component(.weekday, from: Date())
    let diffToMonday = weekday == 1 ? -6 : 2 - weekday

    return Int(
        floor(
            Double(dayOffset - diffToMonday) / 7
        )
    )
}


// MARK: - Below are countless failed attempts at doing this in pure SwiftUI

/*
struct ExperimentalInfinitePager<Content: View>: View {
    // To anyone trying to understand this View, firstly, I am sorry for this mess.
    // The Problem, The View must:
    //  (a) work in a ScrollView, (b) with NavigationStack back gesture, (c) feel native, (d) support iOS 16.0
    // This leads to using TabView. However TabView is not infinite and most solutions break at high speeds.
    
    // Here exist two TabViews. Imagine that at any given moment they overlap like so:
    // View 1:  0  1  2  3         or also        0  1  2  3
    // View 2:        0  1  2  4            0  1  2  3
    // They use way too many states to synchonize each other and the Views in the tabs transition using
    // matchedGeometryEffect.
    
    let content: (Int) -> Content
    
    //@State private var index: Int = 0
    
    @State private var indices: [Int] = [-1, 0, 1]
    
    //@State private var contents: [Content]
    
    //@State private var selectedPage: Int = 1
    @State private var firstSelectedPage: Int = 1
    @State private var secondSelectedPage: Int = 2
    
    @State private var firstEndIsMatched: Bool = false
    @State private var secondEndIsMatched: Bool = false
    @State private var firstStartIsMatched: Bool = true
    @State private var secondStartIsMatched: Bool = false
    
    @State private var firstTabViewFocused: Bool = true
    @State private var lockedFirstTabViewFocused: Bool = true
    
    @State private var previousSelectedPage: Int = 1
    
    @State private var isTransition: Bool = false
    @State private var isDragging: Bool = true
    
    @State private var contentWidth: CGFloat = 0
    
    @Namespace private var animation
    
    var body: some View {
        ZStack {
            content(0)
                .hidden()
                .background {
                    GeometryReader { geo in
                        Color.clear
                            .onAppear {
                                contentWidth = geo.size.width
                            }
                            .onChange(of: geo.size.width) { newWidth in
                                contentWidth = newWidth
                            }
                    }
                }
            /*
            TabView(selection: $selectedPage) {
                content(indices[0])
                    .tag(0)
                
                    .background {
                        GeometryReader { geo in
                            Color.clear
                                .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                    if newMinX > 0 {
                                        //selectedPage = 2
                                        if !isDragging {
                                            withAnimation {
                                                selectedPage = 1
                                            }
                                        }
                                    }
                                }
                        }
                    }
                 
                
                content(indices[1])
                    .tag(1)
                
                
                content(indices[2])
                    .tag(2)
                    .background {
                        GeometryReader { geo in
                            Color.clear
                                .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                    if newMinX < 0 {
                                        //selectedPage = 0
                                        if !isDragging {
                                            withAnimation {
                                                selectedPage = 1
                                            }
                                        }
                                    }
                                }
                        }
                    }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            /*
            .simultaneousGesture(
                DragGesture()
                    .onChanged { _ in
                        isDragging = true
                    }
                    .onEnded { _ in
                        isDragging = false
                    }
            )
             */
            
            .onChange(of: selectedPage) { newPage in
                switch newPage {
                /*
                case 0:
                    if previousSelectedPage == 1 {
                        indices[2] = indices[newPage]
                    } else {
                        indices[1] = indices[newPage] + 1
                    }
                case 1:
                    if previousSelectedPage == 2 {
                        indices[0] = indices[newPage] - 1
                    } else {
                        indices[2] = indices[newPage] + 1
                    }
                case 2:
                    if previousSelectedPage == 0 {
                        indices[1] = indices[newPage] - 1
                    } else {
                        indices[0] = indices[newPage]
                    }
                default:
                    return
                 */
                case 0:
                    if previousSelectedPage == 1 {
                        indices[2] = indices[newPage]
                    } else {
                        indices[1] = indices[newPage] + 1
                    }
                case 1:
                    if previousSelectedPage == 2 {
                        indices[0] = indices[newPage] - 1
                    } else {
                        indices[2] = indices[newPage] + 1
                    }
                case 2:
                    if previousSelectedPage == 0 {
                        indices[1] = indices[newPage] - 1
                    } else {
                        indices[0] = indices[newPage]
                    }
                default:
                    return
                }
                
                previousSelectedPage = selectedPage
            }
             */
            
            let colors: [Color] = [Color.blue, Color.green, Color.yellow, Color.red, Color.purple, Color.brown]
            
            ZStack {
                TabView(selection: $firstSelectedPage) {
                    Rectangle()
                        .fill(colors[0])
                        .tag(0)
                        .matchedGeometryEffect(id: "StartMatch1", in: animation, properties: [.position], isSource: firstStartIsMatched)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX >= 0 {
                                            lockedFirstTabViewFocused = false
                                            firstSelectedPage = 1
                                            firstStartIsMatched = false
                                            secondEndIsMatched = true
                                        }
                                    }
                            }
                        }
                    Rectangle()
                        .fill(colors[1])
                        .tag(1)
                        .matchedGeometryEffect(id: "EndMatch2", in: animation, properties: [.position], isSource: false)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX >= 0 {
                                            firstStartIsMatched = true
                                            firstEndIsMatched = false
                                            secondSelectedPage = 2
                                        }
                                        if newMinX > 0 {
                                            firstTabViewFocused = false
                                        } else if newMinX <= 0 && lockedFirstTabViewFocused == true {
                                            firstTabViewFocused = true
                                        }
                                    }
                            }
                        }
                    Rectangle()
                        .fill(colors[2])
                        .tag(2)
                        .matchedGeometryEffect(id: "StartMatch2", in: animation, properties: [.position], isSource: false)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX <= 0 {
                                            firstStartIsMatched = false
                                            firstEndIsMatched = true
                                            secondSelectedPage = 1
                                            firstTabViewFocused = false
                                        }
                                        if newMinX < 0 {
                                            firstTabViewFocused = false
                                        } else if newMinX >= 0 && lockedFirstTabViewFocused == true {
                                            firstTabViewFocused = true
                                        }
                                    }
                            }
                        }
                    Rectangle()
                        .fill(colors[3])
                        .tag(3)
                        .matchedGeometryEffect(id: "EndMatch1", in: animation, properties: [.position], isSource: firstEndIsMatched)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX <= 0 {
                                            lockedFirstTabViewFocused = false
                                            firstSelectedPage = 2
                                            firstEndIsMatched = false
                                            secondStartIsMatched = true
                                        }
                                    }
                            }
                        }
                }
                .tabViewStyle(.page)
                .padding(.bottom, 400)
                .zIndex(firstTabViewFocused ? 1 : 0)
                
                TabView(selection: $secondSelectedPage) {
                    Rectangle()
                        .fill(colors[2])
                        .tag(0)
                        .matchedGeometryEffect(id: "StartMatch2", in: animation, properties: [.position], isSource: secondStartIsMatched)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX >= 0 {
                                            //firstEndIsMatched = true
                                            lockedFirstTabViewFocused = true
                                            secondStartIsMatched = false
                                            secondSelectedPage = 1
                                        }
                                    }
                            }
                        }
                    
                    Rectangle()
                        .fill(colors[3])
                        .tag(1)
                        .matchedGeometryEffect(id: "EndMatch1", in: animation, isSource: false)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX >= 0 {
                                            secondStartIsMatched = true
                                            secondEndIsMatched = false
                                            firstSelectedPage = 2
                                            firstTabViewFocused = true
                                        }
                                        if newMinX > 0 {
                                            firstTabViewFocused = true
                                        } else if newMinX <= 0 && lockedFirstTabViewFocused == false {
                                            firstTabViewFocused = false
                                        }
                                    }
                            }
                        }
                    
                    Rectangle()
                        .fill(colors[0])
                        .tag(2)
                        .matchedGeometryEffect(id: "StartMatch1", in: animation, isSource: false)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX <= 0 {
                                            secondStartIsMatched = false
                                            secondEndIsMatched = true
                                            firstSelectedPage = 1
                                            firstTabViewFocused = true
                                        }
                                        if newMinX < 0 {
                                            firstTabViewFocused = true
                                        } else if newMinX >= 0 && lockedFirstTabViewFocused == false {
                                            firstTabViewFocused = false
                                        }
                                    }
                            }
                        }
                    
                    Rectangle()
                        .fill(colors[1])
                        .tag(3)
                        .matchedGeometryEffect(id: "EndMatch2", in: animation, properties: [.position], isSource: secondEndIsMatched)
                        .background {
                            GeometryReader { geo in
                                Color.clear
                                    .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                        if newMinX <= 0 {
                                            //firstStartIsMatched = true
                                            lockedFirstTabViewFocused = true
                                            secondEndIsMatched = false
                                            secondSelectedPage = 2
                                        }
                                    }
                            }
                        }
                }
                .tabViewStyle(.page)
                .padding(.top, 400)
                .zIndex(firstTabViewFocused ? 0 : 1)
            }
            
            /*
            .onChange(of: firstSelectedPage) { newSelection in
                if newSelection == 0 || newSelection == 3 {
                    lockedFirstTabViewFocused = false
                } else if firstTabViewFocused == true {
                    if newSelection == 1 || newSelection == 2 {
                        lockedFirstTabViewFocused = true
                    }
                }
            }
            .onChange(of: secondSelectedPage) { newSelection in
                if newSelection == 0 || newSelection == 3 {
                    lockedFirstTabViewFocused = true
                }
            }
            */
            
            VStack {
                Text("1. selectedPage: \(firstSelectedPage)")
                Text("\n\n")
                Text("2. selectedPage: \(secondSelectedPage)")
                Text("\n\n")
                Text("Focused: \(firstTabViewFocused)")
                Text("Locked: \(lockedFirstTabViewFocused)")
            }
            .allowsHitTesting(false)
        }
        .coordinateSpace(name: "InfiniteZStack")
    }
    
    /*
    func updateCards(isForward: Bool) {
        if isForward {
            index += 1
        } else {
            index -= 1
        }
        selectedPage = 0
    }
     */
}

/*
extension InfinitePager {
    private struct LegacyInfinitePager: View {
        let content: (Int) -> Content
        
        @State private var index: Int = 0
        @State private var selectedPage: Int = 0
        
        @State private var isResting: Bool = true
        
        var body: some View {
            ZStack {
                content(0)
                    .hidden()
                TabView(selection: $selectedPage) {
                    ForEach(-2..<3) { offset in
                        content(index + offset)
                            .tag(offset)
                            .background {
                                GeometryReader { geo in
                                    Color.clear
                                        .onChange(of: abs(geo.frame(in: .named("InfiniteZStack")).minX) < 1.0 ) { newState in
                                            isResting = newState
                                        }
                                }
                            }
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                
                .onChange(of: isResting) { newState in
                    if isResting {
                        index += selectedPage
                        selectedPage = 0
                    }
                }
                
                VStack {
                    Text("\(selectedPage)")
                    Text("\(isResting)")
                }
            }
            .coordinateSpace(name: "InfiniteZStack")
        }
        
        
    }
    
    private struct Page: Identifiable, Hashable {
        let id = UUID()
        let position: Int
        let content: Content
        
        func hash(into hasher: inout Hasher) {
            hasher.combine(id)
            hasher.combine(position)
        }
        
        static func ==(lhs: Page, rhs: Page) -> Bool {
            return lhs.id == rhs.id && lhs.position == rhs.position
        }
    }
    
    @available(iOS 17.0, *)
    private struct NewInfinitePager: View {
        let content: (Int) -> Content
        
        @State private var pages: [Page] = []
        @State private var selectedPage: Page?
        //@State private var selectedPage: Int?
        @State private var isResting: Bool = true
        
        @State private var isInserting: Bool = false
        
        var body: some View {
            ZStack {
                content(0)
                    .hidden()
                
                ScrollView(.horizontal) {
                    HStack(spacing: 0) {
                        ForEach(pages, id: \.self) { page in
                            page.content
                                .containerRelativeFrame(.horizontal)
                                .background {
                                    GeometryReader { geo in
                                        Color.clear
                                            .onChange(of: geo.frame(in: .named("ScrollView")).minX == 0) { newState in
                                                isResting = newState
                                            }
                                    }
                                }
                        }
                    }
                    .scrollTargetLayout()
                }
                .coordinateSpace(name: "ScrollView")
                .scrollTargetBehavior(.paging)
                .scrollPosition(id: $selectedPage)
                .scrollIndicators(.hidden)
                
                VStack {
                    Text("\(selectedPage?.position)")
                    Text("\(pages.map {$0.position})")
                    Text("\(isResting)")
                    Text("\(pages.last?.position)")
                    Text("\(pages.first?.position)")
                }
            }
            
            .onAppear {
                pages = [
                    Page(position: -1, content: content(-1)),
                    Page(position: 0, content: content(0)),
                    Page(position: 1, content: content(1))
                ]
                selectedPage = pages[1]
            }
            
            
            .onChange(of: selectedPage) { new in
                if let lastPosition = pages.last?.position,
                   let firstPosition = pages.first?.position,
                   let newPosition = new?.position
                {
                    if newPosition == lastPosition {
                        insertNewPageEnd(lastPosition)
                    } else if newPosition == firstPosition {
                        insertNewPageStart(firstPosition)
                    }
                }
            }
            
            
            .onChange(of: isResting) {
                if isResting {
                    if let selectedPosition = selectedPage?.position {
                        cleanupPages(selectedPosition)
                    }
                    
                    /*
                    if let lastPosition = pages.last?.position,
                       let firstPosition = pages.first?.position,
                       let selectedPosition = selectedPage?.position
                    {
                        if selectedPosition == lastPosition {
                            insertNewPageEnd(lastPosition)
                        } else if selectedPosition == firstPosition {
                            if !isInserting {
                                insertNewPageStart(firstPosition)
                            } else {
                                selectedPage = pages[2]
                            }
                        }
                    }
                     */
                }
            }
        }
        
        func insertNewPageEnd(_ position: Int) {
            let tab = Page(position: position + 1, content: content(position + 1))
            
            pages.append(tab)
        }
        
        func insertNewPageStart(_ position: Int) {
            let tab = Page(position: position - 1, content: content(position - 1))
            
            pages.insert(tab, at: 0)
        }
        
        // Theoretically this can break, if the View is tapped in the short Delay timeframe without Dragging.
        // However this to me seems unlikely
        func cleanupPages(_ position: Int) {
            let currentPage = Page(position: position, content: content(position))
            
            let newPages = [
                Page(position: position - 1, content: content(position - 1)),
                currentPage,
                Page(position: position + 1, content: content(position + 1))
            ]
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
                if isResting {
                    pages = newPages
                    selectedPage = currentPage
                }
            }
        }
    }
}
 */

#Preview {
    //ScrollView {
        ExperimentalInfinitePager { idx in
            ZStack {
                Color(hue: Double((idx % 10)) / 10.0,
                      saturation: 0.6,
                      brightness: 0.9)
                .frame(height: 1000)
                VStack {
                    Spacer()
                    Text("Page \(idx)")
                        .font(.largeTitle)
                        .foregroundColor(.white)
                    
                    Spacer()
                    Spacer()
                }
            }
        }
    //}
}

struct CurrentBestInfinitePager<Content: View>: View {
    
    let content: (Int) -> Content
    
    //@State private var index: Int = 0
    
    @State private var indices: [Int] = [-1, 0, 1]
    
    //@State private var contents: [Content]
    
    @State private var selectedPage: Int = 1
    @State private var previousSelectedPage: Int = 1
    
    @State private var isTransition: Bool = false
    @State private var isDragging: Bool = true
    
    @State private var contentWidth: CGFloat = 0
    
    @Namespace private var animation
    
    var body: some View {
        ZStack {
            content(0)
                .hidden()
                .background {
                    GeometryReader { geo in
                        Color.clear
                            .onAppear {
                                contentWidth = geo.size.width
                            }
                            .onChange(of: geo.size.width) { newWidth in
                                contentWidth = newWidth
                            }
                    }
                }
            
            TabView(selection: $selectedPage) {
                content(indices[0])
                    .tag(0)
                
                    .background {
                        GeometryReader { geo in
                            Color.clear
                                .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                    if newMinX > 0 {
                                        selectedPage = 2
                                        if !isDragging {
                                            withAnimation {
                                                selectedPage = 1
                                            }
                                        }
                                    }
                                }
                        }
                    }
                 
                
                content(indices[1])
                    .tag(1)
                
                
                content(indices[2])
                    .tag(2)
                    .background {
                        GeometryReader { geo in
                            Color.clear
                                .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX) { newMinX in
                                    if newMinX < 0 {
                                        selectedPage = 0
                                        if !isDragging {
                                            withAnimation {
                                                selectedPage = 1
                                            }
                                        }
                                    }
                                }
                        }
                    }
            }
            .tabViewStyle(.page(indexDisplayMode: .never))
            /*
            .simultaneousGesture(
                DragGesture()
                    .onChanged { _ in
                        isDragging = true
                    }
                    .onEnded { _ in
                        isDragging = false
                    }
            )
             */
            
            .onChange(of: selectedPage) { newPage in
                switch newPage {
                case 0:
                    if previousSelectedPage == 1 {
                        indices[2] = indices[newPage]
                    } else {
                        indices[1] = indices[newPage] + 1
                    }
                case 1:
                    if previousSelectedPage == 2 {
                        indices[0] = indices[newPage] - 1
                    } else {
                        indices[2] = indices[newPage] + 1
                    }
                case 2:
                    if previousSelectedPage == 0 {
                        indices[1] = indices[newPage] - 1
                    } else {
                        indices[0] = indices[newPage]
                    }
                default:
                    return
                }
                
                previousSelectedPage = selectedPage
            }
        }
        .coordinateSpace(name: "InfiniteZStack")
    }
    
    /*
    func updateCards(isForward: Bool) {
        if isForward {
            index += 1
        } else {
            index -= 1
        }
        selectedPage = 0
    }
     */
}


/*
struct CombinedInfinitePager<Content: View>: View {
    let content: (Int) -> Content
    
    @State private var pages: [Page] = []
    @State private var selectedPage: Page?
    @State private var pageWidth: CGFloat = 0
    @State private var isResting: Bool = false
    
    var body: some View {
        TabView(selection: $selectedPage) {
            ForEach(pages, id: \.self) { page in
                page.content
                    .tag(page)
                    .background {
                        GeometryReader { geo in
                            Color.clear
                                .onChange(of: geo.frame(in: .named("InfiniteZStack")).minX == 0) { newState in
                                    isResting = newState
                                }
                        }
                    }
            }
        }
        .tabViewStyle(.page)
        .coordinateSpace(name: "InfiniteZStack")
        
        .overlay {
            VStack {
                Text("\(selectedPage?.position)")
            }
        }
        
        .onAppear() {
            pages = [
                Page(position: -1, content: content(-1)),
                Page(position: 0, content: content(0)),
                Page(position: 1, content: content(1))
            ]
            selectedPage = pages[1]
        }
        
        
        .onChange(of: selectedPage) { new in
            if let lastPosition = pages.last?.position,
               let firstPosition = pages.first?.position,
               let newPosition = new?.position
            {
                if newPosition == lastPosition {
                    insertNewPageEnd(lastPosition)
                }
                else if newPosition == firstPosition {
                    insertNewPageStart(firstPosition)
                }
            }
        }
        
        /*
        .onChange(of: isResting) { newState in
            if newState {
                if let lastPosition = pages.last?.position,
                   let firstPosition = pages.first?.position,
                   let newPosition = selectedPage?.position
                {
                    if newPosition == lastPosition {
                        insertNewPageEnd(lastPosition)
                    }
                    else if newPosition == firstPosition {
                        insertNewPageStart(firstPosition)
                    }
                }
            }
        }
         */
    }
    
    func insertNewPageEnd(_ position: Int) {
        let tab = Page(position: position + 1, content: content(position + 1))
        
        pages.append(tab)
        //pages.remove(at: 0)
    }
    
    func insertNewPageStart(_ position: Int) {
        let tab = Page(position: position - 1, content: content(position - 1))

        pages.insert(tab, at: 0)
        //pages.removeLast()
    }
}

extension CombinedInfinitePager {
    private struct Page: Identifiable, Hashable {
        let id = UUID()
        let position: Int
        let content: Content
        
        func hash(into hasher: inout Hasher) {
            hasher.combine(id)
            hasher.combine(position)
        }
        
        static func ==(lhs: Page, rhs: Page) -> Bool {
            return lhs.id == rhs.id && lhs.position == rhs.position
        }
    }
}

#Preview("HopeInfinitePager") {
    CombinedInfinitePager { idx in
        ZStack {
            Color(hue: Double((idx % 10)) / 10.0,
                  saturation: 0.6,
                  brightness: 0.9)
            .frame(height: 1000)
            VStack {
                Text("Page \(idx)")
                    .font(.largeTitle)
                    .foregroundColor(.white)
                Spacer()
            }
        }
    }
}
*/

struct ScrollViewInfinitePager<Content: View>: View {
    let content: (Int) -> Content
    
    @State private var index: Int = 0
    @State private var offset: CGFloat = 0
    @State private var dragOffset: CGFloat = 0
    
    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            
            //let items = [pages.last!] + pages + [pages.first!]  // duplicated edges
            //let count = pages.count
            
            HStack(spacing: 0) {
                content(index - 2)
                    .frame(width: width)
                content(index - 1)
                    .frame(width: width)
                content(index)
                    .frame(width: width)
                content(index + 1)
                    .frame(width: width)
                content(index + 2)
                    .frame(width: width)
            }
            .offset(x: offset + dragOffset)
            .gesture(
                DragGesture(minimumDistance: 20)
                    .onChanged { value in
                        dragOffset = value.translation.width
                    }
                    .onEnded { value in
                        let predicted = offset + value.predictedEndTranslation.width
                        let predictedIndex = Int(round(-predicted / width))
                        let currentIndex = Int(round(-offset / width))
                        
                        var newIndex: Int {
                            if predictedIndex > currentIndex {
                                return currentIndex + 1
                            } else if predictedIndex < currentIndex {
                                return currentIndex - 1
                            } else {
                                return currentIndex
                            }
                        }
                        
                        snap(to: newIndex, width: width, count: 3)
                    }
            )
            .onAppear {
                offset = -width
            }
        }
    }
    
    private func snap(to index: Int, width: CGFloat, count: Int) {
        let final = min(max(index, 0), count + 1)
        
        withAnimation(.spring()) {
            offset = -CGFloat(final) * width
            dragOffset = 0
        }
        
        // Wrap around after snap finishes
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.001) {
            if final == 0 {
                self.index -= 3
                offset = -CGFloat(count) * width
            } else if final == count + 1 {
                offset = -CGFloat(1) * width
                self.index += 3
            }
        }
    }
}


#Preview {
    ScrollView {
        ScrollViewInfinitePager { idx in
            ZStack {
                Color(hue: Double((idx % 10)) / 10.0,
                      saturation: 0.6,
                      brightness: 0.9)
                .frame(height: 1000)
                VStack {
                    Spacer()
                    Text("Page \(idx)")
                        .font(.largeTitle)
                        .foregroundColor(.white)
                    
                    Spacer()
                    Spacer()
                }
            }
        }
    }
}

struct InfiniteChatPager<Content: View>: View {
    let pages: [Content]
    
    @State private var currentIndex: Int = 1     // starts on real first page
    @State private var offset: CGFloat = 0       // current animated offset
    @State private var dragOffset: CGFloat = 0
    
    var body: some View {
        GeometryReader { geo in
            let width = geo.size.width
            
            let items = [pages.last!] + pages + [pages.first!]  // duplicated edges
            let count = pages.count
            
            HStack(spacing: 0) {
                ForEach(0..<items.count, id: \.self) { i in
                    items[i]
                        .frame(width: width)
                }
            }
            .offset(x: offset + dragOffset)
            .gesture(
                DragGesture()
                    .onChanged { value in
                        dragOffset = value.translation.width
                    }
                    .onEnded { value in
                        let predicted = offset + value.predictedEndTranslation.width
                        let predictedIndex = Int(round(-predicted / width))
                        let currentIndex = Int(round(-offset / width))
                        
                        var newIndex: Int {
                            if predictedIndex > currentIndex {
                                return currentIndex + 1
                            } else if predictedIndex < currentIndex {
                                return currentIndex - 1
                            } else {
                                return currentIndex
                            }
                        }
                        
                        snap(to: newIndex, width: width, count: count)
                    }
            )
            .onAppear {
                offset = -width
            }
        }
    }
    
    private func snap(to index: Int, width: CGFloat, count: Int) {
        let final = min(max(index, 0), count + 1)
        
        withAnimation(.spring()) {
            offset = -CGFloat(final) * width
            dragOffset = 0
        }
        
        // Wrap around after snap finishes
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.001) {
            if final == 0 {
                offset = -CGFloat(count) * width
            } else if final == count + 1 {
                offset = -CGFloat(1) * width
            }
        }
    }
}


#Preview("ChatPager") {
    InfiniteChatPager(pages: [
        Color.red
            .frame(height: 1000),
        Color.green
            .frame(height: 1000),
        Color.yellow
            .frame(height: 1000)
    ])
}
*/
