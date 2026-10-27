//  Created on 03.05.25.

import SwiftUI

struct BookView: View {
    var body: some View {
        VStack {
            Image(systemSymbol: .booksVerticalFill)
                .resizable()
                .scaledToFit()
                .frame(width: 70, height: 70)
            Text("Coming in a future update")
                .font(.largeTitle)
                .bold()
                .multilineTextAlignment(.center)
        }
        .foregroundStyle(.tertiary)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}


#Preview {
    NavigationStack {
        BookView()
    }
}
