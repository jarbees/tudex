//  Created on 12.12.25.

import SwiftUI

struct RequestLoadingView: View {
    let itemName: LocalizedStringKey
    
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

struct LoginLoadingView: View {
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

#Preview {
    TabView {
        NavigationStack {
            RequestLoadingView(itemName: "Courses")
                .navigationTitle("RandomTitle")
                .frame(maxWidth: .infinity)
                .background(Color(.secondarySystemBackground))
        }
        .tabItem {
            Label("Settings", systemSymbol: .book)
        }
        
        Spacer()
            .tabItem {
                Label("Map", systemSymbol: .map)
            }
        
        Spacer()
            .tabItem {
                Label("Kamera", systemSymbol: .camera)
            }
        
        Spacer()
            .tabItem {
                Label("Settings", systemSymbol: .book)
            }
    }
    .tabViewStyle(.automatic)
}
