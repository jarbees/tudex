//  Created on 02.11.26.

import SwiftUI
import SwiftUIIntrospect

// ACCESSORY HELPERS

@MainActor
private final class TitleVisibilityObservation: NSObject {
    private var observation: NSKeyValueObservation?

    init(wrapper: UIView, interactive: UIView) {
        super.init()
        
        observation = wrapper.observe(\.alpha, options: [.initial, .new]) { [weak interactive] wrapper, _ in
            let enabled = wrapper.alpha > 0.5
            DispatchQueue.main.async { interactive?.isUserInteractionEnabled = enabled }
        }
    }
}

@MainActor
private final class TitleAccessoryPair {
    private var visualHost: UIHostingController<AnyView>?
    private var interactiveHost: UIHostingController<AnyView>?
    private var visibilityObservation: TitleVisibilityObservation?
    private var constraints: [NSLayoutConstraint] = []
    
    private weak var visibilitySource: UIView?

    func attach(
        _ content: AnyView,
        visualParent: UIView,
        interactiveParent: UIView,
        visibilitySource: UIView,
        horizontal: NSLayoutXAxisAnchor,
        vertical: NSLayoutYAxisAnchor,
        spacing: CGFloat,
        layoutTrailing: NSLayoutXAxisAnchor? = nil
    ) {
        let visualContent = AnyView(
            content
                .allowsHitTesting(false)
                .accessibilityHidden(true)
        )

        let interactiveContent = AnyView(
            content.colorMultiply(.clear)
        )

        if let visualHost,
           let interactiveHost,
           visualHost.view.superview === visualParent,
           interactiveHost.view.superview === interactiveParent,
           self.visibilitySource === visibilitySource {

            visualHost.rootView = visualContent
            interactiveHost.rootView = interactiveContent

            updateConstraints(
                horizontal: horizontal,
                vertical: vertical,
                spacing: spacing,
                layoutTrailing: layoutTrailing
            )

            return
        }

        remove()

        let visualHost = UIHostingController(rootView: visualContent)
        let interactiveHost = UIHostingController(rootView: interactiveContent)

        let visual = visualHost.view!
        let interactive = interactiveHost.view!

        for view in [visual, interactive] {
            view.backgroundColor = .clear
            view.translatesAutoresizingMaskIntoConstraints = false
        }

        visualParent.addSubview(visual)
        interactiveParent.addSubview(interactive)

        self.visualHost = visualHost
        self.interactiveHost = interactiveHost
        self.visibilitySource = visibilitySource

        updateConstraints(
            horizontal: horizontal,
            vertical: vertical,
            spacing: spacing,
            layoutTrailing: layoutTrailing
        )

        visibilityObservation = TitleVisibilityObservation(
            wrapper: visibilitySource,
            interactive: interactive
        )
    }

    func remove() {
        visibilityObservation = nil

        NSLayoutConstraint.deactivate(constraints)
        constraints.removeAll()

        visualHost?.view.removeFromSuperview()
        interactiveHost?.view.removeFromSuperview()

        visualHost = nil
        interactiveHost = nil
        visibilitySource = nil
    }

    private func updateConstraints(
        horizontal: NSLayoutXAxisAnchor,
        vertical: NSLayoutYAxisAnchor,
        spacing: CGFloat,
        layoutTrailing: NSLayoutXAxisAnchor?
    ) {
        guard let visualHost, let interactiveHost else { return }

        NSLayoutConstraint.deactivate(constraints)

        constraints = [visualHost.view, interactiveHost.view].flatMap { view in
            [
                view.leadingAnchor.constraint(
                    equalTo: horizontal,
                    constant: spacing
                ),
                view.centerYAnchor.constraint(
                    equalTo: vertical
                )
            ]
        }

        if let layoutTrailing {
            constraints.append(
                visualHost.view.trailingAnchor.constraint(
                    equalTo: layoutTrailing
                )
            )
        }

        NSLayoutConstraint.activate(constraints)
    }
}

