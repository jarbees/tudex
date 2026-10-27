//  Created on 07.09.25.

import SwiftUI
import Shared

extension Collection {
    subscript(safe index: Index) -> Element? {
        return indices.contains(index) ? self[index] : nil
    }
}

struct TeaserDetailView: View {
    @EnvironmentObject private var mensaService: MensaService
    
    let animation: Namespace.ID
    
    @Binding var teaserFullscreenProgress: CGFloat
    @Binding var isTeaserFullscreen: Bool
    @Binding var selectedTeaserTab: Int
    @Binding var animatedTeaserIndex: Int?
    
    
    var body: some View {
        if let animatedTeaserIndex = animatedTeaserIndex {
            ZStack(alignment: .topTrailing) {
                TabView(selection: $selectedTeaserTab) {
                    ForEach(Array(mensaService.teasers.enumerated()), id: \.element.id) { index, teaser in
                        let isImageVisible: Bool =
                        isTeaserFullscreen ||
                        selectedTeaserTab != index ||
                        (animatedTeaserIndex != index && teaserFullscreenProgress == 1)
                        
                        TeaserDetailCell(teaser: teaser, index: index, animation: animation, teaserFullscreenProgress: teaserFullscreenProgress, isImageVisible: isImageVisible, dismissFullscreen: dismissFullscreen)
                            .tag(index)
                    }
                }
                .tabViewStyle(.page)
                
                if teaserFullscreenProgress == 1 {
                    CloseButton(closeAction: dismissFullscreen, invertedColor: true)
                }
            }
            .ignoresSafeArea()
            
            .simultaneousGesture(
                DragGesture()
                    .onEnded { value in
                        let dragDistance = value.translation.width
                        
                        if (value.startLocation.x < 20 && dragDistance > 50 && value.velocity.width > 300) {
                            dismissFullscreen()
                        }
                    }
            )
        }
    }
    
    private func dismissFullscreen() {
        animatedTeaserIndex = selectedTeaserTab
        isTeaserFullscreen = false
        
        withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
            teaserFullscreenProgress = 0
        }
    }
}

private struct TeaserDetailCell: View {
    @Environment(\.colorScheme) private var colorScheme
    
    let teaser: Teaser
    let index: Int
    let animation: Namespace.ID
    let teaserFullscreenProgress: CGFloat
    let isImageVisible: Bool
    
    let dismissFullscreen: () -> Void
    
    var body: some View {
        ScrollView {
            VStack {
                if let image = teaser.image.platformImage?.uiImage {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                        .rounded()
                        .matchedGeometryEffect(id: "TeaserFullscreen \(index)", in: animation)
                        .padding(.horizontal)
                        .padding(.bottom)
                        .zIndex(1)  // Put it over description for animation
                        .opacity(isImageVisible ? 1 : 0)
                        // removes opacity animations
                        .transaction { transaction in
                            transaction.animation = nil
                        }
                }
                
                VStack {
                    if teaser.__typename == "InAppTeaser" {
                        VStack {
                            if let title = teaser.detailTitle, !title.isEmpty {
                                Text(title)
                                    .bold()
                                    .padding(.bottom)
                            }
                            
                            VStack(alignment: .leading) {
                                if let infoTitle = teaser.infoTitle, !infoTitle.isEmpty {
                                    Text(infoTitle)
                                        .bold()
                                        .padding(.bottom, 4)
                                }
                                
                                if let infoText = teaser.infoText, !infoText.isEmpty {
                                    Text(infoText)
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding()
                            .background(Color.accentColor.opacity(colorScheme == .dark ? 0.6 : 0.4))
                            .rounded()
                        }
                    } else {
                        if let url = URL(string: teaser.url ?? "") {
                            Button(action: {
                                UIApplication.shared.open(url)
                            }) {
                                Label("Open External Link", systemImage: "link")
                                    .padding(.vertical, 8)
                                    .frame(maxWidth: .infinity)
                                    .fontWeight(.semibold)
                            }
                            .buttonStyle(BorderedProminentButtonStyle())
                            .glassEffectIfAvailable()
                            .cornerRadius(14)
                        }
                        
                    }
                }
                .padding(.horizontal)
                .padding(.bottom)
                .offset(y: teaserFullscreenProgress == 1 ? 0 : -100)
                .opacity(teaserFullscreenProgress == 1 ? 1 : 0)
            }
        }
        .scrollIndicators(.hidden)
        .onTapGesture {
            dismissFullscreen()
        }
    }
}

#Preview {
    FoodView()
        .environmentObject(MensaService(previewMode: true))
}
