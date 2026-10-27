//  Created on 26.05.25.

import SwiftUI
import Shared
import SFSafeSymbols
import CodeScanner
import AVFoundation

struct TULoginView: View {
    @Binding var isPresented: Bool
    
    @State private var username: String = ""
    @State private var password: String = ""
    @State private var totpSecret: String = ""
    
    @State private var loginFlowPage: Int = 0       // 0: username+password, 1: totpSecret
    @State private var direction: NavigationDirection = .forward
    
    @State private var showErrorModal: Bool = false
    @State private var errorText: String = "An unknown error occurred."
    
    init(isPresented: Binding<Bool>, skipBasicLogin: Bool = false) {
        self._isPresented = isPresented
        self._loginFlowPage = State(initialValue: skipBasicLogin ? 1 : 0)
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                switch loginFlowPage {
                case 0:
                    TUCredentialsView(username: $username, password: $password, showErrorModal: $showErrorModal, errorText: $errorText, loginFlowPage: $loginFlowPage, isPresented: $isPresented)
                        .transition(.directionalSlide(direction))
                case 1:
                    TUInformationView(isPresented: $isPresented, direction: $direction, loginFlowPage: $loginFlowPage)
                        .transition(.directionalSlide(direction))
                case 2:
                    TUTotpInstructionsView(direction: $direction, loginFlowPage: $loginFlowPage)
                        .transition(.directionalSlide(direction))
                case 3:
                    TUTotpView(username: $username, password: $password, totpSecret: $totpSecret, direction: $direction, loginFlowPage: $loginFlowPage, isPresented: $isPresented)
                        .transition(.directionalSlide(direction))
                case 4:
                    TUConfirmTotpView(username: username, password: password, totpSecret: $totpSecret, isPresented: $isPresented, direction: $direction, loginFlowPage: $loginFlowPage, errorText: $errorText, showErrorModal: $showErrorModal)
                        .transition(.directionalSlide(direction))
                default:
                    EmptyView()
                }
            }
            .alert(isPresented: $showErrorModal) {
                Alert(
                    title: Text("Login failed"),
                    message: Text(errorText),
                    dismissButton: .default(Text("OK"))
                )
            }
            .navigationBarItems(leading: Button("Cancel") {
                isPresented = false
            })
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

private struct TUCredentialsView: View {
    @EnvironmentObject var keyboardHeightHelper: KeyboardHeightHelper
    @EnvironmentObject var identityService: IdentityService
    
    @Binding var username: String
    @Binding var password: String
    
    @Binding var showErrorModal: Bool
    @Binding var errorText: String
    
    @Binding var loginFlowPage: Int
    @Binding var isPresented: Bool
    
    @State private var isLoading: Bool = false
    @FocusState private var isFocused: Bool
    
    private var verticalPadding: CGFloat {
        if #available(iOS 26.0, *) {
            16
        } else {
            12
        }
    }
        
