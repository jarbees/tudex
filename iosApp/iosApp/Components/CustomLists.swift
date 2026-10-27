//  Created on 12.05.25.

import SwiftUI
import Shared
import SFSafeSymbols

extension EnvironmentValues {
    @Entry var listVerticalPadding: CGFloat = 0
    @Entry var listLeadingPadding: CGFloat = 0
}

private let defaultPadding = if #available(iOS 26.0, *) {
    15.65
} else {
    11.7
}

struct GroupedList<Content: View>: View {
    @ViewBuilder let content: () -> Content
    let padding: CGFloat
    let style: GroupedListStyle
    
    @Environment(\.listVerticalPadding) private var verticalPadding
    @Environment(\.listLeadingPadding) private var leadingPadding
    
    init(padding: CGFloat? = nil, style: GroupedListStyle? = nil, @ViewBuilder content: @escaping () -> Content) {
        self.content = content
        self.padding = padding ?? defaultPadding
        self.style = style ?? .grouped
    }
    
    var body: some View {
        Group {
            if #available(iOS 18.0, *) {
                ModernGroupedList(content: content, padding: padding, style: style)
            } else {
                LegacyGroupedList(content: content, padding: padding, style: style)
            }
        }
        .environment(\.listVerticalPadding, padding)
        .environment(\.listLeadingPadding, leadingPadding + 16)
    }
}

@available(iOS 18.0, *)
private struct ModernGroupedList<Content: View>: View {
    @ViewBuilder let content: () -> Content
    let padding: CGFloat
    let style: GroupedListStyle

    var body: some View {
        Group(subviews: content()) { subviews in
            VStack(alignment: .leading, spacing: 0) {
                ForEach(subviews) { subview in
                    subview
                        .padding(.vertical, padding)
                    
                    if subview.id != subviews.last?.id {
                        Divider()
                            .dividerListPadding()
                    }
                }
            }
            .modifier(ListStyleModifier(style: style))
        }
    }
}

private struct LegacyGroupedList<Content: View>: View {
    @ViewBuilder let content: () -> Content
    let padding: CGFloat
    let style: GroupedListStyle

    var body: some View {
        _VariadicView.Tree(LegacyGroupedListLayout(padding: padding, style: style)) {
            content()
        }
    }
}

private struct LegacyGroupedListLayout: _VariadicView_MultiViewRoot {
    let padding: CGFloat
    let style: GroupedListStyle
    
    @ViewBuilder
    func body(children: _VariadicView.Children) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            ForEach(children) { child in
                child
                    .padding(.vertical, padding)
                
                if child.id != children.last?.id {
                    Divider()
                        .dividerListPadding()
                }
            }
        }
        .modifier(ListStyleModifier(style: style))
    }
}

enum GroupedListStyle {
    case grouped
    case plain
}

private struct ListStyleModifier: ViewModifier {
    let style: GroupedListStyle

    @ViewBuilder
    func body(content: Content) -> some View {
        switch style {
        case .grouped:
            content
                .padding(.leading)
                .background(Color(.secondarySystemGroupedBackground))
                .rounded()

        case .plain:
            content
                .padding(.leading)
        }
    }
}


#Preview("Legacy List") {
    NavigationStack {
        VStack {
            LegacyGroupedList(content: {
                Text("Testing")
                Text("Testing")
                Text("Testing")
                
                if false {
                    Text("false")
                }
                
                if true {
                    Text("Yay")
                }
                
                ForEach(1..<3) { index in
                    Text("Number")
                }
            }, padding: defaultPadding, style: .grouped)
        }
        .padding(.top, 19)
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Color(.systemGroupedBackground))
    }
}

@available(iOS 18.0, *)
#Preview("New List") {
    NavigationStack {
        VStack {
            ModernGroupedList(content: {
                Text("Testing")
                Text("Testing")
                Text("Testing")
                
                if false {
                    Text("false")
                }
                
                if true {
                    Text("Yay")
                }
                
                ForEach(1..<3) { index in
                    Text("Number")
                }
            }, padding: defaultPadding, style: .grouped)
        }
        .padding(.top, 19)
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Color(.systemGroupedBackground))
    }
}

#Preview("Reference List") {
    NavigationStack {
        List {
            VStack {
                Text("Testing")
                Text("Testing")
                Text("Testing")
            }
            Text("Testing")
            Text("Testing")
            
            if false {
                Text("false")
            }
            
            if true {
                Text("Yay")
            }
            
            ForEach(1..<3) { index in
                Text("Number")
            }
        }
        .background(Color(.systemGroupedBackground))
    }
}



