//  Created on 21.12.25.

import SwiftUI

enum NavigationDirection {
    case forward
    case backward
}

extension AnyTransition {
    static func directionalSlide(_ direction: NavigationDirection) -> AnyTransition {
        .asymmetric(
            insertion: .move(edge: direction == .forward ? .trailing : .leading),
            removal:  .move(edge: direction == .forward ? .leading  : .trailing)
        )
    }
}