@MainActor
private final class TitleAccessoryInstallation {
    private let large = TitleAccessoryPair()
    private let inline = TitleAccessoryPair()
    
    weak var navigationBar: UINavigationBar?
    weak var controller: UIViewController?

    func attachLarge(
        _ content: AnyView,
        to parent: UIView,
        label: UILabel
    ) {
        large.attach(
            content,
            visualParent: label,
            interactiveParent: parent,
            visibilitySource: label,
            horizontal: label.trailingAnchor,
            vertical: label.centerYAnchor,
            spacing: 8
        )
    }

    func attachInline(
        _ content: AnyView,
        titleControl: UIView,
        label: UILabel,
        visualParent: UIView,
        interactiveParent: UIView,
        visibilitySource: UIView
    ) {
        inline.attach(
            content,
            visualParent: visualParent,
            interactiveParent: interactiveParent,
            visibilitySource: visibilitySource,
            horizontal: label.trailingAnchor,
            vertical: label.centerYAnchor,
            spacing: 6,
            layoutTrailing: titleControl.trailingAnchor
        )
    }
    
    func removeInline() {
        inline.remove()
    }

    func remove() {
        large.remove()
        inline.remove()
    }
    
    func scheduleAttachment(
        for controller: UIViewController,
        _ attach: @escaping (UINavigationController, UINavigationBar) -> Void
    ) {
        guard let navigation = activeTitleNavigation(for: controller) else {
            return
        }
        
        func performAttachment() {
            guard activeTitleNavigation(for: controller) === navigation else {
                return
            }

            let bar = navigation.navigationBar

            self.navigationBar = bar
            self.controller = controller

            activateTitleInstallation(self, in: bar)
            attach(navigation, bar)
        }

        if #available(iOS 26.0, *) {
            performAttachment()
        } else {
            DispatchQueue.main.async {
                performAttachment()
            }
        }
    }
}

@MainActor
private final class ActiveTitleInstallation {
    weak var value: TitleAccessoryInstallation?
}

private var activeTitleInstallationKey: UInt8 = 0

@MainActor
private func activateTitleInstallation(
    _ installation: TitleAccessoryInstallation,
    in bar: UINavigationBar
) {
    let holder: ActiveTitleInstallation

    if let existing = objc_getAssociatedObject(
        bar,
        &activeTitleInstallationKey
    ) as? ActiveTitleInstallation {
        holder = existing
    } else {
        holder = ActiveTitleInstallation()

        objc_setAssociatedObject(
            bar,
            &activeTitleInstallationKey,
            holder,
            .OBJC_ASSOCIATION_RETAIN_NONATOMIC
        )
    }

    guard holder.value !== installation else { return }

    holder.value?.remove()
    holder.value = installation
}

@MainActor
private func deactivateTitleInstallation(
    _ installation: TitleAccessoryInstallation,
    in bar: UINavigationBar
) {
    let holder = objc_getAssociatedObject(
        bar,
        &activeTitleInstallationKey
    ) as? ActiveTitleInstallation

    if holder?.value === installation {
        holder?.value = nil
    }

    installation.remove()
}


// OTHER HELPERS

@MainActor
private func titleClass(_ view: UIView, contains name: String) -> Bool {
    String(describing: type(of: view)).contains(name)
}

@MainActor
private func findSubview(in root: UIView, where matches: (UIView) -> Bool) -> UIView? {
    if matches(root) {
        return root
    }

    for child in root.subviews {
        if let found = findSubview(in: child, where: matches) {
            return found
        }
    }

    return nil
}

@MainActor
private func findSubview<T: UIView>(
    in root: UIView,
    ofType type: T.Type
) -> T? {
    findSubview(in: root) { $0 is T } as? T
}

@MainActor
private func activeTitleNavigation(for controller: UIViewController) -> UINavigationController? {
    guard let navigation = controller.navigationController,
          let top = navigation.topViewController else { return nil }
    var current: UIViewController? = controller
    while let candidate = current {
        if candidate === top { return navigation }
        current = candidate.parent
    }
    return nil
}

