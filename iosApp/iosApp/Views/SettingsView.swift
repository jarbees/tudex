//  Created on 03.05.25.

import SwiftUI
import SFSafeSymbols

struct SettingsView: View {
    @EnvironmentObject var identityService: IdentityService
    @EnvironmentObject var settingsService: SettingsService
    @EnvironmentObject var mensaService: MensaService
    
    @State private var demoClickNumber: Int = 0
    
    @State private var username: String = ""
    @State private var password: String = ""
    
    @State private var isSheetPresented = false
    @State private var skipBasicLogin = false
    @State private var showLogoutModal: Bool = false
    @State private var showTotpForgetModal: Bool = false
    
    @State private var excludedAllergies: [String] = []
    @State private var excludedFoodTypes: [String] = []
    
    private let canteenSelectorList: [(id: Int, title: String, shortTitle: String)] = [
            (0, String(localized: "Last Viewed Canteen"), String(localized: "Last Viewed")),
            (1, "Mensa Stadtmitte", "Stadtmitte"),
            (2, "Mensa Lichtwiese", "Lichtwiese"),
            (3, "Mensa Schöfferstraße", "Schöfferstraße"),
            (4, "Mensa Dieburg", "Dieburg"),
            (5, "Bistro Haardtring", "Haardtring"),
            (7, "Campusrestaurant Schöffers", "Schöffers")
    ]
    
    private let statusSelectorList: [(String, String, String?)] = [
        ("STUDENT", String(localized: "Student"), nil),
        ("GUEST", String(localized: "Guest"), nil)
    ]
    
    private let menuLangSelectorList: [(String, String, String?)] = [
        ("DE", String(localized: "German"), nil),
        ("EN", String(localized: "English"), nil)
    ]
    
    var body: some View {
        List {
            Section {
                NavigationLink("App Icon", destination: AppIconPickerView())
            } header: {
                Text("Customization")
            }
            
            Section {
                NavigationLink("Food Types", destination: FoodTypesView())
                NavigationLink("Allergies", destination: AllergiesView())
                
                ListSelectorMenu(title: "Status", itemMap: statusSelectorList, selection: mensaService.status) { newStatus in
                    Task {
                        await mensaService.updateStatus(to: newStatus)
                    }
                }
                ListSelectorMenu(title: "Menu Language", itemMap: menuLangSelectorList, selection: mensaService.lang) { newLang in
                    Task {
                        await mensaService.updateLang(to: newLang)
                    }
                }
                ListSelectorMenu(title: "Default Location", itemMap: canteenSelectorList, selection: mensaService.defaultCanteen) { newDefaultCanteen in
                    Task {
                        await mensaService.updateDefaultCanteen(to: newDefaultCanteen)
                    }
                }
            } header: {
                Text("Food Settings")
            }
            
            Section {
                if (identityService.tuLoginStatus == .sso) {
                    Text("Logged in as: \(identityService.username) (SSO Login)")
                    Button("Log out") {
                        showLogoutModal = true
                    }
                    .foregroundStyle(.red)
                } else if (identityService.tuLoginStatus == .mfa) {
                    Text("Logged in as: \(identityService.username) (2FA Login)")
                    Button("Log out") {
                        showLogoutModal = true
                    }
                    .foregroundStyle(.red)
                } else if (identityService.tuLoginStatus == .demo) {
                    Text("Logged in as Demo User")
                    Button("Log out") {
                        showLogoutModal = true
                    }
                    .foregroundStyle(.red)
                } else if (identityService.tuLoginStatus == .mfaTucanError) {
                    Text("\(identityService.username) (2FA Login, TUCaN broken)")
                    Button("Log out") {
                        showLogoutModal = true
                    }
                    .foregroundStyle(.red)
                } else {
                    TULoginController(isSheetPresented: $isSheetPresented, skipBasicLogin: $skipBasicLogin) { loginAction, isLoading in
                        Button  {
                            let oathHelper = OAuthViewModel()
                            oathHelper.startLogin(exchangeCodeForTokens: { code, verifier, nonce in
                                Task {
                                    await identityService.tucanLogin(code: code, codeVerifier: verifier, nonce: nonce)
                                }
                            })
                        } label: {
                            HStack {
                                Text("Login with SSO")
                                    .foregroundStyle(Color.accentColor)
                                Spacer()
                                if (isLoading) {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle())
                                }
                            }
                        }
                    }
                    
                    if settingsService.isMfaLoginAvailable {
                        TULoginController(isSheetPresented: $isSheetPresented, skipBasicLogin: $skipBasicLogin, isMfa: true) { loginAction, isLoading in
                            Button  {
                                loginAction()
                            } label: {
                                HStack {
                                    Text("Login with Username/Password")
                                        .foregroundStyle(Color.accentColor)
                                    Spacer()
                                    if (isLoading) {
                                        ProgressView()
                                            .progressViewStyle(CircularProgressViewStyle())
                                    }
                                }
                            }
                        }
                    }
                    if settingsService.isDemoLoginAvailable {
                        Button  {
                            identityService.tuDemoLogin()
                        } label: {
                            HStack {
                                Text("Login for Demo Mode")
                                    .foregroundStyle(Color.accentColor)
                            }
                        }
                    }
                }
                
            } header: {
                Text("TU ID")
                    .onLongPressGesture(minimumDuration: 12) {
                        settingsService.updateMfaLoginAvailability(to: !settingsService.isMfaLoginAvailable)
                    }
            } footer: {
                #if !DEBUG
                    BuildNumberText()
                #endif
            }
            
            /*
            Section {
                Text("Logged in as: ULB")
                Text("Log out")
                    .foregroundStyle(.red)
            } header: {
                Text("ULB Account")
            }
            */
            
            #if DEBUG
            Section {
                Button {
                    settingsService.switchUiType()
                } label: {
                    if #available(iOS 18.0, *) {
                        Label("Switch to Android UI", systemSymbol: .gearshapeArrowTrianglehead2ClockwiseRotate90)
                    } else {
                        Label("Switch to Android UI", systemSymbol: .gearshapeArrowTriangle2Circlepath)
                    }
                }
            } header: {
                Text("Debug")
            } footer: {
                BuildNumberText()
            }
            #endif
        }
        .alert("Confirm Logout", isPresented: $showLogoutModal) {
            Button("Logout") {
                showLogoutModal = false
                if (identityService.tuLoginStatus == .mfa ||
                    identityService.tuLoginStatus == .mfaUnconfirmed)
                {
                    showTotpForgetModal = true
                }
                Task {
                    await identityService.logout()
                }
            }
            .keyboardShortcut(.defaultAction)
            Button("Cancel", role: .cancel) {
                showLogoutModal = false
            }
        }
        
        .alert("Remove 2FA Secret?", isPresented: $showTotpForgetModal) {
            Button("Keep") {
                showTotpForgetModal = false
            }
            .keyboardShortcut(.defaultAction)
            Button("Remove", role: .cancel) {
                identityService.resetTotpSecret()
                showTotpForgetModal = false
            }
        } message: {
            Text("**WARNING**\nYou won't be able to use 2FA until you add another TOTP Secret\n\nSelect \"Keep\" if you only want to log out.")
        }
        
        // "[skipBasicLogin] in" makes TULoginView update correctly, see https://stackoverflow.com/a/77169574/259521
        .sheet(isPresented: $isSheetPresented) { [skipBasicLogin] in
            TULoginView(isPresented: $isSheetPresented, skipBasicLogin: skipBasicLogin)
        }
        .navigationTitle("Settings")
    }
}

