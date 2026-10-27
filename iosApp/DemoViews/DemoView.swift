//  Created on 12.05.25.

import SwiftUI

struct DemoView: View {
    @State private var offset: CGFloat = 0
    
    var body: some View {
        VStack {
            Rectangle()
                .fill(Color.blue)
                .frame(width: 100, height: 100)
                .offset(y: offset) // Apply the offset to move the rectangle
                .animation(.easeInOut(duration: 0.3), value: offset) // Animate the offset change
            
            Button(action: {
                // Move the rectangle up by changing the offset
                offset -= 100 // Adjust this value to control how far it moves up
            }) {
                Text("Move Up")
                    .padding()
                    .background(Color.gray)
                    .foregroundStyle(.white)
                    .cornerRadius(8)
            }
            Button(action: {
                // Move the rectangle up by changing the offset
                offset += 100 // Adjust this value to control how far it moves up
            }) {
                Text("Move Down")
                    .padding()
                    .background(Color.gray)
                    .foregroundStyle(.white)
                    .cornerRadius(8)
            }
        }
        .background(.yellow)
        .padding()
        .navigationTitle("Demo")
    }
}


private struct DemoView2: View {
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack {
                    Text("Hello")
                    Rectangle()
                        .frame(height: 1000)
                }
                .navigationTitle(".")
                .navigationTitleAccessory {
                    Text("Small")
                } large: {
                    Text("Large")
                }

            }
        }
    }
}

#Preview {
    DemoView2()
}