@MainActor
private func refreshTitleHitTesting(in navigationBar: UINavigationBar) {
    for container in navigationBar.subviews where
        container.subviews.contains(where: { $0 is UIRefreshControl }) {
        container.isUserInteractionEnabled = false
    }
}

private struct TitleAccessoryLifecycle: UIViewControllerRepresentable {
    let onAppear: () -> Void
    let onWillDisappear: () -> Void
    let onDisappear: () -> Void

    func makeUIViewController(context: Context) -> Observer {
        Observer()
    }

    func updateUIViewController(_ controller: Observer, context: Context) {
        controller.onAppear = onAppear
        controller.onWillDisappear = onWillDisappear
        controller.onDisappear = onDisappear
    }

    final class Observer: UIViewController {
        var onAppear: (() -> Void)?
        var onWillDisappear: (() -> Void)?
        var onDisappear: (() -> Void)?

        override func loadView() {
            let view = UIView()
            view.isUserInteractionEnabled = false
            self.view = view
        }

        override func viewDidAppear(_ animated: Bool) {
            super.viewDidAppear(animated)
            onAppear?()
        }

        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            onWillDisappear?()
        }

        override func viewDidDisappear(_ animated: Bool) {
            super.viewDidDisappear(animated)
            onDisappear?()
        }
    }
}

private struct TitleAccessoryOwner: ViewModifier {
    let installation: TitleAccessoryInstallation
    let attach: (UIViewController) -> Void

    func body(content: Content) -> some View {
        content
            .introspect(
                .viewController,
                on: .iOS(.v16), .iOS(.v17), .iOS(.v18),
                    .iOS(.v26), .iOS(.v27)
            ) { controller in
                installation.controller = controller
                attach(controller)
            }
            .background {
                TitleAccessoryLifecycle {
                    guard let controller = installation.controller else { return }
                    attach(controller)
                } onWillDisappear: {
                    if #available(iOS 26.0, *) {
                        installation.removeInline()
                    }
                } onDisappear: {
                    if let bar = installation.navigationBar {
                        deactivateTitleInstallation(installation, in: bar)
                    } else {
                        installation.remove()
                    }
                }
                .frame(width: 0, height: 0)
            }
    }
}



// MODERN IMPLEMENTATION

@MainActor
private func titleWidth(in bar: UINavigationBar) -> CGFloat? {
    guard let content = bar.subviews.first(where: { titleClass($0, contains: "NavigationBarContentView") }),
          let container = content.subviews.first,
          let titleContainer = container.subviews.first,
          let buttonContainer = container.subviews.last,
          let title = titleContainer.subviews.first else { return nil }

    let midpoint = title.convert(title.bounds, to: container).midX
    let frames = buttonContainer.subviews.map { $0.convert($0.bounds, to: container) }
    let left = frames.filter { $0.maxX <= midpoint }.map(\.maxX).max() ?? container.bounds.minX
    let right = frames.filter { $0.minX >= midpoint }.map(\.minX).min() ?? container.bounds.maxX
    let width = right - left - 26
    return width > 0 ? width : nil
}

private enum TitleWrapping {
    static func width(_ text: String, font: UIFont) -> CGFloat {
        (text as NSString).size(withAttributes: [.font: font]).width
    }

    static func split(_ text: String, font: UIFont, maxWidth: CGFloat) -> (String, String) {
        if width(text, font: font) <= maxWidth { return (text, "") }
        let words = text.split(separator: " ")
        guard words.count > 1 else { return (text, "") }

        var best: (String, String, CGFloat)?
        for i in 1..<words.count {
            let first = words[..<i].joined(separator: " ")
            let second = words[i...].joined(separator: " ")
            let a = width(first, font: font)
            let b = width(second, font: font)
            guard a <= maxWidth, b <= maxWidth else { continue }
            let difference = abs(a - b)
            if best == nil || difference < best!.2 { best = (first, second, difference) }
        }
        if let best { return (best.0, best.1) }

        var first = ""
        for word in words {
            let candidate = first.isEmpty ? String(word) : first + " " + word
            if width(candidate, font: font) > maxWidth { break }
            first = candidate
        }
        guard !first.isEmpty else { return (text, "") }
        let rest = text.dropFirst(first.count).trimmingCharacters(in: .whitespaces)
        return (first, rest)
    }
}

