//  Created on 19.06.25.

import SwiftUI
import SFSafeSymbols

private struct NavigationButtonStyle: ButtonStyle {
    @Binding var isNavigating: Bool
    
    func makeBody(configuration: Configuration) -> some View {
        return configuration.label
            .background(Color((configuration.isPressed || isNavigating) ? .quaternaryLabel : .secondarySystemGroupedBackground))
            .animation(.default, value: configuration.isPressed)
            .animation(.default.speed(2), value: isNavigating)
    }
}

enum NavButtonStyle {
    case plain
    case styled
}

extension EnvironmentValues {
    @Entry var navButtonStyle: NavButtonStyle = .styled
}

extension NavigationLinkButton {
    func navButtonStyle(_ style: NavButtonStyle) -> some View {
        environment(\.navButtonStyle, style)
    }
}

struct NavigationLinkButton<Destination: View, Content: View>: View {
    private let action: (() -> Void)?
    private let content: () -> Content
    private let destination: () -> Destination
    
    @Environment(\.navButtonStyle) private var style
    @Environment(\.listVerticalPadding) private var verticalPadding
    @Environment(\.listLeadingPadding) private var leadingPadding
    
    private let externalIsNavigating: Binding<Bool>?
    @State private var defaultIsNavigating = false

    private var isNavigating: Binding<Bool> {
        externalIsNavigating ?? $defaultIsNavigating
    }
    
    init(
        action: @escaping () -> Void,
        content: @escaping () -> Content,
        destination: @escaping () -> Destination,
        isNavigating: Binding<Bool>
    ) {
        self.action = action
        self.content = content
        self.destination = destination
        self.externalIsNavigating = isNavigating
    }
    
    init(
        content: @escaping () -> Content,
        destination: @escaping () -> Destination
    ) {
        self.action = nil
        self.content = content
        self.destination = destination
        self.externalIsNavigating = nil
    }
    
    var body: some View {
        // See FailedPlainNavigationButton for various (unsuccessful) alternatives
        // This is a compromise which might break in future iOS versions...
        // Also is broken in iOS 17.0
        Group {
            if #available(iOS 17.0, *) {
                Button(action: {
                    (action ?? { isNavigating.wrappedValue = true })()
                }) {
                    content()
                        .padding(.vertical, verticalPadding)
                        .padding(.leading, leadingPadding)
                }
                .background(
                    NavigationLink(destination: destination(), isActive: isNavigating) {
                        content()
                    }
                    .hidden()
                )
                .padding(.vertical, -verticalPadding)
                .padding(.leading, -leadingPadding)
            } else {
                Button(action: {
                    (action ?? { isNavigating.wrappedValue = true })()
                }) {
                    content()
                        .padding(.vertical, verticalPadding)
                        .padding(.leading, leadingPadding)
                }
                .navigationDestination(isPresented: isNavigating, destination: {destination()})
                .padding(.vertical, -verticalPadding)
                .padding(.leading, -leadingPadding)
            }
        }
        .modify { view in
            if style == .styled {
                view.buttonStyle(NavigationButtonStyle(isNavigating: isNavigating))
            } else {
                view
            }
        }
    }
}


#Preview {
    List {
        
    }
}


// See below for a collection of failed attempts to do something similar
/*
 private struct FailedPlainNavigationButton<Destination: View, Content: View>: View {
     let action: () -> Void
     let content: () -> Content
     let destination: () -> Destination
     let type: Int
     
     @Binding var isNavigating: Bool
     
     var body: some View {
         if type == 1 {
             // This view is deprecated
             // In iOS 16.0, isNavigating is always false, even though the view navigates
             // In iOS 17.0, isNavigating is true when View is visible and during appear/disappear
             // Also when going from .inline to .large NavTitle, a Jump occurs (bad)
             // In iOS 18.0 & 26.0, isNavigating is true when View is visible and during appear (only)
             // -> Inconsistent before iOS 18, after that seems ideal
             Button(action: {
                 action()
             }) {
                 content()
             }
             .background(
                  NavigationLink(destination: destination(), isActive: $isNavigating) {
                      content()
                  }
                  
                 .hidden()
             )
         } else if type == 2 {
             // This view appears to be the obvious choice
             // In iOS 16.0, isNavigating is true when View is visible and during appear/disappear
             // In iOS 17.0, same as 16.0 but sometimes inconsistent spacing like 26.0
             // In iOS 26.0, isNavigating is true when View is visible and during appear (only)
             // In iOS 26, the navigationTitle space jumps causing shift in ScrollView
             // Almost good, but not quite
             Button(action: {
                 action()
             }) {
                 content()
             }
             //.background(Color(.secondarySystemGroupedBackground))
             .navigationDestination(isPresented: $isNavigating, destination: {destination()})
         } else if type == 3 {
             // Other options would be:
             
             // .navigationDestination(item: ..., destination: ...)
             // This is only supported after iOS 17 and is hacky.
             
             // .navigationDestination(for: Hashable.type, destination: (Hashable) -> View)
             // In combination with NavigationPath in NavigationStack
             // This is very hacky and also a pain to implement
             // Would theoretically work, but isnt worth the effort
         }
     }
 }

 */
