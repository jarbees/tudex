//  Created on 05.07.25.

import SwiftUI

struct HeaderText: View {
    let key: LocalizedStringKey
    
    init(_ key: LocalizedStringKey = "") {
        self.key = key
    }
    
    var body: some View {
        if #available(iOS 26.0, *) {
            Text(key)
                .padding(.leading, 32)
                .font(.headline)
                .bold()
                .padding(.top, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .foregroundStyle(.secondary)
        } else {
            Text(key)
                .textCase(.uppercase)
                .padding(.leading, 32)
                .font(.footnote)
                .padding(.top, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .foregroundStyle(.secondary)
        }
    }
}

struct HeaderTextNotLocal: View {
    let text: String
    
    init(_ text: String = "") {
        self.text = text
    }
    
    var body: some View {
        if #available(iOS 26.0, *) {
            Text(text)
                .padding(.leading, 32)
                .font(.headline)
                .bold()
                .padding(.top, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .foregroundStyle(.secondary)
        } else {
            Text(text)
                .textCase(.uppercase)
                .padding(.leading, 32)
                .font(.footnote)
                .padding(.top, 8)
                .frame(maxWidth: .infinity, alignment: .leading)
                .foregroundStyle(.secondary)
        }
    }
}

struct FooterText: View {
    let text: LocalizedStringKey
    
    init(_ text: LocalizedStringKey = "") {
        self.text = text
    }
    
    var body: some View {
        Text(text)
            .padding(.leading, 32)
            .font(.footnote)
            .padding(.bottom, 8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .foregroundStyle(.secondary)
    }
}

#Preview {
    VStack {
        Text("Custom")
            .bold()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading)
        
        HeaderText("Header")
        
        Rectangle()
            .fill(Color(.secondarySystemGroupedBackground))
            .frame(height: 100)
            .rounded()
            .padding(.horizontal)
        
        FooterText("Footer")
        
        Text("Standard")
            .bold()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.leading)
        
        List {
            Section {
                Rectangle()
                    .fill(Color.clear)
                    .frame(height: 68)
                    .rounded()
                    .padding(.horizontal)
            } header: {
                Text("Header")
            } footer: {
                Text("Footer")
            }
        }
    }
    .background(Color(.systemGroupedBackground))
}
