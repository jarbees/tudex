//  Created on 24.08.25.

import SwiftUI
import Shared


struct FoodDetailView: View {
    @EnvironmentObject private var mensaService: MensaService
    
    let menuItem: MenuItem
    let animation: Namespace.ID
    
    // Needed for dismiss action
    @Binding var isCardFullscreen: Bool
    
    // Tracks animation status for fading in content
    @Binding var cardFullscreenProgress: CGFloat
    
    // For swipe-back gesture
    @Binding var dragAmount: CGFloat
    private var dragProgress: CGFloat { dragAmount / FoodView.maxDragDistance }
    
    // For transitioning content, animates to true while content is appearing
    @State private var hasAppeared: Bool = false
    
    @Binding var fullscreenCornerRadius: CGFloat
    
    // feedback input
    @State private var tasteRating: Double = 0
    @State private var priceRating: Double = 0
    @State private var message: String = ""
    
    // isEditingFeedback should be used for view updates since it is animated
    // while feedbackEditorFocused is only for interacting with the focus of the Editor
    @State private var isEditingFeedback: Bool = false
    @FocusState private var feedbackEditorFocused: Bool
    
    @State private var isFeedbackLoading: Bool = false
    @State private var showDiscardFeedbackAlert: Bool = false
    
    private var hasUnsavedFeedback: Bool {
        return tasteRating != Double(menuItem.dish.feedback?.tasteRating ?? 0) ||
               priceRating != Double(menuItem.dish.feedback?.priceRating ?? 0) ||
               message.trimmingCharacters(in: .whitespacesAndNewlines) != (menuItem.dish.feedback?.message ?? "")
    }
    
    // allergy tooltip
    @State private var showAllergyTooltip: Bool = false
    
    @State private var showIncompleteFeedbackAlert: Bool = false
    
    private let canteenName: [Int : String] = [
        1: "Mensa Stadtmitte",
        2: "Mensa Lichtwiese",
        3: "Mensa Schöfferstraße",
        4: "Mensa Dieburg",
        5: "Bistro Haardtring",
        7: "Schöffers"
    ]
    
