//  Created on 22.08.26.

import SwiftUI
import UIKit
import Shared
import SwiftUIIntrospect


struct NavigationTitleAdaptiveModifier<LargeView: View>: ViewModifier {
    let smallText: String
    let largeView: () -> LargeView
    
    // Only needed for iOS 16-18
    let backgroundColor: Color
    
    // Only needed for debugging
    let debugHierarchy: Binding<String>?

    // This State is only relevant for iOS 26 onwards
    @State private var titleOne: String
    @State private var titleTwo: String
    @State private var isFirstAppear: Bool = true
    @State private var isPopping: Bool = false
    
    // These are relevant for iOS 16-18
    @State private var headerHeight: CGFloat = 0
    @State private var scrollOffset: CGFloat = 0
    @State private var showNavTitle = false
    @State private var navigationTitle: String = ""
    
    // This is relevant for iOS 16
    @State private var snapDelegate: HeaderSnapDelegate?
    @State private var navTitleAlpha: CGFloat = 0
    
    init(
        smallText: String,
        largeView: @escaping () -> LargeView,
        backgroundColor: Color,
        debugHierarchy: Binding<String>?
    ) {
        self.smallText = smallText
        self.largeView = largeView
        self.backgroundColor = backgroundColor
        self.debugHierarchy = debugHierarchy
        self.titleOne = smallText
        self.titleTwo = String(repeating: "I", count: 10)
    }
    
    
    // A brief overview over this UIKit monstrocity.
    // Problem: I want to place a custom large Title (Bonus: have two lines for Title for >iOS26)
    // Hypothesis: There is no reasonable way of achieving this cleanly without using Introspect.
    
    // In iOS 16-18, this is relatively complicated. I have tried inserting a custom View into the View Hierarchy, this however turns out to be extremely brittle and not work reliably across versions.
    // The only modifiable thing with introspect seems to be the NSAttributedString.Key Dict, but this is then applied to the NavBar in General carrying over to other views and breaking smooth animations
    // Instead I basically reimplemented the behavior by inserting the customTitle into a ScrollView and having that ScrollView snap accordingly. To get the same visual effect, the navigationBar's opacity is controlled by scrollOffset as well as the small navTitle
    
    // In iOS 26, apple introduced a property for doing exactly this! Pure SwiftUI!
    // Sadly it has one last bug, tested in iOS 26.0-26.2 where the custom largeTitle will only disappear after the View Popping animation has completed. This is fixed through observing whether the View is being popped and hiding it if so
    // Popping the View sadly also causes the unintended side-effect of the title being visually cut off arbitrarily
    // Lastly iOS 26 implements two-line small titles, which could be brittle
    
    // Tested in Simulator for iOS 16-26, see each version for details
    // iOS 16: the smallTitle expansion doesn't correctly animate
    // iOS 17: works perfectly
    // iOS 18: works perfectly
    // iOS 26: works perfectly

