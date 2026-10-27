//  Created on 25.10.25.

import SwiftUI


/// A Menu with working Label transition in iOS 26+ (instead adds glass outline on transition in dark mode, improvable)
/// (When using Menu, after making a selection, the new text transitions with old frame and then in a jump corrects, moving horizontally)
/// See https://www.reddit.com/r/SwiftUI/comments/1oc99ks/animation_glitch_in_ios_26/
struct BetterMenu<Content: View, Label: View>: View {
    let content: () -> Content
    let label: () -> Label
    
    @Environment(\.colorScheme) var colorScheme
    
    init(@ViewBuilder _ content: @escaping () -> Content, @ViewBuilder label: @escaping () -> Label) {
        self.content = content
        self.label = label
    }
    
    var body: some View {
        if #available(iOS 26.0, *) {
            GlassEffectContainer {
                Menu {
                    content()
                } label: {
                    label()
                        .glassEffect(.identity)
                }
            }
            
            // Alternative Solution, this however produces a glassEffect around the menu
            /*
            Menu {
                content()
            } label: {
                label()
            }
            .glassEffect()
            */
        } else {
            Menu {
                content()
            } label: {
                label()
            }
        }
    }
}


#Preview("SettingsView") {
    NavigationView {
        SettingsView()
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
    .environmentObject(MensaService(previewMode: true))
}


@available(iOS 26.0, *)
#Preview("Menu Introspection") {
    @Previewable @State var selection = "Short"

    VStack {
        Menu {
            Button("Short") {
                selection = "Short"
            }
            
            Button("A much much longer option") {
                selection = "A much much longer option"
            }
        } label: {
            Text(selection)
                .glassEffect(.identity)
        }
        .glassEffect(.clear)
    }
    .background(Color(.secondarySystemGroupedBackground))
    .padding()
}