enum GroupedDisclosureStyle {
    case standard
    case dividerAligned
}

struct GroupDisclosureGroup<Header: View, Content: View>: View {
    private let header: () -> Header
    private let content: () -> Content
    private let isDefaultHeader: Bool
    private let padding: CGFloat
    private let style: GroupedDisclosureStyle
    
    @State private var isExpanded = false
    
    init(
        padding: CGFloat? = nil,
        style: GroupedDisclosureStyle? = nil,
        @ViewBuilder header: @escaping () -> Header,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.header = header
        self.content = content
        self.padding = padding ?? defaultPadding
        self.style = style ?? .standard
        self.isDefaultHeader = false
    }
    
    init(
        _ title: String,
        padding: CGFloat? = nil,
        style: GroupedDisclosureStyle? = nil,
        @ViewBuilder content: @escaping () -> Content)
        where Header == Text
    {
        self.header = { Text(title) }
        self.content = content
        self.padding = padding ?? defaultPadding
        self.style = style ?? .standard
        self.isDefaultHeader = true
    }
    
    var body: some View {
        VStack(spacing: 0) {
            HStack {
                if isDefaultHeader {
                    header()
                        .lineLimit(1)
                        .padding(.vertical, padding)
                } else {
                    header()
                        .padding(.vertical, padding)
                }
                Spacer()
                Image(systemSymbol: .chevronRight)
                    .foregroundStyle(Color.accentColor)
                    .font(.system(size: 14, weight: .semibold))
                    .rotationEffect(.degrees(isExpanded ? 90 : 0))
                    .padding(.trailing)
            }
            .padding(.horizontal, 16)
            .background(Color(.secondarySystemGroupedBackground))
            .zIndex(1)
            .onTapGesture {
                withAnimation(Animation.default.speed(1.4)) {
                    isExpanded.toggle()
                }
            }
            
            if isExpanded {
                Divider()
                    .dividerListPadding()
                    .padding(.horizontal, 16)
                    .zIndex(1)
                GroupedList(padding: padding, style: .plain) {
                    content()
                        .modify { view in
                            if style == .dividerAligned {
                                view.padding(.leading)
                            }
                        }
                }
                .modify { view in
                    if style == .dividerAligned {
                        view.padding(.leading, -16)
                    }
                }
                .padding(.horizontal, 16)
                .transition(.move(edge: .top))
            }
        }
        .mask {     // Effectively a vertical clip
            Rectangle()
                .padding(.horizontal, -10_000)
        }
        .padding(.vertical, -padding)
        .padding(.horizontal, -16)
    }
}

#Preview("Disclosure") {
    VStack {
        GroupedList {
            GroupDisclosureGroup("Testing") {
                Text("Test1")
                Text("Test1")
                Text("Test1")
            }
        }
    }
    .frame(maxHeight: .infinity, alignment: .top)
    .background(Color(.systemGroupedBackground))
}

#Preview("Course View") {
    NavigationStack {
        CourseView()
            .environmentObject(TucanService())
            .environmentObject(IdentityService(previewMode: true))
    }
}


#Preview("Nav Link Test") {
    NavigationStack {
        VStack {
            Text("Nothing")
            NavigationLinkButton {
                Text("Navigate")
            } destination: {
                DemoView()
            }
            
            GroupedList {
                NavigationLinkButton {
                    Text("Navigate")
                        .frame(maxWidth: .infinity, alignment: .leading)
                } destination: {
                    DemoView()
                }
            }
            
            GroupedList {
                GroupDisclosureGroup("Testing") {
                    NavigationLinkButton {
                        Text("Navigate")
                            .frame(maxWidth: .infinity, alignment: .leading)
                    } destination: {
                        DemoView()
                    }
                }
            }
            
            GroupedList {
                GroupDisclosureGroup("Testing") {
                    NavigationLinkButton {
                        Text("Navigate")
                            .frame(maxWidth: .infinity, alignment: .leading)
                    } destination: {
                        DemoView()
                    }
                    NavigationLinkButton {
                        Text("Navigate")
                            .frame(maxWidth: .infinity, alignment: .leading)
                    } destination: {
                        DemoView()
                    }
                    NavigationLinkButton {
                        Text("Navigate")
                            .frame(maxWidth: .infinity, alignment: .leading)
                    } destination: {
                        DemoView()
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
        .background(Color(.systemGroupedBackground))
    }
}