    func body(content: Content) -> some View {
        content
            .modify { view in
                if #available(iOS 26.0, *) {
                    view.navigationTitle(titleOne)
                        .toolbar {
                            ToolbarItem(placement: .largeTitle) {
                                VStack {
                                    // In iOS 26, the title is cut off visually without this on View pop
                                    // In iOS 26.2 Simulator, a too large title causes collapse...
                                    if #available(iOS 26.2, *) { } else {
                                        Spacer()
                                            .frame(height: 24)
                                    }
                                    largeView()
                                        .opacity(isPopping ? 0 : 1) // Required for iOS 26, see above
                                }
                            }
                            ToolbarItem(placement: .subtitle) {
                                Text(titleTwo)
                                    .font(.subheadline.weight(.semibold))
                            }
                        }
                        .background {
                            NavigationPopObserver(isPopping: $isPopping)
                                .frame(width: 0, height: 0)
                        }
                        .onAppear {
                            isPopping = false
                        }
                } else if #available(iOS 17.0, *) {
                    ScrollView {
                        VStack(spacing: 0) {
                            largeView()
                                .padding(.bottom, 12)
                                .onGeometryChange(for: CGFloat.self) { geometry in
                                    geometry.size.height
                                } action: { newHeight in
                                    headerHeight = newHeight
                                }
                                .onGeometryChange(for: CGFloat.self) { geometry in
                                    geometry.frame(in: .named("scroll")).minY
                                } action: { minY in
                                    scrollOffset = minY
                                    showNavTitle = minY + headerHeight - 12 <= 0
                                }
                                .mask {
                                    VStack {
                                        Spacer()
                                        Rectangle()
                                            .frame(height: headerHeight + scrollOffset)
                                    }
                                }
                                // This is only used in iOS 18
                                .onAppear {
                                    showNavTitle = scrollOffset + headerHeight - 12 <= 0
                                }
                            
                            view
                        }
                        .scrollTargetLayout()
                    }
                    .background(backgroundColor)
                    .scrollTargetBehavior(HeaderSnapBehavior(headerHeight: headerHeight))
                    .coordinateSpace(name: "scroll")
                    .navigationBarTitleDisplayMode(.inline)
                    .navigationTitle(smallText)
                } else {
                    ScrollView {
                        VStack(spacing: 8) {
                            largeView()
                                .onGeometryChange(for: CGFloat.self) { geometry in
                                    geometry.size.height
                                } action: { newHeight in
                                    headerHeight = newHeight
                                }
                                .onGeometryChange(for: CGFloat.self) { geometry in
                                    geometry.frame(in: .named("scroll")).minY
                                } action: { minY in
                                    scrollOffset = minY
                                    showNavTitle = minY + headerHeight - 12 <= 0
                                }
                                .mask {
                                    VStack {
                                        Spacer()
                                        Rectangle()
                                            .frame(height: headerHeight + scrollOffset)
                                    }
                                }
                            
                            view
                        }
                    }
                    .introspect(.scrollView, on: .iOS(.v16), .iOS(.v17), .iOS(.v18)) { scrollView in
                        let delegate = HeaderSnapDelegate(headerHeight: headerHeight, scrollOffset: $scrollOffset)
                        snapDelegate = delegate
                        scrollView.delegate = snapDelegate
                    }
                    .background(backgroundColor)
                    .coordinateSpace(name: "scroll")
                    .navigationBarTitleDisplayMode(.inline)
                    .navigationTitle(smallText)
                }
            }
        
            .introspect(.viewController, on: .iOS(.v16)) { viewController in
                //var result: String = ""
                
                if let navBar = viewController.navigationController?.navigationBar {
                    if let view = navBar.subviews.first(where: { "\(type(of: $0))".contains("_UIBarBackground")}) {
                        if (scrollOffset + headerHeight >= 0) {
                            view.alpha = 0
                        } else {
                            let difference = scrollOffset + headerHeight
                            let scaled = -difference / 5
                            let capped = min(scaled, 1)
                            view.alpha = capped
                        }
                    }
                    
                    if let view = navBar.subviews.first(where: { "\(type(of: $0))".contains("UINavigationBarContentView")}),
                       let view = view.subviews.first(where: { "\(type(of: $0))".contains("UINavigationBarTitleControl")}),
                       let view = view.subviews.first,
                       let view = view as? UILabel
                    {
                        // This is an imperfect solution, but for some reason, in iOS 16 setting
                        // only the alpha makes the animation visually trigger when scrolling
                        UIView.animate(withDuration: 0.175) {
                            view.alpha = showNavTitle ? 1 : 0
                            view.textColor = showNavTitle ? .label : .clear
                        }
                    }
                }
                
                //debugHierarchy?.wrappedValue = result
            }
        
            .introspect(.viewController, on: .iOS(.v17), .iOS(.v18)) { viewController in
                //var result: String = ""
                
                if let navBar = viewController.navigationController?.navigationBar {
                    if let view = navBar.subviews.first(where: { "\(type(of: $0))".contains("_UIBarBackground")}) {
                        if (scrollOffset + headerHeight >= 0) {
                            view.alpha = 0
                        } else {
                            let difference = scrollOffset + headerHeight
                            let scaled = -difference / 5
                            let capped = min(scaled, 1)
                            view.alpha = capped
                        }
                    }
                    
                    if let view = navBar.subviews.first(where: { "\(type(of: $0))".contains("UINavigationBarContentView")}),
                       let view = view.subviews.first(where: { "\(type(of: $0))".contains("UINavigationBarTitleControl")}),
                       let view = view.subviews.first,
                       let view = view.subviews.first
                    {
                        UIView.animate(
                            withDuration: 0.175,
                            animations: {
                                view.alpha = showNavTitle ? 1 : 0
                            }
                        )
                    }
                }
                
                //debugHierarchy?.wrappedValue = result
            }
        
            .introspect(.viewController, on: .iOS(.v26), .iOS(.v27)) { viewController in
                //var result: String = ""
                if #available(iOS 26.0, *) {
                    if let navBar = viewController.navigationController?.navigationBar {
                        if let view = navBar.subviews.first(where: { "\(type(of: $0))".contains("NavigationBarContentView")}),
                           let container = view.subviews.first,
                           let titleContainer = container.subviews.first,
                           let buttonContainer = container.subviews.last,
                           let title = titleContainer.subviews.first
                        {
                            let titleFrame = title.convert(title.bounds, to: container)
                            let titleMidX = titleFrame.midX
                            
                            let buttonFrames = buttonContainer.subviews.map { button in
                                button.convert(button.bounds, to: container)
                            }
                            
                            let leftEdge = buttonFrames
                                .filter { $0.maxX <= titleMidX }
                                .map(\.maxX)
                                .max() ?? container.bounds.minX

                            let rightEdge = buttonFrames
                                .filter { $0.minX >= titleMidX }
                                .map(\.minX)
                                .min() ?? container.bounds.maxX

                            let availableWidth: CGFloat = rightEdge - leftEdge - 26
                            
                            let preferredFont = UIFont.preferredFont(forTextStyle: .subheadline)
                                                                                    
                            let subtitleFont = UIFont.systemFont(
                                ofSize: preferredFont.pointSize,
                                weight: .semibold
                            )
                            
                            let split = splitIntoBalancedLines(smallText, font: subtitleFont, width: availableWidth)
                            
                            // Rough pass before layout has finished
                            titleOne = split.0
                            titleTwo = split.1 ?? ""
                            
                            // Exact pass after layout has finished
                            DispatchQueue.main.async {
                                titleOne = split.0
                                titleTwo = split.1 ?? ""
                            }
                            
                            //result += getHierarchyString(view)
                        }
                    }
                }
                //debugHierarchy?.wrappedValue = result
            }
         
    }
    
    @available(iOS 17.0, *)
    private struct HeaderSnapBehavior: ScrollTargetBehavior {
        let headerHeight: CGFloat
        
        func updateTarget(_ target: inout ScrollTarget, context: TargetContext) {
            // Only snap if scrolling is within the header area
            if target.rect.minY >= headerHeight {
                return // Content area - no snapping
            }
            
            // Determine which half of the header we're in
            let headerMidpoint = headerHeight / 2 - 12
            
            if target.rect.minY < headerMidpoint {
                // Upper half - snap to top of header
                target.rect.origin.y = 0
            } else {
                // Lower half - snap to bottom of header
                target.rect.origin.y = headerHeight
            }
        }
    }
    
    private func textWidth(_ text: String, font: UIFont) -> CGFloat {
        (text as NSString).size(withAttributes: [.font: font]).width
    }
    
    private func splitIntoBalancedLines(
        _ text: String,
        font: UIFont,
        width: CGFloat
    ) -> (String, String?) {
        
        if textWidth(text, font: font) < width {
            return (text, nil)
        }
        
        let words = text.split(separator: " ", omittingEmptySubsequences: true)

        if words.count <= 1 {
            return (text, nil)
        }

        var bestSplit: (index: Int, difference: CGFloat)?

        for index in 1..<words.count {
            let firstLine = words[..<index].joined(separator: " ")
            let secondLine = words[index...].joined(separator: " ")

            let firstWidth = textWidth(firstLine, font: font)
            let secondWidth = textWidth(secondLine, font: font)

            guard firstWidth <= width, secondWidth <= width else {
                continue
            }

            let difference = abs(firstWidth - secondWidth)

            if bestSplit == nil || difference < bestSplit!.difference {
                bestSplit = (index, difference)
            }
        }

        if let bestSplit {
            let firstLine = words[..<bestSplit.index].joined(separator: " ")
            let secondLine = words[bestSplit.index...].joined(separator: " ")
            return (firstLine, secondLine)
        }

        // Fallback: fit as many words as possible on the first line
        return wrapAtWordBoundary(text, font: font, width: width)
    }
    
    private func wrapAtWordBoundary(
        _ text: String,
        font: UIFont,
        width: CGFloat
    ) -> (String, String?) {
        
        let words = text.split(separator: " ", omittingEmptySubsequences: false)
        var firstLine = ""

        for word in words {
            let candidate = firstLine.isEmpty
                ? String(word)
                : firstLine + " " + word

            if textWidth(candidate, font: font) > width {
                break
            }

            firstLine = candidate
        }

        guard !firstLine.isEmpty else {
            return (text, nil)
        }

        let remainder = text
            .dropFirst(firstLine.count)
            .trimmingCharacters(in: .whitespaces)

        return (
            firstLine,
            remainder.isEmpty ? nil : String(remainder)
        )
    }
    
    func fontSizeForHeight(_ targetHeight: CGFloat) -> CGFloat {
        let label = UILabel()
        label.text = "Ag"  // Use characters with height variance
        
        let fontSize: CGFloat = 10
        for size in stride(from: 10.0, through: 100.0, by: 1.0) {
            label.font = UIFont.systemFont(ofSize: size)
            label.sizeToFit()
            if label.frame.height >= targetHeight {
                return size - 1
            }
        }
        return fontSize
    }
    
    private final class HeaderSnapDelegate: NSObject, UIScrollViewDelegate {
        let headerHeight: CGFloat
        let scrollOffset: Binding<CGFloat>
        let debugHierarchy: Binding<String>?
        
        init(headerHeight: CGFloat, scrollOffset: Binding<CGFloat>, debugHierarchy: Binding<String>? = nil) {
            self.headerHeight = headerHeight
            self.scrollOffset = scrollOffset
            self.debugHierarchy = debugHierarchy
        }

        func scrollViewWillEndDragging(
            _ scrollView: UIScrollView,
            withVelocity velocity: CGPoint,
            targetContentOffset: UnsafeMutablePointer<CGPoint>
        ) {
            debugHierarchy?.wrappedValue = ""
            debugHierarchy?.wrappedValue += "HeaderHeight: \(headerHeight)\n"
            let proposedY = targetContentOffset.pointee.y + scrollView.adjustedContentInset.top
            
            debugHierarchy?.wrappedValue += "Proposed: \(proposedY + scrollView.adjustedContentInset.top)\n"

            // Only snap if scrolling is within the header area.
            guard proposedY < headerHeight else {
                return
            }

            // Determine which half of the header we're in.
            let headerMidpoint = headerHeight / 2
            
            
            debugHierarchy?.wrappedValue += "headerMid: \(headerMidpoint)\n"

            if proposedY < headerMidpoint {
                targetContentOffset.pointee.y = -scrollView.adjustedContentInset.top
            } else {
                // Lower half - snap to bottom of header.
                targetContentOffset.pointee.y = headerHeight - scrollView.adjustedContentInset.top
            }
        }
    }
}

