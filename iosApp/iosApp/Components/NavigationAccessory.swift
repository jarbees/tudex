//  Created on 07.01.26.

import SwiftUI
import SwiftUIIntrospect

extension View {
    /// Add an accessoryView in small and large next to the navigationTitle. Doesn't support interaction. It requires use of navigationTitleAccessoryCleanup() for views above and below in the Stack. It is also extremely brittle. A single modifier can break everything.
    func navigationTitleAccessory(
        small smallView: @escaping () -> some View,
        large largeView: @escaping () -> some View
    ) -> some View {
        // A brief overview over this UIKit monstrocity.
        // Problem: I want to display an indicator next to the NavigationTitle in such a way that it animates as a singular unit.
        // Hypothesis: There is no reasonable way of achieving this cleanly without using Introspect.
        
        // In iOS 16-18, the navigationTitle works in a relatively understandable way. A few UIViews are nested and somewhere exists an animated large and small label.
        // For both of these labels, an accessoryView is added with a tag to remember that it is a custom accessory.
        // By default, parents would give their accessories to their children, which is why all accessories are removed on each call of introspect and then redrawn.
        // Only the currently shown view is active in that regard.
        
        // For iOS 26, Apple changed how navigationTitles are displayed. The large Title now instead is part of the Content Hierarchy and not clearly seperated into navigationBar like before.
        // This is why it is searched for with findLargeTitleContainer() instead.
        // Also placing the accessorySmall as a subview for UILabel breaks its display, which is why it is instead placed in the parent.
        
        // Tested in Simulator for iOS 16-26, see each version for details
        // iOS 16/17: works well, but animation for largeAccessory is broken
        // iOS 18: works well, except when navigating back to collapsed title (hacky fix)
        // iOS 26: works as expected
        
        self
            .introspect(.viewController, on: .iOS(.v16), .iOS(.v17)) { viewController in
                guard let navController = viewController.navigationController else { return }
                guard navController.topViewController == viewController else { return }
                
                let navBar = navController.navigationBar
                let accessoryLarge = UIHostingController(rootView: largeView())
                let accessorySmall = UIHostingController(rootView: smallView())
                
                let accessoryTag = 9999
                accessorySmall.view.tag = accessoryTag
                accessoryLarge.view.tag = accessoryTag
                
                
                // Add accessory to large title
                
                if let largeLabelContainer = navBar.subviews.first(where: {
                    String(describing: type(of: $0)).contains("NavigationBarLargeTitleView")
                }) {
                    if let titleLabel = largeLabelContainer.subviews.first {
                        titleLabel.subviews
                            .filter { $0.tag == accessoryTag }
                            .forEach { $0.removeFromSuperview() }
                        titleLabel.addSubview(accessoryLarge.view)
                        accessoryLarge.view.backgroundColor = .clear
                        accessoryLarge.view.translatesAutoresizingMaskIntoConstraints = false
                        
                        NSLayoutConstraint.activate([
                            accessoryLarge.view.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
                            accessoryLarge.view.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
                        ])
                    }
                }
                
                // Add accessory to small title
                
                if let contentContainer = navBar.subviews.first(where: {
                    String(describing: type(of: $0)).contains("NavigationBarContentView")
                }) {
                    if let smallLabelContainer = contentContainer.subviews.first(where: {
                        String(describing: type(of: $0)).contains("_UINavigationBarTitleControl")
                    }) {
                        //if let smallLabel = smallLabelContainer.subviews.first {
                            smallLabelContainer.subviews
                                .filter { $0.tag == accessoryTag }
                                .forEach { $0.removeFromSuperview() }
                            smallLabelContainer.addSubview(accessorySmall.view)
                            accessorySmall.view.backgroundColor = .clear
                            accessorySmall.view.translatesAutoresizingMaskIntoConstraints = false
                            
                            NSLayoutConstraint.activate([
                                accessorySmall.view.leadingAnchor.constraint(equalTo: smallLabelContainer.trailingAnchor),
                                accessorySmall.view.centerYAnchor.constraint(equalTo: smallLabelContainer.centerYAnchor),
                            ])
                        //}
                    }
                }
            }
        
            .introspect(.viewController, on: .iOS(.v18)) { viewController in
                guard let navController = viewController.navigationController else { return }
                guard navController.topViewController == viewController else { return }
                
                let navBar = navController.navigationBar
                let accessoryLarge = UIHostingController(rootView: largeView())
                let accessorySmall = UIHostingController(rootView: smallView())
                
                let accessoryTag = 9999
                accessorySmall.view.tag = accessoryTag
                accessoryLarge.view.tag = accessoryTag
                
                
                // Add accessory to large title
                
                
                if let largeLabelContainer = navBar.subviews.first(where: {
                    String(describing: type(of: $0)).contains("NavigationBarLargeTitleView")
                }) {
                    if let titleLabel = largeLabelContainer.subviews.first {
                        titleLabel.subviews
                            .filter { $0.tag == accessoryTag }
                            .forEach { $0.removeFromSuperview() }
                        titleLabel.addSubview(accessoryLarge.view)
                        accessoryLarge.view.backgroundColor = .clear
                        accessoryLarge.view.translatesAutoresizingMaskIntoConstraints = false
                        
                        NSLayoutConstraint.activate([
                            accessoryLarge.view.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
                            accessoryLarge.view.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
                        ])
                    }
                }
                
                // MARK: This here is unique to iOS 18
                // The LargeNavigationTitle does not exist in hierarchy if navigating back to an inlined title.
                // This isn't a good solution...
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                    Timer.scheduledTimer(withTimeInterval: 0.1, repeats: true) { timer in
                        if let largeLabelContainer = navBar.subviews.first(where: {
                            String(describing: type(of: $0)).contains("NavigationBarLargeTitleView")
                        }) {
                            if let titleLabel = largeLabelContainer.subviews.first {
                                titleLabel.subviews
                                    .filter { $0.tag == accessoryTag }
                                    .forEach { $0.removeFromSuperview() }
                                titleLabel.addSubview(accessoryLarge.view)
                                accessoryLarge.view.backgroundColor = .clear
                                accessoryLarge.view.translatesAutoresizingMaskIntoConstraints = false
                                
                                NSLayoutConstraint.activate([
                                    accessoryLarge.view.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
                                    accessoryLarge.view.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
                                ])
                                
                                timer.invalidate()
                            }
                        }
                    }
                }
                
                // Add accessory to small title
                
                if let contentContainer = navBar.subviews.first(where: {
                    String(describing: type(of: $0)).contains("NavigationBarContentView")
                }) {
                    if let smallLabelContainer = contentContainer.subviews.first(where: {
                        String(describing: type(of: $0)).contains("_UINavigationBarTitleControl")
                    }) {
                        if let smallLabel = smallLabelContainer.subviews.first {
                            smallLabel.subviews
                                .filter { $0.tag == accessoryTag }
                                .forEach { $0.removeFromSuperview() }
                            smallLabel.addSubview(accessorySmall.view)
                            accessorySmall.view.backgroundColor = .clear
                            accessorySmall.view.translatesAutoresizingMaskIntoConstraints = false
                            
                            NSLayoutConstraint.activate([
                                accessorySmall.view.leadingAnchor.constraint(equalTo: smallLabel.trailingAnchor),
                                accessorySmall.view.centerYAnchor.constraint(equalTo: smallLabel.centerYAnchor),
                            ])
                        }
                    }
                }
            }
            