    var body: some View {
        ZStack(alignment: .topLeading) {
            ZStack(alignment: .topLeading) {
                VStack {
                    FoodDetailHero(menuItem: menuItem, animation: animation)
                    
                    ScrollView {
                        VStack(alignment: .leading) {
                            if (!isEditingFeedback) {
                                VStack(alignment: .leading) {
                                    DishDetails(menuItem: menuItem, showAllergyTooltip: $showAllergyTooltip)
                                    
                                    FeedbackRatings(tasteRating: $tasteRating, priceRating: $priceRating)
                                }
                                .transition(.move(edge: .top))
                            }
                            
                            TextEditor(text: $message)
                                .overlay(alignment: .topLeading) {
                                    Text("Something else you'd like to tell us? Please do! We are happy about any feedback.")
                                        .foregroundStyle(.tertiary)
                                        .padding(8)
                                        .padding(.leading, -4)
                                        .opacity(message.isEmpty ? 1 : 0)
                                }
                                .focused($feedbackEditorFocused)
                                .padding(8)
                                .frame(height: 150)
                                .scrollContentBackground(.hidden)
                                .background(Color(.systemGroupedBackground))
                                .rounded()
                                .overlay {
                                    // This makes the focused state manually managed
                                    Color.clear
                                        .contentShape(Rectangle())
                                        .onTapGesture {
                                            withAnimation {
                                                isEditingFeedback = true
                                            }
                                        }
                                    
                                }
                            // Enforce Character Limit of 255
                                .onChange(of: message) { newMessage in
                                    if newMessage.count > 255 {
                                        message = String(newMessage.prefix(255))
                                    }
                                }
                            
                            Text("\(255 - message.count) characters remaining")
                                .foregroundStyle(.tertiary)
                                .frame(maxWidth: .infinity, alignment: .trailing)
                                .padding(.bottom)
                            
                            BigBlueButton {
                                submitFeedback()
                            } content: {
                                Text("Send Feedback")
                                    .fontWeight(.semibold)
                                    .overlay(alignment: .trailing) {
                                        if isFeedbackLoading {
                                            ProgressView()
                                                .scaleEffect(0.7)
                                                .offset(x: 38)
                                        }
                                    }
                            }
                            .alert("Incomplete Feedback", isPresented: $showIncompleteFeedbackAlert) {
                                Button("OK") {}
                                    .keyboardShortcut(.defaultAction)
                            } message: {
                                Text("Feedback may only be sent if both taste and price were rated.")
                            }
                            .disabled(isFeedbackLoading || !hasUnsavedFeedback)
                            .padding(.bottom, isEditingFeedback ? 16 : 40)
                        }
                        .onChange(of: isEditingFeedback) { newFocusState in
                            feedbackEditorFocused = newFocusState
                        }
                        .padding(.horizontal)
                        .padding(.top, 40)
                        .opacity(hasAppeared ? 1 : 0)
                    }
                    .matchedGeometryEffect(id: FoodView.MatchedID.content, in: animation, properties: .position, anchor: .center)
                    .ignoresSafeArea()
                    .padding(.top, -40)     // This is needed to have a fading out effect for scrolling up
                    .disabled(dragAmount != 0)      // scrolling disabled when dragging
                    .mask {
                        LinearGradient(
                            gradient: Gradient(stops: [
                                .init(color: .clear, location: 0),
                                .init(color: .black, location: 0.1)
                            ]),
                            startPoint: .top,
                            endPoint: .bottom
                        )
                        .padding(.top, -40)
                    }
                }
                PriceTag(foodType: menuItem.dish.type, price: menuItem.dish.price, animation: animation)
                    .padding(.top)
            }
            .onAppear {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    hasAppeared = true
                }
            }
            .simultaneousGesture(dismissGesture)
            .disabled(isFeedbackLoading)
            .onTapGesture {
                withAnimation {
                    isEditingFeedback = false
                }
            }
            .scaleEffect(1 - dragProgress * 0.20)
            .padding(-2)
            .compositingGroup()     // Prevents layers artifacting/shimmering through at corners
            .background(
                Rectangle()
                    .fill(Color(.secondarySystemGroupedBackground))
                    .matchedGeometryEffect(id: FoodView.MatchedID.background, in: animation)
                    .ignoresSafeArea(.all, edges: .bottom)
            )
            
            CloseButton(closeAction: dismissFullscreen)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .padding(fullscreenCornerRadius == 0 ? -10 : 0)    // For iPhones with square corners
                .disabled(isFeedbackLoading)
        }
        .mask {
            FoodDetailMask(animation: animation, dragProgress: dragProgress, fullscreenCornerRadius: $fullscreenCornerRadius)
        }
        .overlayPreferenceValue(AllergyTooltipAnchorKey.self) { anchor in
            GeometryReader { proxy in
                if let anchor {
                    let frame = proxy[anchor]
                    
                    ZStack(alignment: .topLeading) {
                        Color.black
                            .opacity(showAllergyTooltip ? 0.4 : 0)
                            .ignoresSafeArea()
                            .onTapGesture {
                                withAnimation(.default.speed(1.2)) {
                                    showAllergyTooltip = false
                                }
                            }
                        
                        AllergyTooltip(menuItem: menuItem)
                            .padding(.leading, 14)
                            .scaleEffect(showAllergyTooltip ? 1 : 0, anchor: .leading)
                            .opacity(showAllergyTooltip ? 1 : 0)
                            .padding(.leading, frame.maxX)
                            .padding(.trailing)
                            .position(                      // position the center of tooltip to
                                x: proxy.size.width / 2,    // x: center of itself (no change)
                                y: frame.midY               // y: center of frame (from the text)
                            )
                    }
                }
            }
        }
        .onAppear {
            tasteRating = Double(menuItem.dish.feedback?.tasteRating ?? 0)
            priceRating = Double(menuItem.dish.feedback?.priceRating ?? 0)
            if let feedbackMsg = menuItem.dish.feedback?.message {
                message = feedbackMsg
            }
        }
        .alert("Discard Feedback?", isPresented: $showDiscardFeedbackAlert) {
            Button("Discard", role: .cancel) {
                closeFullscreen()
            }
            Button("Keep Editing") {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    dragAmount = 0
                }
            }
            .keyboardShortcut(.defaultAction)
        } message: {
            Text("You are about to return to overview but haven't sent your feedback yet.")
        }
        .ignoresSafeArea(.container, edges: .bottom)
    }
    
    private func submitFeedback() {
        if tasteRating != 0 && priceRating != 0 {
            Task {
                isFeedbackLoading = true
                defer { isFeedbackLoading = false }
                if let location = canteenName[Int(menuItem.issuingOffice.canteenId)] {
                    await mensaService.sendFeedback(tasteRating: Int(tasteRating), priceRating: Int(priceRating), message: message, location: location, dishId: Int(menuItem.dish.id))
                }
            }
        } else {
            showIncompleteFeedbackAlert = true
        }
    }
    
    private var dismissGesture: some Gesture {
        DragGesture()
            .onChanged { value in
                if (isEditingFeedback) {
                    withAnimation {
                        isEditingFeedback = false
                    }
                }
                
                if (showAllergyTooltip) {
                    withAnimation(.default.speed(1.2)) {
                        showAllergyTooltip = false
                    }
                }
                
                guard value.startLocation.x < FoodView.maxDragStartX else { return }
                
                let dragAmount = value.translation.width - FoodView.visualDragStartDistance
                
                if dragAmount >= FoodView.maxDragDistance {
                    dismissFullscreen()
                } else if dragAmount > 0 {
                    self.dragAmount = dragAmount
                }
            }
            .onEnded { value in
                guard value.startLocation.x < FoodView.maxDragStartX else { return }
                
                if value.translation.width > FoodView.minDismissDistance {
                    dismissFullscreen()
                } else {
                    dragAmount = 0
                }
            }
    }
    
    private func dismissFullscreen() {
        if hasUnsavedFeedback {
            showDiscardFeedbackAlert = true
        } else {
            closeFullscreen()
        }
    }
    
    private func closeFullscreen() {
        isCardFullscreen = false
        
        withAnimation(
            .spring(response: 0.4, dampingFraction: 0.8)
        ) {
            cardFullscreenProgress = 0
        }
    }
}


