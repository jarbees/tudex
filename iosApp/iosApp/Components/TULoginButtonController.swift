//  Created on 22.12.25.

import SwiftUI

struct TULoginController<Content: View>: View {
    @Binding var isSheetPresented: Bool
    @Binding var skipBasicLogin: Bool
    
    let isMFA: Bool
    let content: (_ loginAction: @escaping () -> Void, _ isLoading: Bool) -> Content
    
    @EnvironmentObject private var identityService: IdentityService
    
    @State private var isLoading: Bool = false
    @State private var showErrorModal: Bool = false
    @State private var modalType: String = "Basic"
    @State private var errorText: String = ""
    
    init(
        isSheetPresented: Binding<Bool>,
        skipBasicLogin: Binding<Bool>,
        isMfa: Bool = false,
        @ViewBuilder content: @escaping (_ loginAction: @escaping () -> Void, _ isLoading: Bool) -> Content
    ) {
        self._isSheetPresented = isSheetPresented
        self._skipBasicLogin = skipBasicLogin
        self.isMFA = isMfa
        self.content = content
    }
    
    init(
        @ViewBuilder content: @escaping (_ loginAction: @escaping () -> Void, _ isLoading: Bool) -> Content
    ) {
        self._isSheetPresented = Binding(get: { return false }, set: { _ in })
        self._skipBasicLogin = Binding(get: { return false }, set: { _ in })
        self.isMFA = false
        self.content = content
    }
    
    var body: some View {
        content(loginAction, isLoading)
            .disabled(isLoading)
            .alert("Login failed", isPresented: $showErrorModal) {
                Button("Retry") {
                    tuLogin()
                }
                if modalType == "Basic" {
                    Button("Enter login details") {
                        showErrorModal = false
                        skipBasicLogin = false
                        isSheetPresented = true
                    }
                } else if modalType == "MFA" {
                    Button("Reset TOTP Secret") {
                        showErrorModal = false
                        skipBasicLogin = true
                        isSheetPresented = true
                    }
                }
                Button("Cancel") {
                    showErrorModal = false
                }
            } message: {
                Text(errorText)
            }
    }
    
    private func loginAction() {
        if (identityService.username == "") {      // username is either valid or ""
            if !isMFA {
                let oathHelper = OAuthViewModel()
                oathHelper.startLogin(exchangeCodeForTokens: { code, verifier, nonce in
                    Task {
                        await identityService.tucanLogin(code: code, codeVerifier: verifier, nonce: nonce)
                    }
                })
            } else {
                isSheetPresented = true
            }
        }
        else {
            tuLogin()
        }
    }
    
    private func tuLogin() {
        Task {
            isLoading = true
            
            let status = await identityService.tuRefreshLogin()
            if (status != 200) {
                if (status == -4) {
                    errorText = String(localized: "2FA couldn't be completed.\nWait 30s then try again\nUsername and password are correct")
                    modalType = "Basic"
                } else if (status == -2) {
                    errorText = String(localized: "Username or password wasn't accepted")
                    modalType = "MFA"
                } else if (status == -1) {
                    errorText = String(localized: "An unknown error has occurred.\nThis could be due to your internet connection\nUsername and password are probably correct")
                    modalType = "Basic"
                } else {
                    errorText = String(localized: "Web request failed with Error code") + "\(status)"
                    modalType = "Basic"
                }
                
                showErrorModal = true
            }
            
            isLoading = false
        }
    }
}
