//  Created on 29.10.25.

import SwiftUI

struct TestFoodView: View {
    @Environment(\.colorScheme) var colorScheme
    @State private var showOverlay: Bool = false
    
    @Namespace private var animation
    
    var body: some View {
        ZStack(alignment: .top) {
            NavigationStack {
                ZStack {
                    ScrollView {
                        VStack {
                            if !showOverlay {
                                Image("DummyImage")
                                    .resizable()
                                    .scaledToFit()
                                    .matchedGeometryEffect(id: "Image", in: animation)
                                    .transition(.offset())
                                    .zIndex(0)
                            }
                            
                            Spacer()
                                .frame(height: 400)
                            
                            
                        }
                        .frame(maxWidth: .infinity)
                        .zIndex(showOverlay ? 3 : 2)
                    }
                }
                .navigationTitle("TestView")
                .toolbar {
                    ToolbarItem(placement: .bottomBar) {
                        Button("Wow") {
                            withAnimation {
                                showOverlay.toggle()
                            }
                        }
                    }
                }
            }
            .overlay {
                VStack {
                    if showOverlay {
                        Image("DummyImage")
                            .resizable()
                            .scaledToFit()
                            .matchedGeometryEffect(id: "Image", in: animation)
                            .transition(.offset())
                    }
                    
                    Spacer()
                }
                .frame(maxHeight: .infinity)
            }
        }
    }
}


#Preview {
    TestFoodView()
        .environmentObject(MensaService(previewMode: true))
}