    var body: some View {
        VStack {
            Image(systemSymbol: .key)
                .resizable()
                .scaledToFit()
                .frame(width: 58, height:  58)
                .padding(.top, 10)
                .foregroundStyle(Color.accentColor)
            Text("Login with TU ID")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 10)
                .padding(.bottom)
            
            VStack(spacing: 0) {
                TextField("Username", text: $username)
                    .textContentType(.username)
                    .padding(.leading)
                    .padding(.vertical, verticalPadding)  // different padding for iOS 26+
                    .foregroundStyle(isLoading ? .tertiary : .primary)
                    .focused($isFocused)
                Divider()
                    .padding(.leading)
                    .dividerListPadding()
                SecureField("Password", text: $password)
                    .textContentType(.password)
                    .padding(.leading)
                    .padding(.vertical, verticalPadding)  // different padding for iOS 26+
                    .foregroundStyle(isLoading ? .tertiary : .primary)
                    .focused($isFocused)
            }
            .background(Color(.secondarySystemBackground))
            .rounded()
            .padding(.horizontal)
            
            FooterText("Your account details are only used for login with TUDa.\nThey are stored locally and never shared with anyone else.\nLearn more... (no link yet)")
                .fixedSize(horizontal: false, vertical: true)
                .padding(.bottom, -8)
            
            Spacer()
                .frame(minHeight: 0)
            
            BigBlueButton("Login") {
                Task {
                    // await basicLogin()
                    if (identityService.totpSecretExists) {
                        await mfaLogin()
                    } else {
                        loginFlowPage = 2
                    }
                }
            }
            .padding()
            .disabled(username.isEmpty || password.isEmpty)
            
            
            Spacer()
                .frame(height: keyboardHeightHelper.keyboardHeight == 0 ? 38 : 0)
            
            /*
            Spacer()
                .frame(height: 38)
                .ignoresSafeArea(.keyboard)
             */
            
        }
        .disabled(isLoading)
        .onAppear(perform: keyboardHeightHelper.activate)
        .onDisappear(perform: keyboardHeightHelper.deactivate)
        .onTapGesture {
            isFocused = false
        }
    }
    
    private func basicLogin() async -> Int {
        isLoading = true
        
        //let status: Int = await identityService.tuBasicLogin(username: username, password: password)
        
        /*
        if status != 200 {
            if (status == -2) {
                errorText = "Username or Password is incorrect, please try again"
            } else if (status == -1) {
                errorText = "An unknown error has occurred.\nThis could be due to your internet connection"
            } else {
                errorText = "The web request failed with Error code \(status)"
            }
        
            showErrorModal = true
        } else {
            if identityService.totpSecretExists {
                let mfaStatus = await identityService.tuLogin()
                
                if mfaStatus == 200 {
                    isPresented = false
                } else {
                    // TODO: This isn't optimal handling, better would be to navigate to a seperate View
                    withAnimation {
                        loginFlowPage = 1
                    }
                }
            } else {
                withAnimation {
                    loginFlowPage = 1
                }
            }
        }
        */
        
        loginFlowPage = 2       // Skip InformationView, as it is unneccessary
        
        isLoading = false
        
        //return status
        return 200
    }
    
    private func mfaLogin() async {
        isLoading = true
        let status: Int = await identityService.tuLogin(username: username, password: password)
        isLoading = false
        
        if (status == 200 || status == 302) {
            isPresented = false
        } else {
            if (status == -4 || status == -3) {
                loginFlowPage = 2
            } else if (status == -2) {
                errorText = "The SSO rejected your username or password.\nSince TUCaN accepted it, either try again, completely log out of the app or restart..."
            } else if (status == -1) {
                errorText = "An unknown error has occurred.\nThis could be due to your internet connection"
            } else {
                errorText = "The web request failed with Error code \(status)"
            }
        
            showErrorModal = true
        }
    }
}

private struct TUInformationView: View {
    @Binding var isPresented: Bool
    @Binding var direction: NavigationDirection
    @Binding var loginFlowPage: Int
    
    var body: some View {
        VStack {
            var symbol: SFSymbol {
                if #available(iOS 17.0, *) {
                    .lockCircleDotted
                } else {
                    .lockCircle
                }
            }
            
            Image(systemSymbol: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: 58, height:  58)
                .padding(.top, 10)
                .foregroundStyle(Color.accentColor)
            
            Text("Continue with 2FA?")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 10)
                .padding(.bottom)
            
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 32) {
                GridRow {
                    GridRowView("No Login\nCanteens and Maps", systemSymbol: .shield)
                }
                
                GridRow {
                    GridRowView("Basic Login\n+ Timetable, Courses, Exams, etc.", systemSymbol: .shieldLefthalfFilled)
                }
                
                GridRow {
                    GridRowView("2FA login\n+ Student IDs", systemSymbol: .checkmarkShieldFill)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 32)
            
            Spacer()
            
            BigBlueButton("Continue") {
                direction = .forward
                withAnimation {
                    loginFlowPage = 2
                }
            }
            .padding(.horizontal)
            
            Button("Use Basic Login") {
                isPresented = false
            }
            .padding()
            
            Spacer()
                .frame(height: 38)
        }
    }
}

private struct GridRowView: View {
    let title: LocalizedStringKey
    let systemSymbol: SFSymbol
    
    init(_ title: LocalizedStringKey, systemSymbol: SFSymbol) {
        self.title = title
        self.systemSymbol = systemSymbol
    }
    
    var body: some View {
        Image(systemSymbol: systemSymbol)
            .resizable()
            .scaledToFit()
            .frame(width: 44, height:  44)
            .foregroundStyle(Color.accentColor)
        
        Text(title)
    }
}

