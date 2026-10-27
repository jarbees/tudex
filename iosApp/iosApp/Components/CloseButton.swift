//  Created on 07.09.25.

import SwiftUI
import SFSafeSymbols

struct CloseButton: View {
    let closeAction: () -> Void
    let invertedColor: Bool
    
    init(closeAction: @escaping () -> Void) {
        self.closeAction = closeAction
        self.invertedColor = false
    }
    
    init(closeAction: @escaping () -> Void, invertedColor: Bool) {
        self.closeAction = closeAction
        self.invertedColor = invertedColor
    }
    
    var body: some View {
        Group {
            Button(action: {
                closeAction()
            }) {
                if #available(iOS 26.0, *) {
                    Image(systemSymbol: .xmark)
                        .font(.system(size: 16))
                        .bold()
                        .frame(width: 8, height: 18)
                        
                } else {
                    Image(systemSymbol: .xmarkCircleFill)
                        .font(.system(size: 28))
                        .foregroundColor(Color(invertedColor ? .label : .secondarySystemGroupedBackground))
                        .shadow(radius: 8)
                }
            }
            .modify {
                if #available(iOS 26.0, *) {
                    $0.buttonStyle(GlassButtonStyle())
                }
            }
        }
        .padding(20)
        .ignoresSafeArea()
    }
}

#Preview {
    CloseButton(closeAction: {})
}
