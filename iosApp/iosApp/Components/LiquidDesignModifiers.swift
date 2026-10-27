//  Created on 22.08.25.

import SwiftUI

extension View {
    func rounded(liquidCornerRadius: CGFloat = 26, oldCornerRadius: CGFloat = 10) -> some View {
        #if os(iOS)
        if #available(iOS 26.0, *) {
            return self.cornerRadius(liquidCornerRadius)
        } else {
            return self.cornerRadius(oldCornerRadius)
        }
        #else
        return self.cornerRadius(oldCornerRadius)
        #endif
    }
    
    func glassEffectIfAvailable() -> some View {
        if #available(iOS 26.0, *) {
            return self.glassEffect()
        } else {
            return self
        }
    }
    
    func dividerListPadding() -> some View {
        if #available(iOS 26.0, *) {
            return self.padding(.trailing, 16)
        } else {
            return self
        }
    }
    
    /*func ListPadding() -> some View {
        if #available(iOS 26.0, *) {
            return self.padding(.trailing, 20)
        } else {
            return self
        }
    }*/
    
    @ViewBuilder
    func modify(@ViewBuilder _ transform: (Self) -> (some View)?) -> some View {
        if let view = transform(self), !(view is EmptyView) {
            view
        } else {
            self
        }
    }
}