private struct FoodDetailHero: View {
    let menuItem: MenuItem
    let animation: Namespace.ID
    
    @ScaledMetric private var foodImageSize: CGFloat = 48
    @Environment(\.colorScheme) private var colorScheme
    @EnvironmentObject private var mensaService: MensaService
    
    var body: some View {
        let image: UIImage = menuItem.dish.image?.platformImage?.uiImage ?? UIImage(named: "UnknownFood")!
        
        ZStack(alignment: .bottomLeading) {
            FoodImage(image)
                .layoutPriority(1)      // This prevents the VStack from compressing from keyboard
                .matchedGeometryEffect(id: FoodView.MatchedID.image, in: animation)
            
            HStack {
                if menuItem.dish.bio {
                    Image("FoodBio")
                        .resizable()
                        .scaledToFit()
                        .frame(height: foodImageSize + 4)
                        .padding(.vertical, -2)
                }
                if menuItem.dish.mensa_vital {
                    if let vitalLogo = mensaService.vitalLogo {
                        SVGView(source: vitalLogo, alignment: .leading)
                            .frame(height: foodImageSize)
                            .shadow(color: .white, radius: 24)
                            .shadow(color: .white, radius: 48)
                            .shadow(color: .white, radius: 48)
                            .modify { view in
                                if colorScheme == .light {
                                    view.shadow(color: .white, radius: 48)
                                        .shadow(color: .white, radius: 48)
                                }
                            }
                    } else {
                        Text("MensaVital")
                    }
                }
            }
            .matchedGeometryEffect(id: FoodView.MatchedID.labels, in: animation)
            .padding(.leading)
            .padding(.bottom)
        }
    }
}

private struct DishDetails: View {
    let menuItem: MenuItem
    
    @Binding var showAllergyTooltip: Bool
    
    private var allergicsText: String {
        let allergics: [String] = FoodView.resolvedAllergics(for: menuItem)
        let additives: [String] = menuItem.dish.additionals
        
        let allergicsString = allergics.joined(separator: ", ")
        let additivesString = additives.joined(separator: ", ")
        
        let resultString = [allergicsString, additivesString]
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }     // remove empty entries
            .joined(separator: "), (")
        
        return "(\(resultString))"
    }
    
    var body: some View {
        Text(menuItem.dish.name)
            .font(.title)
            .bold()
        
        Text(allergicsText)
            .font(.caption)
            .anchorPreference(key: AllergyTooltipAnchorKey.self, value: .bounds) { $0 }
            .onTapGesture {
                withAnimation(.default.speed(1.2)) {
                    showAllergyTooltip = true
                }
            }
        Spacer()
            .frame(height: 16)
        
        Text(menuItem.issuingOffice.name)
            .padding(.bottom, 4)
        
        
        HStack(alignment: .center) {
            // Sadly, the API provides a default of rating = 3.0, which looks wrong
            if menuItem.dish.ratingCount == 0 {
                RatingStarsView(rating: .constant(0))
                VStack {
                    Text(" Ø 0 (0)")
                        .padding(.top, 2)
                }
            } else {
                RatingStarsView(rating: .constant(menuItem.dish.rating))
                VStack {
                    Text(" Ø \(String(format: "%.2f", menuItem.dish.rating)) (\(menuItem.dish.ratingCount))")
                        .padding(.top, 2)
                }
            }
        }
        .padding(.bottom, 32)
    }
}

