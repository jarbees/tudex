//  Created on 01.06.25.

import SwiftUI

class KeyboardHeightHelper: ObservableObject {
    @Published var keyboardHeight: CGFloat = 0
    private var isActive = false

    func activate() {
        guard !isActive else { return }
        isActive = true

        NotificationCenter.default.addObserver(forName: UIResponder.keyboardDidShowNotification, object: nil, queue: .main) { (notification) in
            guard let userInfo = notification.userInfo,
                  let keyboardRect = userInfo[UIResponder.keyboardFrameEndUserInfoKey] as? CGRect else { return }
            
            self.keyboardHeight = keyboardRect.height
        }
        
        NotificationCenter.default.addObserver(forName: UIResponder.keyboardDidHideNotification, object: nil, queue: .main) { (notification) in
            self.keyboardHeight = 0
        }
    }

    func deactivate() {
        guard isActive else { return } // Only deactivate if active
        isActive = false

        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardDidShowNotification, object: nil)
        NotificationCenter.default.removeObserver(self, name: UIResponder.keyboardDidHideNotification, object: nil)
    }
}