private struct ListSelectorMenu<SelectionType: Hashable>: View {
    @State private var selection: SelectionType
    
    let title: LocalizedStringKey
    let itemMap: [(id: SelectionType, title: String, shortTitle: String?)]
    //let selection: SelectionType
    let onChange: (SelectionType) -> Void
    
    init(title: LocalizedStringKey, itemMap: [(id: SelectionType, title: String, shortTitle: String?)], selection: SelectionType, onChange: @escaping (SelectionType) -> Void) {
        self.selection = selection
        self.title = title
        self.itemMap = itemMap
        self.onChange = onChange
    }
    
    var body: some View {
        HStack {
            Text(title)
            Spacer()
            BetterMenu {
                Picker(title, selection: $selection) {
                    ForEach(itemMap, id: \.id) { (id, title, shortTitle) in
                        Text(title).tag(id)
                    }
                }
                .onChange(of: selection) { newSelection in
                    onChange(newSelection)
                }
            } label: {
                HStack {
                    if let item = itemMap.first(where: { $0.id == selection }) {
                        if let shortTitle = item.shortTitle {
                            Text(shortTitle)
                        } else {
                            Text(item.title)
                        }
                    } else {
                        Text("Unknown")
                    }
                    Image(systemSymbol: .chevronUpChevronDown)
                }
                .foregroundStyle(Color(.secondaryLabel))
            }
        }
    }
}

private struct BuildNumberText: View {
    @EnvironmentObject private var identityService: IdentityService
    @EnvironmentObject private var settingsService: SettingsService
    @State private var demoClickNumber: Int = 0
    
    var body: some View {
        VStack {
            Text("TUDex v\(Bundle.main.releaseVersionNumber ?? "Error") build \(Bundle.main.buildVersionNumber ?? "Error")")
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .onTapGesture {
            if (identityService.tuLoginStatus == .none) {
                if (demoClickNumber < 9) {
                    demoClickNumber += 1
                } else {
                    demoClickNumber = 0
                    settingsService.updateDemoLoginAvailability(to: !settingsService.isDemoLoginAvailable)
                }
            }
        }
    }
}


#Preview {
    NavigationView {
        SettingsView()
    }
    .environmentObject(IdentityService(previewMode: true))
    .environmentObject(TucanService())
    .environmentObject(SettingsService())
    .environmentObject(MensaService(previewMode: true))
}

/*
struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            SettingsView()
        }
        .environmentObject(TucanService())
    }
}
*/