private struct FeedbackRatings: View {
    @Binding var tasteRating: Double
    @Binding var priceRating: Double
    
    var body: some View {
        Text("Feedback")
            .font(.title2)
            .bold()
            .padding(.bottom, 8)
        
        Text("How did you like it?")
            .padding(.bottom, 4)
        
        RatingStarsView(rating: $tasteRating, isEditable: true)
            .font(.largeTitle)
            .padding(.bottom, 24)
        
        Text("Was the price reasonable?")
            .padding(.bottom, 4)
        
        RatingStarsView(rating: $priceRating, isEditable: true)
            .font(.largeTitle)
            .padding(.bottom, 24)
    }
}

private struct AllergyTooltipAnchorKey: PreferenceKey {
    static var defaultValue: Anchor<CGRect>?
    
    static func reduce(value: inout Anchor<CGRect>?, nextValue: () -> Anchor<CGRect>?) {
        value = nextValue() ?? value
    }
}

private struct AllergyTooltip: View {
    struct LeftTriangle: Shape {
        func path(in rect: CGRect) -> Path {
            var path = Path()
            
            path.move(to: CGPoint(x: rect.minX, y: rect.midY))           // left center
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.minY))        // top
            path.addLine(to: CGPoint(x: rect.maxX, y: rect.maxY))        // bottom
            path.closeSubpath()
            
            return path
        }
    }
    
    let menuItem: MenuItem
    
    let allergyMap: [String : String] = [
        "A": String(localized: "Gluten containing grains"),
        "A1": String(localized: "Wheat"),
        "A2": String(localized: "Spelt"),
        "A3": String(localized: "Rye"),
        "A4": String(localized: "Barley"),
        "A5": String(localized: "Oats"),
        "B": String(localized: "Crustaceans and crustacean products"),
        "C": String(localized: "Eggs and egg products"),
        "D": String(localized: "Fish and fish products"),
        "E": String(localized: "Peanuts and peanut products"),
        "F": String(localized: "Soybeans"),
        "G": String(localized: "Milk and milk products"),
        "H": String(localized: "Nuts"),
        "H1": String(localized: "Almonds"),
        "H2": String(localized: "Hazelnuts"),
        "H3": String(localized: "Walnuts"),
        "H4": String(localized: "Cashew nuts"),
        "H5": String(localized: "Pecan nuts"),
        "H6": String(localized: "Brazil nuts"),
        "H7": String(localized: "Pistachio nuts"),
        "H8": String(localized: "Macadamia nuts"),
        "I": String(localized: "Celery and celery products"),
        "J": String(localized: "Mustard and mustard products"),
        "K": String(localized: "Sesame and sesame products"),
        "L": String(localized: "Sulfur dioxides and sulfides"),
        "M": String(localized: "Lupines"),
        "N": String(localized: "Molluscs")
    ]
    
    let additivesMap: [String : String] = [
        "1": String(localized: "Food colouring"),
        "2": String(localized: "Preservatives"),
        "3": String(localized: "Antioxidants"),
        "4": String(localized: "Flavour enhancer"),
        "5": String(localized: "Sulfides"),
        "6": String(localized: "Blackened"),
        "7": String(localized: "Waxed"),
        "8": String(localized: "Phosphates"),
        "9": String(localized: "Sweeteners"),
        "10": String(localized: "Phenylalanine")
    ]
    
    private var allergics: [String] {
        FoodView.resolvedAllergics(for: menuItem)
    }
    
    var body: some View {
        VStack(alignment: .leading) {
            if !allergics.isEmpty {
                Text("Allergens")
                    .font(.title2)
                    .bold()
                
                Grid(alignment: .topLeading) {
                    ForEach(allergics, id: \.self) { allergic in
                        GridRow {
                            Text(allergic)
                                .bold()
                            
                            Text(allergyMap[allergic] ?? "Unknown")
                                .multilineTextAlignment(.leading)
                        }
                    }
                }
                .padding(.bottom, 8)
            }
            
            let additives = menuItem.dish.additionals
            
            if !additives.isEmpty {
                Text("Additives")
                    .font(.title2)
                    .bold()
                
                Grid(alignment: .topLeading) {
                    ForEach(additives, id: \.self) { additive in
                        GridRow {
                            Text(additive)
                                .bold()
                            
                            Text(additivesMap[additive] ?? "Unknown")
                                .multilineTextAlignment(.leading)
                        }
                    }
                }
            }
            
            if allergics.isEmpty && additives.isEmpty {
                Text("No allergens or additives listed")
                    .bold()
            }
        }
        .padding()
        .background {
            // the spacing is here to cover the case of no allergics/additives
            HStack(spacing: -4) {
                LeftTriangle()
                    .fill(Color(.secondarySystemGroupedBackground))
                    .frame(width: 14, height: 28)
                Rectangle()
                    .fill(Color(.secondarySystemGroupedBackground))
                    .rounded()
            }
            .padding(.leading, -10)
            .compositingGroup()
            .shadow(color: .black.opacity(0.2), radius: 8, x: 0, y: 4)
        }
    }
}