extension View {
    func navigationTitleAdaptive<LargeView: View>(
        _ smallText: String,
        debugHierarchy: Binding<String>? = nil,
        backgroundColor: Color = Color(.systemGroupedBackground),
        @ViewBuilder largeView: @escaping () -> LargeView
    ) -> some View {
        modifier(
            NavigationTitleAdaptiveModifier(
                smallText: smallText,
                largeView: largeView,
                backgroundColor: backgroundColor,
                debugHierarchy: debugHierarchy
            )
        )
    }
}

struct NavigationPopObserver: UIViewControllerRepresentable {
    @Binding var isPopping: Bool

    func makeUIViewController(context: Context) -> Observer {
        Observer(isPopping: $isPopping)
    }

    func updateUIViewController(
        _ uiViewController: Observer,
        context: Context
    ) {
        uiViewController.isPopping = $isPopping
    }

    final class Observer: UIViewController {
        var isPopping: Binding<Bool>

        init(isPopping: Binding<Bool>) {
            self.isPopping = isPopping
            super.init(nibName: nil, bundle: nil)

            view.isHidden = true
            view.isUserInteractionEnabled = false
        }

        @available(*, unavailable)
        required init?(coder: NSCoder) {
            fatalError()
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)

            guard let coordinator = transitionCoordinator else {
                return
            }