@available(iOS 26.0, *)
private struct TitleLinesLayer: ViewModifier {
    let title: String
    let enabled: Bool
    @Binding var firstLine: String
    @Binding var secondLine: String

    func body(content: Content) -> some View {
        content
            .navigationTitle(firstLine)
            .toolbar {
                if enabled && !secondLine.isEmpty {
                    ToolbarItem(placement: .subtitle) {
                        Text(secondLine).font(.subheadline.weight(.semibold))
                    }
                }
            }
            .introspect(.viewController, on: .iOS(.v26), .iOS(.v27)) { controller in
                guard enabled, let navigation = activeTitleNavigation(for: controller),
                      let width = titleWidth(in: navigation.navigationBar) else { return }
                let preferred = UIFont.preferredFont(forTextStyle: .subheadline)
                let font = UIFont.systemFont(ofSize: preferred.pointSize, weight: .semibold)
                let result = TitleWrapping.split(title, font: font, maxWidth: width)
                guard result.0 != firstLine || result.1 != secondLine else { return }
                DispatchQueue.main.async { [weak controller] in
                    guard let controller, activeTitleNavigation(for: controller) != nil else { return }
                    if firstLine != result.0 { firstLine = result.0 }
                    if secondLine != result.1 { secondLine = result.1 }
                }
            }
    }
}


@available(iOS 26.0, *)
private struct EnhancedTitlePopObserver: UIViewControllerRepresentable {
    @Binding var isPopping: Bool
    func makeUIViewController(context: Context) -> Observer { Observer(isPopping: $isPopping) }
    func updateUIViewController(_ controller: Observer, context: Context) {
        controller.isPopping = $isPopping
    }

    final class Observer: UIViewController {
        var isPopping: Binding<Bool>
        init(isPopping: Binding<Bool>) {
            self.isPopping = isPopping
            super.init(nibName: nil, bundle: nil)
            view.isHidden = true
            view.isUserInteractionEnabled = false
        }
        @available(*, unavailable) required init?(coder: NSCoder) { fatalError() }
        override func viewWillDisappear(_ animated: Bool) {
            super.viewWillDisappear(animated)
            guard let coordinator = transitionCoordinator else { return }
            isPopping.wrappedValue = true
            coordinator.animate(alongsideTransition: nil) { [weak self] context in
                if context.isCancelled { self?.isPopping.wrappedValue = false }
            }
        }
    }
}

@available(iOS 26.0, *)
private struct CustomLargeTitleLayer<LargeTitle: View, Accessory: View>: ViewModifier {
    let largeTitle: (() -> LargeTitle)?
    let accessory: ((Bool) -> Accessory)?
    @State private var isPopping = false

    func body(content: Content) -> some View {
        content
            .toolbar {
                if let largeTitle {
                    ToolbarItem(placement: .largeTitle) {
                        VStack {
                            if #unavailable(iOS 26.2) { Spacer().frame(height: 24) }
                            HStack(spacing: 8) {
                                largeTitle()
                                if let accessory { accessory(true) }
                            }
                            .opacity(isPopping ? 0 : 1)
                        }
                    }
                }
            }
            .background {
                if largeTitle != nil {
                    EnhancedTitlePopObserver(isPopping: $isPopping)
                        .frame(width: 0, height: 0)
                }
            }
            .onAppear { isPopping = false }
    }
}


@available(iOS 26.0, *)
private struct TitleAccessoryLayer<Accessory: View>: ViewModifier {
    let accessory: ((Bool) -> Accessory)?
    let customLargeTitle: Bool
    let secondLine: String
    @State private var installation = TitleAccessoryInstallation()

