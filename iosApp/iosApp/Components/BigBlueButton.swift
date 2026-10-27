//  Created on 21.12.25.

import SwiftUI
import SFSafeSymbols

struct BigBlueButton<Content: View>: View {
    let action: () -> Void
    let content: () -> Content
    
    init(action: @escaping () -> Void, content: @escaping () -> Content) {
        self.action = action
        self.content = content
    }
    
    init(_ title: LocalizedStringKey, systemSymbol: SFSymbol, action: @escaping () -> Void) where Content == _SemiboldLabel {
        self.action = action
        self.content = {
            _SemiboldLabel(title: title, systemSymbol: systemSymbol)
        }
    }
    
    init(_ title: LocalizedStringKey, action: @escaping () -> Void) where Content == Text {
        self.action = action
        self.content = {
            Text(title)
                .fontWeight(.semibold)
        }
    }
    
    var body: some View {
        Button(action: {
            action()
        }) {
            content()
                .frame(maxWidth: .infinity)
        }
        .controlSize(.large)
        .modify { view in
            if #available(iOS 26, *) {
                view.buttonStyle(GlassProminentButtonStyle())
            } else {
                view.buttonStyle(BorderedProminentButtonStyle())
                    .cornerRadius(14)
            }
        }
    }
}

struct _SemiboldLabel: View {
    let title: LocalizedStringKey
    let systemSymbol: SFSymbol
    
    var body: some View {
        Label(title, systemSymbol: systemSymbol)
            .fontWeight(.semibold)
    }
}
