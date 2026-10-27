//  Created on 26.05.25.

import SwiftUI

struct ULBLoginView: View {
    let tuId = "ab12cdef"
    
    var body: some View {
        List {
            Section {
                Text("Logged in as: \(tuId)")
                Text("Log out")
                    .foregroundStyle(.red)
            } header: {
                Text("TU ID")
            }
            Section {
                Text("Logged in as: ULB")
                Text("Log out")
                    .foregroundStyle(.red)
            } header: {
                Text("ULB Account")
            }
        }
        .navigationTitle("Login with ULB Account")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct ULBLoginView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            ULBLoginView()
        }
        .environmentObject(TucanService())
        .environmentObject(KeyboardHeightHelper())
    }
}