    func body(content: Content) -> some View {
        content
            .modifier(TitleAccessoryOwner(
                installation: installation,
                attach: scheduleAttachment
            ))
            .onChange(of: secondLine) { _, _ in
                guard let controller = installation.controller else { return }
                scheduleAttachment(for: controller)
            }
    }

    private func scheduleAttachment(for controller: UIViewController) {
        installation.scheduleAttachment(for: controller) { navigation, bar in
            refreshTitleHitTesting(in: bar)

            guard let accessory else {
                installation.remove()
                return
            }

            let usesLargeTitle =
                bar.prefersLargeTitles &&
                navigation.topViewController?.navigationItem.largeTitleDisplayMode != .never

            if !customLargeTitle,
               usesLargeTitle,
               let root = navigation.view,
               let container = findSubview(in: root, where: { subview in
                   titleClass(subview, contains: "NavigationBarLargeTitleView")
                       && subview.subviews.contains { $0 is UILabel }
               }),
               let label = container.subviews.first(where: { $0 is UILabel }) as? UILabel {
                
                installation.attachLarge(
                    AnyView(accessory(true)),
                    to: container,
                    label: label
                )
            }

            guard let content = bar.subviews.first(where: { titleClass($0, contains: "NavigationBarContentView") }),
                  let transition = content.subviews.first(where: { titleClass($0, contains: "NavigationBarTransitionContainer") }),
                  let container = transition.subviews.first(where: { titleClass($0, contains: "HostedViewContainer") }),
                  let titleWrapper = container.subviews.first(where: { wrapper in
                      wrapper.subviews.contains { titleClass($0, contains: "UINavigationBarTitleControl") }
                  }),
                  let titleControl = titleWrapper.subviews.first(where: {
                      titleClass($0, contains: "UINavigationBarTitleControl")
                  }),
                  let label = findSubview(
                    in: titleControl, ofType: UILabel.self
                  ) else { return }
            
            installation.attachInline(
                AnyView(accessory(false).fixedSize()),
                titleControl: titleControl,
                label: label,
                visualParent: label,
                interactiveParent: container,
                visibilitySource: titleWrapper
            )
        }
    }
}

@available(iOS 26.0, *)
private struct EnhancedTitleModifier<Accessory: View, LargeTitle: View>: ViewModifier {
    let title: String
    let twoLines: Bool
    let accessory: ((Bool) -> Accessory)?
    let largeTitle: (() -> LargeTitle)?
    @State private var firstLine: String
    @State private var secondLine = ""

    init(title: String, twoLines: Bool,
         accessory: ((Bool) -> Accessory)?, largeTitle: (() -> LargeTitle)?) {
        self.title = title
        self.twoLines = twoLines
        self.accessory = accessory
        self.largeTitle = largeTitle
        _firstLine = State(initialValue: title)
    }

    func body(content: Content) -> some View {
        content
            .modifier(TitleLinesLayer(title: title, enabled: twoLines, firstLine: $firstLine, secondLine: $secondLine))
            .modifier(CustomLargeTitleLayer(largeTitle: largeTitle, accessory: accessory))
            .modifier(TitleAccessoryLayer(accessory: accessory, customLargeTitle: largeTitle != nil, secondLine: secondLine))
            .onChange(of: title) { _, newTitle in
                firstLine = newTitle
                secondLine = ""
            }
    }
}


// LEGACY FALLBACKS (iOS 16-18)

private struct LegacyTitleAccessoryLayer<Accessory: View>: ViewModifier {
    let accessory: (Bool) -> Accessory
    let hasCustomLargeTitle: Bool
    
    @State private var installation = TitleAccessoryInstallation()

    func body(content: Content) -> some View {
        content.modifier(
            TitleAccessoryOwner(
                installation: installation,
                attach: scheduleAttachment
            )
        )
    }
    
