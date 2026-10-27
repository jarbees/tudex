//  Created on 19.04.26.

import SwiftUI
import Shared

class SettingsService: ObservableObject {
    private let settingsManager = SettingsManager.shared
    
    @Published private(set) var userImage: UIImage?                 = SettingsManager.shared.userImage.value?.uiImage
    @Published private(set) var timeViewType: TimeViewType          = SettingsManager.shared.timeViewType.value
    @Published private(set) var isMfaLoginAvailable: Bool           = Bool(truncating: SettingsManager.shared.isMfaLoginAvailable.value)
    @Published private(set) var isDemoLoginAvailable: Bool          = Bool(truncating: SettingsManager.shared.isDemoLoginAvailable.value)
    @Published private(set) var uiType: UIType                      = SettingsManager.shared.uiType.value         // This is a debug property
    
    // Initialization
    
    init() {
        Task {
            await syncStates()
        }
    }
    
    /// Ensures that all states of IdentiyService are synced to singleton instance IdentityManager.shared
    @MainActor
    private func syncStates() async {
        Task {
            for await tempUserImage in settingsManager.userImage {
                self.userImage = tempUserImage?.uiImage
            }
        }
        Task {
            for await tempTimeViewType in settingsManager.timeViewType {
                self.timeViewType = tempTimeViewType
            }
        }
        Task {
            for await tempIsMfaLoginAvailable in settingsManager.isMfaLoginAvailable {
                self.isMfaLoginAvailable = Bool(truncating: tempIsMfaLoginAvailable)
            }
        }
        Task {
            for await tempIsDemoLoginAvailable in settingsManager.isDemoLoginAvailable {
                self.isDemoLoginAvailable = Bool(truncating: tempIsDemoLoginAvailable)
            }
        }
        Task {
            for await tempUiType in settingsManager.uiType {
                self.uiType = tempUiType
            }
        }
    }
    
    // Functions
    
    /// Sets a new image for the student ID Card
    func setUserImageData(to data: Foundation.Data) {
        settingsManager.setUserImage(imageData: data.toByteArray())
    }
    
    func updateTimeViewType(to newType: TimeViewType) {
        settingsManager.updateTimeViewType(newType: newType)
    }
    
    func updateMfaLoginAvailability(to isAvailable: Bool) {
        settingsManager.updateMfaLoginAvailability(isAvailable: isAvailable)
    }
    
    func updateDemoLoginAvailability(to isAvailable: Bool) {
        settingsManager.updateDemoLoginAvailability(isAvailable: isAvailable)
    }
    
    #if DEBUG
    /// A debugging function which toggles ``uiType`` between `.compose` and `.swiftUi`
    func switchUiType() {
        settingsManager.switchUiType()
    }
    #endif
}
