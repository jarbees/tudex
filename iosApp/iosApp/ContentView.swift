//  Created on 03.05.25.

import SwiftUI
import Shared
import SFSafeSymbols

struct ContentView: View {
    var body: some View {
        TabView {
            StudyView()
                .tabItem {
                    Label("Studies", systemSymbol: .graduationcapFill)
                }
            FoodView()
                .tabItem {
                    Label("Food", systemSymbol: .forkKnife)
                }
            NavigationStack {
                MapView()
            }
            .tabItem {
                Label("Maps", systemSymbol: .mapFill)
            }
            
            NavigationStack {
                BookView()
            }
            .tabItem {
                Label("Books", systemSymbol: .bookClosedFill)
            }
            
            NavigationStack {
                SettingsView()
            }
            .tabItem {
                Label("Settings", systemSymbol: .ellipsis)
            }
        }
        .tabViewCrossfade()
        .modify { view in
            if #available(iOS 26.0, *) {
                view.scrollEdgeEffectStyle(.soft, for: .vertical)
            } else {
                view
            }
        }
    }
}

#Preview() {
    ContentView()
        .environmentObject(TucanService())
        .environmentObject(IdentityService(previewMode: true))
        .environmentObject(MensaService())
        .environmentObject(KeyboardHeightHelper())
        .environmentObject(SettingsService())
}