            isPopping.wrappedValue = true

            coordinator.animate(alongsideTransition: nil) {
                [weak self] context in

                // If cancelled, we're still on this screen.
                if context.isCancelled {
                    self?.isPopping.wrappedValue = false
                }
            }
        }
    }
}



// Debugging

#Preview("D1") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "Tue, 31. Mar. 2026",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [
                KotlinPair(first: "1,0", second: 1),
                KotlinPair(first: "1,3", second: 3),
                KotlinPair(first: "1,7", second: 5),
                KotlinPair(first: "2,0", second: 7),
                KotlinPair(first: "2,3", second: 10),
                KotlinPair(first: "2,7", second: 20),
                KotlinPair(first: "3,0", second: 30),
                KotlinPair(first: "3,3", second: 40),
                KotlinPair(first: "3,7", second: 70),
                KotlinPair(first: "4,0", second: 100),
                KotlinPair(first: "5,0", second: 150)
            ],
            gradeAvg: "3,98",
            resultCount: "436",
            otherCounts: [KotlinPair(first: "fehlend (Krankschreibung)", second: 6)]
        )
    )
    
    return NavigationStack {
        ExamDetailView(exam: exam)
    }
}

#Preview {
    VeryCustomNavBarView()
}

#Preview("App") {
    ContentView()
        .environmentObject(TucanService())
        .environmentObject(IdentityService(previewMode: true))
        .environmentObject(MensaService())
        .environmentObject(KeyboardHeightHelper())
        .environmentObject(SettingsService())
}

