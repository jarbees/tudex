//  Created on 05.06.25.

import SwiftUI

// I do not know why, but using too many dynamic LazyVGrids inside makes the refreshable animation unstable.
// This is default behaviour. The only difference to .refreshable is that this handles the case where
// an .automatic NavigationTitle is exactly at the position for .inline where normally, it would pop out over the content
struct RefreshableScrollView<Content: View>: View {
    let isEnabled: Bool
    let action: () async -> Void
    let content: () -> Content
    
    init(isEnabled: Bool = true, action: @escaping () async -> Void, content: @escaping () -> Content) {
        self.isEnabled = isEnabled
        self.action = action
        self.content = content
    }

    @State private var isScrolling: Bool = false
    @State private var isAtTopWithInlineTitle: Bool = true
    
    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                GeometryReader { geometry in
                    Color.clear
                        .onChange(of: geometry.frame(in: .named("ContentCoords")).minY) { position in
                            if (abs(position) < 1e-10 && position != 0) {        // For some reason, the inlineNavigationTitle makes ScrollView sit at ~1.42e-14
                                if (isAtTopWithInlineTitle == false) {
                                    isAtTopWithInlineTitle = true
                                }
                            }
                            else if (isAtTopWithInlineTitle == true) {
                                isAtTopWithInlineTitle = false
                            }
                        }
                }
                .frame(height: 0)
                .id(0)

                content()
            }
            .coordinateSpace(name: "ContentCoords")
            .modify { view in
                if isEnabled {
                    view.refreshable {
                            await action()
                            Task {
                                if (isAtTopWithInlineTitle && !isScrolling) {
                                    withAnimation {
                                        proxy.scrollTo(0, anchor: .top)
                                    }
                                }
                            }
                        }
                }
            }
        }
        // I am unsure of what the purpose of this is/was, however it breaks the scrolling in iOS 16
        /*
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
            .onChanged { _ in
                if !isScrolling {
                    isScrolling = true
                }
            }
            .onEnded { _ in
                isScrolling = false
            }
        )
        */
    }
}

/*
struct RefreshableScrollView<Content: View>: View {
    let action: () async -> Void
    let content: () -> Content
    /*
    let disableCorrectiveScroll: Bool = false
    
    @State private var isScrolling: Bool = false
    @State private var isOverscrolled: Bool = false
    @State private var isAtTop: Bool = true
    @State private var navigationTitleHeight: CGFloat = 0*/
    
    @State private var isShown: Bool = false
    
    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {/*
                GeometryReader { geometry in
                    Color.clear
                        .onAppear {
                            Task {
                                navigationTitleHeight = geometry.frame(in: .global).minY
                            }
                        }
                        .onChange(of: geometry.frame(in: .global).minY) { newValue in
                            let contentPos = geometry.frame(in: .named("ContentCoords")).minY
                            print(newValue)
                            print(contentPos)
                            if (newValue > navigationTitleHeight) {
                                if (isOverscrolled == false) {
                                    isOverscrolled = true
                                }
                            }
                            else if (isOverscrolled == true) {
                                isOverscrolled = false
                            }
                            
                            if (abs(contentPos) < 1e-10) {        // For some reason, the inlineNavigationTitle makes ScrollView sit at ~1.42e-14
                                if (isAtTop == false) {
                                    isAtTop = true
                                }
                            }
                            else if (isAtTop == true) {
                                isAtTop = false
                            }
                            
                            print(isAtTop)
                            print(isOverscrolled)
                        }
                }
                .frame(height: 0)
                //.id(0)
                */
                /*if (isShown) {
                    Spacer()
                        .frame(height: 0)
                }*/
                content()
                    /*.blockPullToRefresh()
                    .id(0)*/
            }
            /*.coordinateSpace(name: "ContentCoords")*/
            .refreshable {
                await action()
                /*withAnimation {
                    proxy.scrollTo(0, anchor: .top)
                }*/
                Task {
                    print("isScrolling: \(isScrolling), isOverscrolled: \(isOverscrolled), isAtTop: \(isAtTop)")
                    /*if (!isScrolling && isOverscrolled) {
                        proxy.scrollTo(0, anchor: .top)
                    } else if (isAtTop) {
                        withAnimation {
                            proxy.scrollTo(0, anchor: .top)
                        }
                    }*/
                }
            }
        }/*
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
            .onChanged { _ in
                if !isScrolling {
                    isScrolling = true
                }
            }
            .onEnded { _ in
                isScrolling = false
            }
        )*/
    }
}
*/
/*
struct RefreshableScrollView<Content: View>: View {
    let action: () async -> Void
    let content: () -> Content
    
    @State private var isScrolling: Bool = false
    
    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                content()
                    .id(0)
            }
            .refreshable {
                await action()
                Task {
                    if (!isScrolling) {
                        proxy.scrollTo(0, anchor: .top)
                    }
                }
            }
        }
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
            .onChanged { _ in
                if (!isScrolling) {
                    isScrolling = true
                }
                
            }
            .onEnded { _ in
                isScrolling = false
            }
        )
    }
}
*/