private struct GridRowViewWithHyperlink: View {
    let title: String
    let systemSymbol: SFSymbol
    
    init(_ title: String, systemSymbol: SFSymbol) {
        self.title = title
        self.systemSymbol = systemSymbol
    }
    
    var body: some View {
        Image(systemSymbol: systemSymbol)
            .resizable()
            .scaledToFit()
            .frame(width: 44, height:  44)
            .foregroundStyle(Color.accentColor)
        
        Text(.init(title))      // By using init, Hyperlinks become clickable
    }
}

private struct TUTotpInstructionsView: View {
    @Binding var direction: NavigationDirection
    @Binding var loginFlowPage: Int
    
    var body: some View {
        VStack {
            Image(systemSymbol: .infoCircle)
                .resizable()
                .scaledToFit()
                .frame(width: 58, height:  58)
                .padding(.top, 10)
                .foregroundStyle(Color.accentColor)
            
            Text("Setup 2FA for TUPlan")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 10)
            
            Text("If possible, use a separate device and follow these steps there:")
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
                .padding(.bottom)
            
            Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 32) {
                GridRow {
                    GridRowViewWithHyperlink(
                        String(localized: "Open 2FA Management and login\n([login.tu-darmstadt.de/2fa](https://login.tu-darmstadt.de/2fa))"),
                        systemSymbol: ._1Circle
                    )
                }
                
                GridRow {
                    GridRowView("Enroll a new **TOTP** Token and include \"**TUPlan**\" in its description", systemSymbol: ._2Circle)
                }
                
                GridRow {
                    GridRowView("You should now see a QR Code,\nkeep the website open", systemSymbol: ._3Circle)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 32)
            .padding(.bottom)
            
            Text("You can now press continue below")
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal)
                .padding(.bottom)
            
            Spacer()
            
            BigBlueButton("Continue") {
                direction = .forward
                withAnimation {
                    loginFlowPage = 3
                }
            }
            .padding(.horizontal)
            
            /*
            Button("View login types again") {
                direction = .backward
                withAnimation {
                    loginFlowPage = 1
                }
            }
            .padding()
             */
            
            Button("Change username or password") {
                direction = .backward
                withAnimation {
                    loginFlowPage = 0
                }
            }
            .padding()
            
            Spacer()
                .frame(height: 38)
        }
    }
}

private struct TUTotpView: View {
    @Binding var username: String
    @Binding var password: String
    @Binding var totpSecret: String
    
    @Binding var direction: NavigationDirection
    @Binding var loginFlowPage: Int
    
    @Binding var isPresented: Bool
    
    @FocusState private var isFocused: Bool
    
    @State private var scannerState: String = "info"        // "info", "scanning" or "fail"
    @State private var scannedString: String = ""
    