    private func scheduleAttachment(for controller: UIViewController) {
        installation.scheduleAttachment(for: controller) { navigation, bar in
            let usesLargeTitle =
            navigation.navigationBar.prefersLargeTitles &&
            navigation.topViewController?.navigationItem.largeTitleDisplayMode != .never
            
            // On legacy systems the large title lives inside the navigation bar.
            // A custom header already composes its own large accessory.
            if !hasCustomLargeTitle,
               usesLargeTitle,
               let large = bar.subviews.first(where: {
                   titleClass($0, contains: "NavigationBarLargeTitleView")
               }),
               let label = large.subviews.first(where: {
                   $0 is UILabel
               }) as? UILabel {
                
                installation.attachLarge(
                    AnyView(accessory(true)),
                    to: large,
                    label: label
                )
            }
            
            guard let content = bar.subviews.first(where: {
                    titleClass($0, contains: "NavigationBarContentView")
                }),
                let titleControl = findSubview(in: content, where: {
                    titleClass($0, contains: "UINavigationBarTitleControl")
                }),
                let label = findSubview(in: titleControl, ofType: UILabel.self),
                let visibilityWrapper = label.superview else {
                    return
                }
            
            installation.attachInline(
                AnyView(accessory(false).fixedSize()),
                titleControl: titleControl,
                label: label,
                visualParent: label,
                interactiveParent: content,
                visibilitySource: visibilityWrapper
            )
        }
    }
}


@available(iOS, introduced: 16.0, obsoleted: 26.0)
private struct LegacyLargeTitleModifier<Header: View>: ViewModifier {
    let title: String
    let backgroundColor: Color
    let header: () -> Header

    @State private var headerHeight: CGFloat = 0
    @State private var scrollOffset: CGFloat = 0
    @State private var showInlineTitle = false
    @State private var snapDelegate: LegacyHeaderSnapDelegate?

    func body(content: Content) -> some View {
        Group {
            if #available(iOS 17.0, *) {
                ScrollView {
                    VStack(spacing: 0) {
                        headerView(bottomPadding: 12)
                        content
                    }
                    .scrollTargetLayout()
                }
                .scrollTargetBehavior(LegacyHeaderSnapBehavior(headerHeight: headerHeight))
                .modifier(legacyNavigationStyle)
            } else {
                ScrollView {
                    VStack(spacing: 8) {
                        headerView(bottomPadding: 0)
                        content
                    }
                }
                .introspect(.scrollView, on: .iOS(.v16)) { scrollView in
                    if snapDelegate == nil {
                        snapDelegate = LegacyHeaderSnapDelegate(headerHeight: headerHeight)
                    }
                    snapDelegate?.headerHeight = headerHeight
                    if scrollView.delegate !== snapDelegate {
                        scrollView.delegate = snapDelegate
                    }
                }
                .modifier(legacyNavigationStyle)
            }
        }
        .introspect(.viewController, on: .iOS(.v16), .iOS(.v17), .iOS(.v18)) { controller in
            guard let bar = controller.navigationController?.navigationBar else { return }
            if let background = bar.subviews.first(where: {
                String(describing: type(of: $0)).contains("_UIBarBackground")
            }) {
                background.alpha = min(max(-(scrollOffset + headerHeight) / 5, 0), 1)
            }
            if let content = bar.subviews.first(where: {
                String(describing: type(of: $0)).contains("UINavigationBarContentView")
            }), let titleControl = findSubview(in: content, where: {
                titleClass($0, contains: "UINavigationBarTitleControl")
            }) {
                UIView.animate(withDuration: 0.175) {
                    titleControl.alpha = showInlineTitle ? 1 : 0
                    if #unavailable(iOS 17.0), let label = titleControl as? UILabel {
                        label.textColor = showInlineTitle ? .label : .clear
                    }
                }
            }
        }
    }

    private var legacyNavigationStyle: some ViewModifier {
        LegacyNavigationStyle(title: title, backgroundColor: backgroundColor)
    }

    private func headerView(bottomPadding: CGFloat) -> some View {
        header()
            .padding(.bottom, bottomPadding)
            .onGeometryChange(for: CGFloat.self) { $0.size.height } action: { height in
                headerHeight = height
            }
            .onGeometryChange(for: CGFloat.self) { $0.frame(in: .named("legacyTitleScroll")).minY } action: { y in
                scrollOffset = y
                showInlineTitle = headerHeight > 0
                    && y + headerHeight - 12 <= 0
            }
            .mask {
                VStack {
                    Spacer()
                    Rectangle().frame(height: max(0, headerHeight + scrollOffset))
                }
            }
    }
}