#Preview("Test") {
    if #available(iOS 26.0, *) {
        NavigationStack {
            ScrollView {
                NavigationLink("Test") {
                    TestDetailView()
                }
                
                ForEach(1 ... 50, id: \.self) { index in
                    Text("Index: \(index)")
                }
            }
            .navigationTitle("Testing")
        }
    }
}

@available(iOS 26.0, *)
private struct TestDetailView: View {
    @State private var position = ScrollPosition()

    var body: some View {
        ScrollView {
            LazyVStack {
                ForEach(1 ... 50, id: \.self) { index in
                    Text("Index: \(index)")
                }
            }
            .frame(maxWidth: .infinity)
        }
        .scrollPosition($position)
        .navigationTitle("Software-Engineering - Maintenance and Quality Assurance")
        .toolbar {
            ToolbarItem(placement: .largeTitle) {
                VStack {
                    Text("18-sc-1030")
                    Text("Software-Engineering - Maintenance and Quality Assurance")
                        .fontWeight(.bold)
                    Text("Wed, 15. Jul. 2026 09:50-11:30")
                }
            }
        }
    }
}


private func printHierarchy(_ view: UIView, indent: Int = 0) {
    let indentString = String(repeating: "  ", count: indent)
    print("\(indentString)\(type(of: view)) frame: \(view.frame)")
    
    for subview in view.subviews {
        printHierarchy(subview, indent: indent + 1)
    }
}

private func getHierarchyString(_ view: UIView, indent: Int = 0) -> String {
    let indentString = String(repeating: "  ", count: indent)
    //var result = "\(indentString)\(type(of: view)) frame: \(view.frame)\n"
    var result = String("\(indentString)\(type(of: view))".prefix(40)) + "\n"
    
    for subview in view.subviews {
        result += getHierarchyString(subview, indent: indent + 1)
    }
    return result
}

private struct VeryCustomNavBarView: View {
    @Namespace var animation
    
    @State var hierarchyDebug = "Start"
    
    var body: some View {
        NavigationStack {
            ScrollView {
                NavigationLink("Navigate") {
                    ScrollView {
                        Text(hierarchyDebug)
                        DemoView2()
                            
                            /*
                             .navigationTitleAdaptive("Screen2Small", debugHierarchy: $hierarchyDebug) {
                             Text("Screen2Big")
                             }
                             */
                    }
                    .navigationTitleAdaptive("Software-Engineering - Maintenance and Quality Assurance", debugHierarchy: $hierarchyDebug) {
                        VStack {
                            Text("18-sc-1030")
                                .foregroundStyle(.secondary)
                            Text("Software-Engineering - Maintenance and Quality Assurance")
                                .fontWeight(.bold)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                            Text("Wed, 15. Jul. 2026 09:50-11:30")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                        .frame(maxWidth: .infinity, alignment: .center)
                        //Text("Test")
                    }
                    .toolbar {
                        ToolbarItem {
                            Text("Test")
                        }
                    }
                }
                
                ForEach(1 ... 50, id: \.self) { index in
                    Text("Index: \(index)")
                }
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("Testing")
            .toolbar {
                ToolbarItem {
                    Text("Test")
                }
            }
        }
    }
}


private struct DemoView1: View {
    var body: some View {
        ScrollView {
            ForEach(1 ... 50, id: \.self) { index in
                Text("Index: \(index)")
            }
            .frame(maxWidth: .infinity)
        }
        .navigationTitle("Test2")
        .navigationTitleAccessoryCleanup()
    }
}

private struct DemoView2: View {
    var body: some View {
        ScrollView {
            VStack {
                NavigationLink("Test") { DemoView1() }
                
                ForEach(1 ... 50, id: \.self) { index in
                    Text("Index: \(index)")
                }
                .frame(maxWidth: .infinity)
            }
        }
    }
}



private struct BarContent: View {
    var body: some View {
        Button {
            print("Profile tapped")
        } label: {
            ProfilePicture()
        }
    }
}

private struct ProfilePicture: View {
    var body: some View {
        Circle()
            .fill(
                LinearGradient(
                    gradient: Gradient(colors: [.red, .blue]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .frame(width: 40, height: 40)
            .padding(.leading, 8)
    }
}


