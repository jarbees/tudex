//  Created on 12.12.25.

import SwiftUI
import Shared
import SFSafeSymbols

/// A View used for cases in which an item list is empty
struct ContentStatusView: View {
    @EnvironmentObject private var identityService: IdentityService
    
    let status: RequestStatus
    let itemName: LocalizedStringResource
    let refresh: () async -> Void
    
    var body: some View {
        if (identityService.tuLoginStatus == .mfaUnconfirmed ||
            identityService.tuLoginStatus == .ssoUnconfirmed
        ) {
            LoginLoadingView()
        } else if status == .loading {
            RequestLoadingView(itemName: itemName)
        } else if status == .failed {
            FailedLoadingView(itemName: itemName, refresh: refresh)
        } else {
            EmptyResultView(itemName: itemName)
        }
    }
}

private struct RequestLoadingView: View {
    let itemName: LocalizedStringResource
    
    var body: some View {
        VStack {
            Spacer()
            ProgressView()
                .scaleEffect(1.8)
                .padding(.bottom, 8)
            (Text("Loading ") + Text(itemName))
                .font(.title2)
                .bold()
                .foregroundStyle(.secondary)
                .padding(.bottom, 32)
            Spacer()
        }
    }
}

private struct LoginLoadingView: View {
    var body: some View {
        VStack {
            Spacer()
            ProgressView()
                .scaleEffect(1.8)
                .padding(.bottom, 8)
            Text("Logging in with TU ID")
                .font(.title2)
                .bold()
                .foregroundStyle(.secondary)
                .padding(.bottom, 32)
            Spacer()
        }
    }
}

private struct FailedLoadingView: View {
    let itemName: LocalizedStringResource
    let refresh: () async -> Void
    
    var body: some View {
        VStack {
            Spacer()
            
            var symbol: SFSymbol {
                if #available(iOS 18.0, *) {
                    .exclamationmarkArrowTrianglehead2ClockwiseRotate90
                } else {
                    .exclamationmarkArrowTriangle2Circlepath
                }
            }
            
            Image(systemSymbol: symbol)
                .resizable()
                .scaledToFit()
                .frame(width: 44, height: 44)
                .padding(.bottom, 8)
                .foregroundStyle(.secondary)
            
            Text("Loading \(itemName) failed")
                .font(.title2)
                .bold()
                .padding(.bottom, 8)
            
            Button(action: {
                Task {
                    await refresh()
                }
            }, label: {
                Text("Try again")
            })
            
            .padding(.bottom)
            Spacer()
        }
        .padding(.horizontal)
    }
}

private struct EmptyResultView: View {
    let itemName: LocalizedStringResource
    
    var body: some View {
        if #available(iOS 17.0, *) {
            ContentUnavailableView("No \(itemName) to display", systemSymbol: .squareDashed)
        } else {
            VStack {
                Spacer()
                
                Image(systemSymbol: .squareDashed)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 44, height: 44)
                    .padding(.bottom, 8)
                    .foregroundStyle(.secondary)
                
                Text("No \(itemName) to display")
                    .font(.title2)
                    .bold()
                    .padding(.bottom, 8)
                
                Spacer()
            }
            .padding(.horizontal)
        }
    }
}


private struct NavigationLoadingModifier: ViewModifier {
    @EnvironmentObject private var identityService: IdentityService

    let title: LocalizedStringResource
    let itemStatus: RequestStatus
    let itemIsEmpty: Bool
    let itemName: LocalizedStringResource
    let refreshAction: () async -> Void
    
    @State private var showAlert = false

    @ViewBuilder
    func body(content: Content) -> some View {
        content
            .navigationTitleEnhanced(title, accessory: { isLarge in
                HStack {
                    if !itemIsEmpty {
                        if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                            identityService.tuLoginStatus == .ssoUnconfirmed ||
                            itemStatus == .loading
                        ) {
                            ProgressView()
                                .scaleEffect(isLarge ? 1.3 : 1)
                                .padding(.top, isLarge ? 2 : 0)
                        } else if (itemStatus == .failed) {
                            Button(action: {
                                showAlert = true
                            }, label: {
                                if #available(iOS 18.0, *) {
                                    Image(systemSymbol: .exclamationmarkArrowTrianglehead2ClockwiseRotate90)
                                        .resizable()
                                        .scaledToFit()
                                } else {
                                    Image(systemSymbol: .exclamationmarkArrowTriangle2Circlepath)
                                        .resizable()
                                        .scaledToFit()
                                }
                            })
                            .frame(height: isLarge ? 24 : 16)
                            .padding(.top, isLarge ? 2 : 0)
                        }
                    }
                }
            })
            .alert("Refresh failed", isPresented: $showAlert, actions: {
                Button("Cancel") {
                    showAlert = false
                }
                
                Button("Retry") {
                    Task {
                        await refreshAction()
                    }
                }
                .keyboardShortcut(.defaultAction)
            }, message: {
                Text("\(Text(itemName)) couldn't be loaded.")
            })
    }
}

extension View {
    func navigationLoadingTitle(
        title: LocalizedStringResource,
        itemStatus: RequestStatus,
        itemIsEmpty: Bool,
        itemName: LocalizedStringResource,
        refreshAction: @escaping () async -> Void
    ) -> some View {
        return self.modifier(NavigationLoadingModifier(title: title, itemStatus: itemStatus, itemIsEmpty: itemIsEmpty, itemName: itemName, refreshAction: refreshAction))
    }
}

/// A View that asks the User to complete 2FA Login. Mustn't be shown if user doesn't have basic Login
struct LoginLockedView: View {
    @State private var isSheetPresented = false
    
    var body: some View {
        VStack {
            Spacer()
            Image(systemSymbol: .lockFill)
                .resizable()
                .scaledToFit()
                .frame(width: 44, height: 44)
                .padding(.bottom)
            
            Text("This section requires 2FA login")
                .font(.title2)
                .fontWeight(.semibold)
                .padding(.bottom)
            
            BigBlueButton("Login with TU ID") {
                isSheetPresented = true
            }
            .padding(.bottom)
            Spacer()
        }
        .padding(.horizontal)
        .sheet(isPresented: $isSheetPresented) {
            TULoginView(isPresented: $isSheetPresented, skipBasicLogin: true)
        }
    }
}

#Preview("Item Loading") {
    NavigationStack {
        RequestLoadingView(itemName: "Courses")
            .navigationTitle("RandomTitle")
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground))
    }
}

#Preview("Login Loading") {
    NavigationStack {
        LoginLoadingView()
            .navigationTitle("RandomTitle")
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground))
    }
}

#Preview("Failed") {
    NavigationStack {
        FailedLoadingView(itemName: "Courses", refresh: {})
            .navigationTitle("RandomTitle")
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground))
    }
}

#Preview("Empty") {
    NavigationStack {
        EmptyResultView(itemName: "Courses")
            .navigationTitle("RandomTitle")
            .frame(maxWidth: .infinity)
            .background(Color(.secondarySystemBackground))
    }
}