@available(iOS, introduced: 16.0, obsoleted: 26.0)
private struct LegacyNavigationStyle: ViewModifier {
    let title: String
    let backgroundColor: Color
    func body(content: Content) -> some View {
        content
            .background(backgroundColor)
            .coordinateSpace(name: "legacyTitleScroll")
            .navigationBarTitleDisplayMode(.inline)
            .navigationTitle(title)
    }
}

@available(iOS 17.0, *)
private struct LegacyHeaderSnapBehavior: ScrollTargetBehavior {
    let headerHeight: CGFloat
    func updateTarget(_ target: inout ScrollTarget, context: TargetContext) {
        guard target.rect.minY < headerHeight else { return }
        target.rect.origin.y = target.rect.minY < headerHeight / 2 - 12 ? 0 : headerHeight
    }
}

private final class LegacyHeaderSnapDelegate: NSObject, UIScrollViewDelegate {
    var headerHeight: CGFloat
    init(headerHeight: CGFloat) { self.headerHeight = headerHeight }
    func scrollViewWillEndDragging(
        _ scrollView: UIScrollView,
        withVelocity velocity: CGPoint,
        targetContentOffset: UnsafeMutablePointer<CGPoint>
    ) {
        let proposed = targetContentOffset.pointee.y + scrollView.adjustedContentInset.top
        guard proposed < headerHeight else { return }
        targetContentOffset.pointee.y =
            (proposed < headerHeight / 2 ? 0 : headerHeight) - scrollView.adjustedContentInset.top
    }
}


// MODIFIERS

extension View {
    // by default, String literals should be localized, however string vars need
    // to be supported as well -> @_disfavoredOverload
    
    @ViewBuilder
    func navigationTitleEnhanced<Accessory: View, LargeTitle: View>(
        _ title: LocalizedStringResource,
        twoLines: Bool = false,
        backgroundColor: Color = Color(.systemGroupedBackground),
        @ViewBuilder accessory: @escaping (Bool) -> Accessory,
        @ViewBuilder largeTitle: @escaping () -> LargeTitle
    ) -> some View {
        navigationTitleEnhanced(String(localized: title), twoLines: twoLines, backgroundColor: backgroundColor, accessory: accessory, largeTitle: largeTitle)
    }
    
