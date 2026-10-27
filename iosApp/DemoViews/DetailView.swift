//  Created on 03.05.25.

import SwiftUI

struct DetailView: View {
    var body: some View {
        ScrollView {
            Text("Detail View")
                .font(.largeTitle)
                .navigationTitle("Detail")
            let colors: [UIColor] = [
                .label, .secondaryLabel, .tertiaryLabel, .quaternaryLabel, .systemFill, .secondarySystemFill, .tertiarySystemFill,
                .quaternarySystemFill, .placeholderText, .tintColor, .systemBackground, .secondarySystemBackground, .tertiarySystemBackground,
                .systemGroupedBackground, .tertiarySystemGroupedBackground, .separator, .opaqueSeparator, .link, .darkText, .lightText
            ]
            
            
            VStack(spacing: 20) {
                ForEach(colors, id: \.self) { color in
                    HStack {
                        Rectangle()
                            .fill(Color(color)) // Convert UIColor to SwiftUI Color
                            .frame(height: 100)
                            .cornerRadius(10)
                        Text("\(color.description)") // Display UIColor description
                            .font(.caption)
                            .padding(.leading, 10)
                    }
                }
            }
            .padding()
        }
    }
}