private struct PriceTag: View {
    let foodType: FoodType
    let price: Double
    let animation: Namespace.ID
    
    @ScaledMetric private var foodImageSize: CGFloat = 24
    
    var body: some View {
        VStack {
            HStack {
                HStack(spacing: 4) {
                    if foodType != .unknown {
                        FoodTypeIcon(type: foodType)
                            .frame(width: foodImageSize, height: foodImageSize)
                            .padding(.vertical, -2)
                    }
                    
                    Text(FoodView.formatDoubleToCurrency(price))
                        .font(.system(.subheadline, weight: .bold))
                }
                .padding(8)
                .background(Color(.secondarySystemBackground))
                .rounded(liquidCornerRadius: 12)
                .fixedSize(horizontal: true, vertical: false)
                .matchedGeometryEffect(id: FoodView.MatchedID.price, in: animation)
                
                Spacer()
            }
        }
        .padding(.leading)
        .padding(.top, 2)
    }
}

private struct FoodDetailMask: View {
    let animation: Namespace.ID
    let dragProgress: CGFloat
    @Binding var fullscreenCornerRadius: CGFloat
    
    @State private var animCornerRadius: CGFloat = 100
    
    private var cornerRadius: CGFloat {
        if #available(iOS 26.0, *) {
            18
        } else {
            8
        }
    }
    
    var body: some View {
        GeometryReader { geo in
            // The devices corners are circular, but continuous looks better, so prefer that when not fullscreen
            RoundedRectangle(cornerRadius: animCornerRadius, style: dragProgress == 0 ?  .circular : .continuous)
                .scaleEffect(x: 1 - dragProgress * 0.20,
                             y: 1 - dragProgress * (fullscreenCornerRadius == 0 ? 0.20 : 0.30))
                .matchedGeometryEffect(id: FoodView.MatchedID.mask, in: animation)
            
                .onAppear {
                    let topSafeAreaInset = geo.safeAreaInsets.top
                    if (topSafeAreaInset <= 20) {
                        fullscreenCornerRadius = 0
                    } else {
                        fullscreenCornerRadius = topSafeAreaInset - 8
                    }
                    animCornerRadius = cornerRadius
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.8)) {
                        animCornerRadius = fullscreenCornerRadius
                    }
                }
                .onChange(of: dragProgress) { newDragProgress in
                    animCornerRadius = fullscreenCornerRadius - newDragProgress * (fullscreenCornerRadius - cornerRadius)
                }
                .ignoresSafeArea()
        }
    }
}



/*
struct FoodDetailView_Previews: PreviewProvider {
    static var previews: some View {
        @Namespace var animation
        @State var isPresented: Bool = true
        @State var animationProgress: CGFloat = 1
        @State var dragAmount: CGFloat = 1
        @State var isCardFullscreen: Bool = true
        @State var fullscreenCornerRadius: CGFloat = 0
        
        NavigationStack {
            FoodDetailView(animation: animation, isCardFullscreen: $isCardFullscreen, showFullscreen: $isPresented, animationTracking: $animationProgress, dragAmount: $dragAmount, fullscreenCornerRadius: $fullscreenCornerRadius)
        }
    }
}
*/

#Preview {
    FoodView()
        .environmentObject(MensaService(previewMode: true))
}