    private var verticalPadding: CGFloat {
        if #available(iOS 26.0, *) {
            16
        } else {
            12
        }
    }
    
    var body: some View {
        VStack {
            /*
            Image(systemSymbol: .personBadgeKey)
                .resizable()
                .scaledToFit()
                .frame(width: 58, height:  58)
                .padding(.top, 10)
                .foregroundStyle(Color.accentColor)
            
            Text("Scan QR Code")
                .font(.largeTitle)
                .fontWeight(.bold)
                .padding(.bottom, 10)
            */
            let permission = AVCaptureDevice.authorizationStatus(for: .video)
            
            if permission == AVAuthorizationStatus.denied ||
               permission == AVAuthorizationStatus.restricted
            {
                Spacer()
                VStack {
                    Text("Camera Access Denied")
                        .font(.title)
                        .bold()
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Text("To scan the QR Code, you must enable Camera access for TUPlan in Settings")
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .rounded()
                .padding(.horizontal)
                
                Spacer()
            } else if permission == AVAuthorizationStatus.notDetermined {
                Spacer()
                VStack {
                    Text("Scan QR Code")
                        .font(.title)
                        .bold()
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Text("You may be asked for permission to use the camera")
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom)
                    
                    Button {
                        scannerState = "scanning"
                    } label: {
                        Label("Start Scan", systemSymbol: .qrcode)
                            .frame(maxWidth: .infinity)
                    }
                    .controlSize(.large)
                    .buttonStyle(.bordered)
                    .cornerRadius(14)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .rounded()
                .padding(.horizontal)
                
                Spacer()
            } else if scannerState == "info" {
                Spacer()
                VStack {
                    Text("Scan QR Code")
                        .font(.title)
                        .bold()
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Text("Press below to activate the camera")
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom)
                    
                    Button {
                        scannerState = "scanning"
                    } label: {
                        Label("Start Scan", systemSymbol: .qrcode)
                            .frame(maxWidth: .infinity)
                    }
                    .controlSize(.large)
                    .buttonStyle(.bordered)
                    .cornerRadius(14)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .rounded()
                .padding(.horizontal)
                
                Spacer()
            } else if scannerState == "scanning" {
                
            
            //if (!showScanError) {
                CodeScannerView(codeTypes: [.qr], simulatedData: "Failing String") { response in
                    // Format for QR Code
                    // otpauth://totp/TU-Darmstadt%202FA%20<TU-ID>?secret=<SECRET>&period=30&digits=6&issuer=Technische% 20Universit%C3%A4t%20Darmstadt&image=https%3A//login.tu-darmstadt.de/idp/images/logo.png
                    if case let .success(result) = response {
                        if !result.string.starts(with: "otpauth://totp/TU-Darmstadt") {
                            //showScanError = true
                            scannerState = "fail"
                            return
                        }
                        
                        if let range = result.string.range(of: "secret=") {
                            let substring = String(result.string.suffix(from: range.upperBound))
                            
                            if let range = substring.range(of: "&") {
                                let secret = String(substring.prefix(upTo: range.lowerBound))
                                
                                totpSecret = secret
                                loginFlowPage = 4
                                return
                            }
                        }
                    }
                }
                .padding(.horizontal)
                .padding(.bottom)
            } else {
                Spacer()
                VStack {
                    Text("Scan failed")
                        .font(.title)
                        .bold()
                        .frame(maxWidth: .infinity, alignment: .leading)
                    
                    Text("The Code you scanned doesn't seem to be a TOTP Code, please try again")
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom)
                    
                    Button {
                        //showScanError = false
                        scannerState = "scanning"
                    } label: {
                        Label("Try again", systemSymbol: .qrcode)
                            .frame(maxWidth: .infinity)
                    }
                    .controlSize(.large)
                    .buttonStyle(.bordered)
                    .cornerRadius(14)
                }
                .padding()
                .background(Color(.systemGroupedBackground))
                .rounded()
                .padding(.horizontal)
                
                Spacer()
            }
            
            Text("Or enter code manually")
                .padding(.bottom)
            
            TextField("TOTP Code", text: $totpSecret)
                .padding(.leading)
                .padding(.vertical, verticalPadding)  // different padding for iOS 26+
                //.foregroundStyle(isLoading ? .tertiary : .primary)
                .background(Color(.secondarySystemBackground))
                .rounded()
                .padding(.horizontal)
                .padding(.bottom)
                .focused($isFocused)
            
            BigBlueButton("Continue") {
                loginFlowPage = 4
                /*
                Task {
                    isLoading = true
                    await mfaLogin()
                    isLoading = false
                }
                */
            }
            .padding(.horizontal)
            
            Button("View setup guide again") {
                direction = .backward
                withAnimation {
                    loginFlowPage = 2
                }
            }
            .padding()
            
            Spacer()
                .frame(height: 38)
        }
        .onTapGesture {
            isFocused = false
        }
        //.disabled(isLoading)
        .navigationTitle("Scan QR Code")
    }
    
    /*
    private func mfaLogin() async {
        let status: Int = await identityManager.tuTotpLogin(totpSecret: totpSecret)
        
        if (status == 200 || status == 302) {
            isPresented = false
        } else {
            if (status == -4) {
                errorText = "Your secret couldn't be validated\nTry entering it again"
            } else if (status == -3) {
                errorText = "No token containing \"**TUPlan**\" exists. Please view the setup guide"
            } else if (status == -2) {
                errorText = "The SSO rejected your username or password.\nSince TUCaN accepted it, either try again, completely log out of the app or restart..."
            } else if (status == -1) {
                errorText = "An unknown error has occurred.\nThis could be due to your internet connection"
            } else {
                errorText = "The web request failed with Error code \(status)"
            }
        
            showErrorModal = true
        }
    }
    */
}

private struct TUConfirmTotpView: View {
    let username: String
    let password: String
    
