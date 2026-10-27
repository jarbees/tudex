//  Created on 14.03.26.

import SwiftUI
import AuthenticationServices
import CryptoKit

class OAuthViewModel: NSObject, ObservableObject {
    func startLogin(exchangeCodeForTokens: @escaping (_ code: String, _ verifier: String, _ nonce: String) -> Void) {
        // Generate PKCE code verifier and code challenge
        let codeVerifier = generateCodeVerifier()
        let codeChallenge = generateCodeChallenge(from: codeVerifier)
        
        let state = randomURLSafeString(length: 32)
        let nonce = randomURLSafeString(length: 32)
        
        // Build the OAuth URL
        var components = URLComponents(string: "https://dsf.tucan.tu-darmstadt.de/IdentityServer/connect/authorize")!
        components.queryItems = [
            URLQueryItem(name: "nonce", value: nonce.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)),
            URLQueryItem(name: "max_age", value: "0"),
            URLQueryItem(name: "response_type", value: "code"),
            URLQueryItem(name: "code_challenge_method", value: "S256"),
            URLQueryItem(name: "scope", value: "openid DSF profile offline_access"),
            URLQueryItem(name: "code_challenge", value: codeChallenge.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)),
            URLQueryItem(name: "redirect_uri", value: "de.datenlotsen.campusnet.tuda://oauth2redirect"),
            URLQueryItem(name: "client_id", value: "MobileApp"),
            URLQueryItem(name: "state", value: state.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed)),
        ]
        
        guard let authURL = components.url else { return }
        
        // Start ASWebAuthenticationSession
        let authSession = ASWebAuthenticationSession(
            url: authURL,
            callbackURLScheme: "de.datenlotsen.campusnet.tuda"
        ) { callbackURL, error in
            if let error = error {
                print("Auth error:", error)
                return
            }
            
            guard let callbackURL = callbackURL,
                  let queryItems = URLComponents(url: callbackURL, resolvingAgainstBaseURL: false)?.queryItems,
                  let code = queryItems.first(where: { $0.name == "code" })?.value,
                  let returnedState = queryItems.first(where: { $0.name == "state" })?.value
            else { return }
            
            guard returnedState == state else { return }   // State should always match, otherwise there may be a CSRF attack
            
            exchangeCodeForTokens(code, codeVerifier, nonce)
        }
        
        //authSession.prefersEphemeralWebBrowserSession = true      // This might make TucanLoginFail obsolete?
        authSession.presentationContextProvider = self
        authSession.start()
    }
    
    private func generateCodeVerifier() -> String {
        let bytes = [UInt8].random(count: 32)
        return Data(bytes).base64URLEncodedString()
    }
    
    private func generateCodeChallenge(from verifier: String) -> String {
        let data = Data(verifier.utf8)
        let hash = SHA256.hash(data: data)
        return Data(hash).base64URLEncodedString()
    }
    
    func randomURLSafeString(length: Int) -> String {
        let bytes = [UInt8].random(count: length)
        let data = Data(bytes)
        return data.base64URLEncodedString()
    }
}

extension OAuthViewModel: ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        // Get the first foreground active UIWindowScene
        let windowScene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
        
        // Return its key window
        return windowScene?.windows.first { $0.isKeyWindow } ?? UIWindow()
    }
}

extension Data {
    func base64URLEncodedString() -> String {
        self.base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

extension Array where Element == UInt8 {
    static func random(count: Int) -> [UInt8] {
        var buffer = [UInt8](repeating: 0, count: count)
        let _ = SecRandomCopyBytes(kSecRandomDefault, count, &buffer)
        return buffer
    }
}