    @_disfavoredOverload
    @ViewBuilder
    func navigationTitleEnhanced<Accessory: View, LargeTitle: View>(
        _ title: String,
        twoLines: Bool = false,
        backgroundColor: Color = Color(.systemGroupedBackground),
        @ViewBuilder accessory: @escaping (Bool) -> Accessory,
        @ViewBuilder largeTitle: @escaping () -> LargeTitle
    ) -> some View {
        if #available(iOS 26.0, *) {
            modifier(EnhancedTitleModifier(title: title, twoLines: twoLines, accessory: accessory, largeTitle: largeTitle))
        } else {
            modifier(LegacyLargeTitleModifier(title: title, backgroundColor: backgroundColor, header: {
                HStack(spacing: 8) {
                    largeTitle()
                    accessory(true)
                }
            }))
            .modifier(LegacyTitleAccessoryLayer(accessory: accessory, hasCustomLargeTitle: true))
        }
    }

    @ViewBuilder
    func navigationTitleEnhanced<Accessory: View>(
        _ title: LocalizedStringResource,
        twoLines: Bool = false,
        @ViewBuilder accessory: @escaping (Bool) -> Accessory
    ) -> some View {
        navigationTitleEnhanced(String(localized: title), twoLines: twoLines, accessory: accessory)
    }

    @_disfavoredOverload
    @ViewBuilder
    func navigationTitleEnhanced<Accessory: View>(
        _ title: String,
        twoLines: Bool = false,
        @ViewBuilder accessory: @escaping (Bool) -> Accessory
    ) -> some View {
        if #available(iOS 26.0, *) {
            modifier(EnhancedTitleModifier<Accessory, EmptyView>(title: title, twoLines: twoLines, accessory: accessory, largeTitle: nil))
        } else {
            navigationTitle(title)
                .modifier(LegacyTitleAccessoryLayer(accessory: accessory, hasCustomLargeTitle: false))
        }
    }

    @ViewBuilder
    func navigationTitleEnhanced<LargeTitle: View>(
        _ title: LocalizedStringResource,
        twoLines: Bool = false,
        backgroundColor: Color = Color(.systemGroupedBackground),
        @ViewBuilder largeTitle: @escaping () -> LargeTitle
    ) -> some View {
        navigationTitleEnhanced(String(localized: title), twoLines: twoLines, backgroundColor: backgroundColor, largeTitle: largeTitle)
    }

    @_disfavoredOverload
    @ViewBuilder
    func navigationTitleEnhanced<LargeTitle: View>(
        _ title: String,
        twoLines: Bool = false,
        backgroundColor: Color = Color(.systemGroupedBackground),
        @ViewBuilder largeTitle: @escaping () -> LargeTitle
    ) -> some View {
        if #available(iOS 26.0, *) {
            modifier(EnhancedTitleModifier<EmptyView, LargeTitle>(title: title, twoLines: twoLines, accessory: nil, largeTitle: largeTitle))
        } else {
            modifier(LegacyLargeTitleModifier(title: title, backgroundColor: backgroundColor, header: largeTitle))
        }
    }

    @ViewBuilder
    func navigationTitleEnhanced(
        _ title: LocalizedStringResource,
        twoLines: Bool = false
    ) -> some View {
        navigationTitleEnhanced(String(localized: title), twoLines: twoLines)
    }

    @_disfavoredOverload
    @ViewBuilder
    func navigationTitleEnhanced(
        _ title: String,
        twoLines: Bool = false) -> some View {
        if #available(iOS 26.0, *) {
            modifier(EnhancedTitleModifier<EmptyView, EmptyView>(title: title, twoLines: twoLines, accessory: nil, largeTitle: nil))
        } else {
            navigationTitle(title)
        }
    }
}


// PREVIEW

private struct NavigationTitlePreview: View {
    @State private var taps = 0
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 18) {
                    Text("Accessory taps: \(taps)")
                    ForEach(0..<50, id: \.self) { Text("Row \($0)") }
                }.frame(maxWidth: .infinity)
            }
            .refreshable {}
            .navigationTitleEnhanced(
                "Software",
                twoLines: true,
                largeTitle: {
                    Text("Test")
                }
            )
        }
    }
}

#Preview {
    NavigationTitlePreview()
}


private struct ScreenA: View {
    @State private var count = 0
    
    var body: some View {
        NavigationStack {
            NavigationLink("Open B") {
                ScrollView {
                    Text("Screen B")
                    Text("count is \(count)")
                    Rectangle()
                        .frame(height: 1000)
                }
                .navigationTitleEnhanced("Screen B") { _ in
                    Button(action: {
                        count += 1
                    }, label: {
                        Image(systemName: "star.fill")
                    })
                }
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .primaryAction) {
                        Text("Verylongnavi")
                    }
                }
            }
            .navigationTitle("Screen A")
            .navigationBarTitleDisplayMode(.inline)
            
            Text("count is \(count)")
        }
    }
}

#Preview("NavigationCheck") {
    ScreenA()
}


#Preview("Two lines") {
    NavigationStack {
        VStack {
            Text("Test")
        }
        .navigationTitleEnhanced("Software-Engineering - Wartung und Qualitätssicherung", twoLines: true, accessory: { isLarge in
            Image(systemSymbol: .star)
        })
        .navigationBarTitleDisplayMode(.inline)
    }
}