            .introspect(.viewController, on: .iOS(.v26), .iOS(.v27)) { viewController in
                guard let navController = viewController.navigationController else { return }
                guard navController.topViewController == viewController else { return }
                let navBar = navController.navigationBar
                
                let accessoryLarge = UIHostingController(rootView: largeView())
                let accessorySmall = UIHostingController(rootView: smallView())
                
                let accessoryTag = 9999
                accessorySmall.view.tag = accessoryTag
                accessoryLarge.view.tag = accessoryTag
                
                if let navControllerView = navController.view,
                   let largeTitleContainer = findLargeTitleContainer(in: navControllerView) {
                    if let titleLabel = largeTitleContainer.subviews.first {
                        titleLabel.subviews
                            .filter { $0.tag == accessoryTag }
                            .forEach { $0.removeFromSuperview() }
                        titleLabel.addSubview(accessoryLarge.view)
                        accessoryLarge.view.backgroundColor = .clear
                        accessoryLarge.view.translatesAutoresizingMaskIntoConstraints = false
                        
                        NSLayoutConstraint.activate([
                            accessoryLarge.view.leadingAnchor.constraint(equalTo: titleLabel.trailingAnchor),
                            accessoryLarge.view.centerYAnchor.constraint(equalTo: titleLabel.centerYAnchor),
                        ])
                    }
                }
                
                if let contentContainer = navBar.subviews.first(where: {
                    String(describing: type(of: $0)).contains("NavigationBarContentView")
                }) {
                    if let smallLabelContainer = contentContainer
                        .subviews.first? .subviews.first? .subviews.first? .subviews.first? .subviews.first
                    {
                        //if let smallLabel = smallLabelContainer.subviews.first {
                            smallLabelContainer.subviews
                                .filter { $0.tag == accessoryTag }
                                .forEach { $0.removeFromSuperview() }
                            smallLabelContainer.addSubview(accessorySmall.view)
                            accessorySmall.view.backgroundColor = .clear
                            accessorySmall.view.translatesAutoresizingMaskIntoConstraints = false
                            
                            NSLayoutConstraint.activate([
                                accessorySmall.view.leadingAnchor.constraint(equalTo: smallLabelContainer.trailingAnchor),
                                accessorySmall.view.centerYAnchor.constraint(equalTo: smallLabelContainer.centerYAnchor),
                            ])
                        //}
                    }
                }
                
                func findLargeTitleContainer(in view: UIView) -> UIView? {
                    if String(describing: type(of: view)).contains("UINavigationBarLargeTitleView") {
                        return view.subviews.first(where: { $0 is UILabel }) != nil ? view : nil
                    }
                    
                    for subview in view.subviews {
                        if let result = findLargeTitleContainer(in: subview) {
                            return result
                        }
                    }
                    return nil
                }
            }
    }
    
    func navigationTitleAccessory(accessory: @escaping () -> some View) -> some View {
        return self.navigationTitleAccessory(small: accessory, large: accessory)
    }
    
    func navigationTitleAccessory(accessory: @escaping (/*isLarge: */Bool) -> some View) -> some View {
        return self.navigationTitleAccessory(small: {accessory(false)}, large: {accessory(true)})
    }
    
    func navigationTitleAccessoryCleanup() -> some View {
        return self.navigationTitleAccessory(small: {EmptyView()}, large: {EmptyView()})
    }
}



#Preview {
    VeryCustomNavBarView()
}

private func printHierarchy(_ view: UIView, indent: Int = 0) {
    let indentString = String(repeating: "  ", count: indent)
    print("\(indentString)\(type(of: view)) frame: \(view.frame)")
    
    for subview in view.subviews {
        printHierarchy(subview, indent: indent + 1)
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

private struct VeryCustomNavBarView: View {
    @Namespace var animation
    
    var body: some View {
        NavigationStack {
            ScrollView {
                NavigationLink("Navigate") {
                    DemoView2()
                        .navigationTitle("Test")
                        .navigationTitleAccessory {
                            Text("Test")
                        } large: {
                            Text("Test")
                        }
                }
                
                ForEach(1 ... 50, id: \.self) { index in
                    Text("Index: \(index)")
                }
                .frame(maxWidth: .infinity)
            }
            .navigationTitle("NavigationTitle")
            .navigationTitleAccessory {
                BarContent()
                    .matchedGeometryEffect(id: "Test2", in: animation)
            } large: {
                BarContent()
                    .matchedGeometryEffect(id: "Test", in: animation)
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