    @Binding var totpSecret: String
    @Binding var isPresented: Bool
    
    @Binding var direction: NavigationDirection
    @Binding var loginFlowPage: Int
    
    @Binding var errorText: String
    @Binding var showErrorModal: Bool
    
    @EnvironmentObject var identityService: IdentityService
    
    @State private var isLoading: Bool = false
    
    var body: some View {
        VStack {
            TotpCodeCard(getTotpCodePair: {
                let kotlinPair = generateTotpPairFromBase32(base32Secret: totpSecret, timeMillis: currentTimeMillis())
                
                if let currentCode = kotlinPair?.first,
                   let nextCode = kotlinPair?.second {
                    return (current: String(currentCode), next: String(nextCode))
                } else {
                    return (current: "Error", next: "Error")
                }
            })
            .padding(.bottom, 32)
            
            VStack(alignment: .leading) {
                Text("Please enter this code on the Webpage")
                    .padding(.bottom)
                
                Spacer()
                
                Text("Only press this, once the page says\n\"Token successfully verified\"")
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal)
            .padding(.bottom)
            
            BigBlueButton("Finish") {
                Task {
                    isLoading = true
                    await mfaLogin()
                    isLoading = false
                }
            }
            .padding(.horizontal)
            
            Button("Enter a different secret") {
                direction = .backward
                withAnimation {
                    loginFlowPage = 3
                }
            }
            .padding()
            
            Spacer()
        }
        .navigationTitle("2FA Code")
        .background(Color(.systemGroupedBackground))
        .disabled(isLoading)
    }
    
    private func mfaLogin() async {
        let status: Int = await identityService.tuLogin(username: username, password: password, totpSecret: totpSecret)
        
        if (status == 200 || status == 302) {
            isPresented = false
        } else {
            if (status == -4) {
                errorText = "Your secret couldn't be validated\nTry entering it again"
            } else if (status == -3) {
                errorText = "No token containing \"**TUPlan**\" exists. Please view the setup guide"
            } else if (status == -2) {
                errorText = "The SSO rejected your username or password.\nSince TUCaN accepted it, either try again, completely log out of the app or restart..."
            } else if (status == -1) {
                errorText = "An unknown error has occurred.\nThis could be due to your internet connection"
            } else {
                errorText = "The web request failed with Error code \(status)"
            }
        
            showErrorModal = true
        }
    }
}

private struct TULoginView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            SettingsView()
        }
        .environmentObject(IdentityService())
        .environmentObject(KeyboardHeightHelper())
    }
}

#Preview("Credentials") {
    TUCredentialsView(username: Binding(get: {""}, set: { _ in}), password: Binding(get: {""}, set: { _ in}), showErrorModal: Binding(get: {false}, set: { _ in}), errorText: Binding(get: {""}, set: { _ in}), loginFlowPage: Binding(get: {1}, set: { _ in}), isPresented: Binding(get: {true}, set: { _ in}))
        .environmentObject(KeyboardHeightHelper())
}

#Preview("Information") {
    TUInformationView(isPresented: Binding(get: {true}, set: { _ in}), direction: Binding(get: {.forward}, set: { _ in}), loginFlowPage: Binding(get: {1}, set: { _ in}))
}

#Preview("Totp Instructions") {
    TUTotpInstructionsView(direction: Binding(get: {.forward}, set: { _ in}), loginFlowPage: Binding(get: {1}, set: { _ in}))
}

#Preview("Totp") {
    //TUTotpView(username: Binding(get: {""}, set: { _ in}), password: Binding(get: {""}, set: { _ in}), totpSecret: Binding(get: {""}, set: { _ in}), showErrorModal: Binding(get: {false}, set: { _ in}), errorText: Binding(get: {""}, set: { _ in}), direction: Binding(get: {.forward}, set: { _ in}), loginFlowPage: Binding(get: {1}, set: { _ in}), isPresented: Binding(get: {true}, set: { _ in}))
    TUTotpView(username: Binding(get: {""}, set: { _ in}), password: Binding(get: {""}, set: { _ in}), totpSecret: Binding(get: {""}, set: { _ in}), direction: Binding(get: {.forward}, set: { _ in}), loginFlowPage: Binding(get: {1}, set: { _ in}), isPresented: Binding(get: {true}, set: { _ in}))
}


