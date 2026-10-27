//  Created on 04.12.25.

import SwiftUI
import Shared

class IdentityService: ObservableObject {
    private let identityManager = IdentityManager.shared
    
    @Published private(set) var username: String                    = IdentityManager.shared.tuUsername.value
    @Published private(set) var tuLoginStatus: TULoginState         = IdentityManager.shared.tuLoginStatus.value
    @Published private(set) var totpSecretExists: Bool              = Bool(truncating: IdentityManager.shared.tuTotpSecretExists.value)
    
    @Published private(set) var studentID: StudentID?               = IdentityManager.shared.studentID.value
    @Published private(set) var studentIDStatus: RequestStatus      = IdentityManager.shared.studentIDStatus.value
    
    // Initialization
    
    convenience init() {
        self.init(previewMode: false)
    }
    
    init(previewMode: Bool) {
        Task {
            if previewMode {
                identityManager.initializeDebugStates()
                identityManager.tuDemoLogin()
            }
            await syncStates()
        }
    }
    
    /// Ensures that all states of IdentiyService are synced to singleton instance IdentityManager.shared
    @MainActor
    private func syncStates() async {
        Task {
            for await tempUsername in identityManager.tuUsername {
                self.username = tempUsername
            }
        }
        Task {
            for await tempTuLoginStatus in identityManager.tuLoginStatus {
                self.tuLoginStatus = tempTuLoginStatus
            }
        }
        Task {
            for await tempTotpSecretExists in identityManager.tuTotpSecretExists {
                self.totpSecretExists = Bool(truncating: tempTotpSecretExists)
            }
        }
        Task {
            for await tempStudentID in identityManager.studentID {
                self.studentID = tempStudentID
            }
        }
        Task {
            for await tempStudentIDStatus in identityManager.studentIDStatus {
                self.studentIDStatus = tempStudentIDStatus
            }
        }
    }
    
    // Functions
    
    /**
     * Performs 2FA TU login using provided totp secret.
     *
     * Only useful if called after successful ``tuBasicLogin(username:password:)``
     *
     * If successful, starts loading studentID.
     *
     * **Warning**: This will fail if called too rapidly as each one-time-code can only be used once and only current and next code (+30s) are used
     *
     * @param totpSecret The user provided totp Secret. 2FA-token must contain name "TUPlan"
     * @return Int status code following the convention for login returns:
     *   - `-1`: unknown error
     *   - `-2`: invalid credentials
     *   - `-3`: missing TOTP token
     *   - `-4`: wrong token entered
     *   - Otherwise: HTTP status code (`200` = success, `404` = not found, etc.)
     */
    
    func tuDemoLogin() {
        identityManager.tuDemoLogin()
    }
    
    func tucanLogin(code: String, codeVerifier: String, nonce: String) async {
        try? await identityManager.tucanLogin(code: code, codeVerifier: codeVerifier, nonce: nonce)
    }
    
    func tuLogin(username: String, password: String) async -> Int {
        let status = try? await Int(truncating: identityManager.tuLogin(username: username, password: password))
        return status ?? -1
    }
    func tuLogin(username: String, password: String, totpSecret: String) async -> Int {
        let status = try? await Int(truncating: identityManager.tuLogin(username: username, password: password, totpSecret: totpSecret))
        return status ?? -1
    }
    
    /**
     * Log in with TU ID based on previous successful login.
     * Results in either mfa, basic or no login.
     *
     * @return Int status code following the convention for login returns:
     *   - `-1`: unknown error
     *   - `-2`: invalid credentials
     *   - `-3`: no token named "tuplan" exists in TUDa 2FA-Management
     *   - `-4`: wrong token entered
     *   - Otherwise: HTTP status code (`200` = success, `404` = not found, etc.)
     */
    func tuRefreshLogin() async -> Int {
        let status = try? await Int(truncating: identityManager.tuRefreshLogin())
        return status ?? -1
    }
    
    /**
     * Resets all states **except** `tuTotpSecret`
     *
     * `tuTotpSecret` must be reset using ``resetTotpSecret()``
     */
    func logout() async {
        try? await identityManager.tuLogout()
    }
    
    
    
    /// Resets the stored `tuTotpSecret` to `""`
    func resetTotpSecret() {
        identityManager.resetTotpSecret()
    }
    
    
    /**
     * Get the next two 2FA one-time-codes. Requires mfa-login.
     *
     * @return current (+0s) and next (+30s) one-time-codes for 2FA
     */
    func getTotpCodePair() -> (current: String, next: String)? {
        guard let kotlinPair = identityManager.getTotpCodePair() else { return nil }
        
        return (current: String(kotlinPair.first ?? "Error"), next: String(kotlinPair.second ?? "Error"))
    }
    
    /**
     * Updates ``studentID`` property from internet.
     *
     * Requires successful mfa-login before usage.
     */
    func updateStudentID() async {
        try? await identityManager.updateStudentID()
    }
}
