//  Created on 26.10.25.

import SwiftUI
import Shared
import SFSafeSymbols

struct FoodExtras: View {
    @EnvironmentObject private var mensaService: MensaService
    @StateObject private var nfcManager: NFCManager = NFCManager()

    var body: some View {
        ScrollView {
            VStack {
                HeaderText("Balance")
                    .padding(.leading, -16)
                VStack {
                    VStack {
                        Text("\(mensaService.cardBalance)€")
                            .font(.largeTitle)
                            .bold()
                        Text("(\(mensaService.cardLastScanDate) at \(mensaService.cardLastScanTime))")
                            .font(.subheadline)
                            .foregroundStyle(Color(.secondaryLabel))
                    }
                    .padding(.vertical)
                    
                    BigBlueButton("Scan Card") {
                        nfcManager.startScan(onSuccess: { cardID, cardBalance, cardLastBalance in
                            mensaService.updateCard(cardID: cardID, cardBalance: cardBalance, cardLastBalance: cardLastBalance)
                        })
                    }
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(Color(.secondarySystemGroupedBackground))
                .rounded()
                //.padding(.bottom, 8)
                
                Text("Last charge: \(mensaService.cardLastBalance)€")
                    .font(.subheadline)
                    .foregroundStyle(Color(.secondaryLabel))
                    .padding(.bottom)
                
                HeaderText("Information")
                    .padding(.leading, -16)
                    .padding(.bottom, 0.01)     // intrinsic padding is ignored without this (?)
                
                GroupedList(padding: 0) {
                    ListNavigationLink(text: String(localized: "Locations"), view: {
                        LocationView()
                    })
                     
                    // "Impressum" and "Datenschutz" contain MensaApp specific information that probably does not apply
                    let filteredInfoItems = mensaService.infoItems.filter {
                        $0.title != "Impressum" && $0.title != "Datenschutz"
                    }
                    
                    ForEach(filteredInfoItems, id: \.id) { infoItem in
                        ListNavigationLink(text: infoItem.title, view: {
                            ScrollView {
                                HTMLTextView(html: infoItem.content)
                                    .modify { view in
                                        // This info specifically has padding in the html
                                        if infoItem.title != "Allergene und Zusatzstoffe" {
                                            view.padding(.horizontal)
                                        }
                                    }
                            }
                            .navigationTitle(infoItem.title)
                        })
                    }
                }
            }
            .padding(.horizontal)
            .padding(.bottom)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Overview")
        .navigationBarTitleDisplayMode(.inline)
    }
}


extension FoodExtras {
    struct ListNavigationLink<Content: View>: View {
        var text: String
        var view: () -> Content
        
        var body: some View {
            NavigationLinkButton {
                HStack {
                    Text(text)
                        .padding(.vertical)
                        .font(.subheadline)
                    Spacer()
                    Image(systemSymbol: .chevronRight)
                        .foregroundStyle(Color(.tertiaryLabel))
                        .font(.system(size: 14, weight: .semibold))
                        .padding(.trailing)
                }
                .frame(maxWidth: .infinity)
            } destination: {
                view()
            }
        }
    }
    
    struct LocationView: View {
        @EnvironmentObject private var mensaService: MensaService
        @Environment(\.colorScheme) var colorScheme
        @Namespace private var animation
        @State private var selectedLocationID: Int = -1
        @State private var showFullscreen: CGFloat = 0
        
        
        var body: some View {
            ScrollView {
                let locations = mensaService.locations
                let types: [(type: LocationType, title: String)] = [
                    (LocationType.bistro, String(localized: "Bistros")),
                    (LocationType.kaffeebar, String(localized: "Coffee bars")),
                    (LocationType.biergarten, String(localized: "Beer garden")),
                    (LocationType.mensa, String(localized: "Canteens"))
                ]
                
                VStack {
                    ForEach(types, id: \.type) { (type, title) in
                        let typeLocations = locations.filter { $0.type == type }
                        if !typeLocations.isEmpty {
                            Text(title)
                                .font(.title)
                                .bold()
                                .frame(maxWidth: .infinity, alignment: .leading)
                            ForEach(typeLocations, id: \.id) { location in
                                LocationCellView(location: location, animation: animation, selectedLocationID: $selectedLocationID)
                            }
                            
                            Spacer()
                                .frame(height: 32)
                        }
                    }
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal)
            }
            .background(Color(.systemGroupedBackground))
            .overlay {
                ZStack {
                    VisualEffectView(uiVisualEffect: UIBlurEffect(style: (colorScheme == .dark) ? .dark : .light))
                        .ignoresSafeArea()
                        .opacity(showFullscreen == 1 ? 1 : 0)
                     
                    if let location = mensaService.locations.filter({ $0.id == selectedLocationID}).first {
                        LocationDetailView(location: location, animation: animation, selectedLocationID: $selectedLocationID, showFullscreen: $showFullscreen)
                            //.background(.regularMaterial)
                            //.transition(.offset())
                            .transition(.offset())
                    }
                }
            }
        }
    }
    
    struct LocationCellView: View {
        let location: Location
        let animation: Namespace.ID
        
        @Binding var selectedLocationID: Int
        
        var body: some View {
            VStack {
//                if let uiImage = location.image.systemImage {
//                if let uiImage = location.image.imageData?.uiImage {
                if let uiImage = location.image.platformImage?.uiImage {
                    Image(uiImage: uiImage)
                        .resizable()
                        .scaledToFill()
                        .frame(height: 150)
                        .matchedGeometryEffect(id: "LocationImage \(location.id) match", in: animation)
                        .overlay(alignment: .bottomLeading) {
                            Text(location.name)
                                .foregroundStyle(.white)
                                .font(.title3)
                                .bold()
                                .padding()
                                .shadow(radius: 10)
                        }
                } else {
                    Text(location.name)
                        .font(.largeTitle)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical)
                        .background(Color(.secondarySystemGroupedBackground))
                }
            }
            .rounded()
            .onTapGesture {
                withAnimation {
                    selectedLocationID = Int(location.id)
                }
            }
            .opacity(selectedLocationID == location.id ? 0 : 1)
            .transaction { transaction in
                transaction.animation = nil
            }
        }
    }
    
    struct LocationDetailView: View {
        let location: Location
        let animation: Namespace.ID
        
        @Binding var selectedLocationID: Int
        @Binding var showFullscreen: CGFloat
        
        @Environment(\.colorScheme) private var colorScheme
        
        @State private var dragAmount: CGFloat = 0
        
        var body: some View {
            ScrollView {
                VStack(alignment: .leading) {
                    /*
                    Text(location.name)
                        .font(.title)
                        .bold()
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.bottom)
                        .opacity(showFullscreen == 1 ? 1 : 0)
                        //.matchedGeometryEffect(id: "LocationText \(location.id)", in: animation)
                     */
                    
//                    if let uiImage = location.image.systemImage {
//                    if let uiImage = location.image.imageData?.uiImage {
                    if let uiImage = location.image.platformImage?.uiImage {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                            .frame(height: showFullscreen == 1 ? nil : 150)
                            .rounded()
                            .matchedGeometryEffect(id: "LocationImage \(location.id) \(showFullscreen == 1 ? "ignore" : "match")", in: animation, isSource: false)
                            .padding(.bottom)
                            .zIndex(1)
                    }
                    
                    VStack(alignment: .leading) {
                        Text("Opening hours")
                            .font(.title3)
                            .bold()
                        Text(location.openingHours)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color.accentColor.opacity(colorScheme == .dark ? 0.6 : 0.4))
                    .rounded()
                    .padding(.bottom)
                    .opacity(showFullscreen == 1 ? 1 : 0)
                    
                    Text(location.description_)
                        .opacity(showFullscreen == 1 ? 1 : 0)
                }
                .padding(.horizontal)
                .padding(.bottom, 32)
            }
            .onAppear {
                Task {
                    withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                        showFullscreen = 1
                    }
                }
            }
            .onTapGesture {
                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    showFullscreen = 0
                }
            }
            .simultaneousGesture(
                DragGesture()
                    .onChanged { value in
                        dragAmount = value.startLocation.x
                    }
                    .onEnded { value in
                        let dragDistance = value.translation.width
                        
                        if (dragAmount < 20 && dragDistance > 50 && value.velocity.width > 300) {
                            withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                                showFullscreen = 0
                            }
                        }
                        dragAmount = 0
                    }
            )
            
            .navigationTitle(showFullscreen == 1 ? location.name : "")
            .navigationBarBackButtonHidden(showFullscreen == 1)
            .toolbar {
                if showFullscreen == 1 {
                    if #available(iOS 26.0, *) {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button("", systemSymbol: .xmark) {
                                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                                    showFullscreen = 0
                                }
                            }
                        }
                    } else {
                        ToolbarItem(placement: .topBarLeading) {
                            Button {
                                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                                    showFullscreen = 0
                                }
                            } label: {
                                Image(systemSymbol: .chevronLeft)
                                    .fontWeight(.semibold)
                            }
                            .padding(.leading, -8)
                        }
                    }
                }
            }
            
            .onAnimationCompleted(for: showFullscreen) {
                if showFullscreen == 0 {
                    selectedLocationID = -1
                }
            }
        }
    }
    
    struct HTMLTextView: View {
        let html: String
        @State private var attrString: NSAttributedString? = nil
        
        init(html: String) {
            // UITextView doesn't add any spacing after a <ul> block
            let processedHtml = html.replacingOccurrences(of: "</ul>", with: "</ul><div></div>")
            
            self.html = processedHtml
        }
        
        var body: some View {
            VStack {
                if let attrString = attrString {
                    AttributedText(attrString: attrString)
                }
            }
            .onAppear {
                // This is done onAppear so that FoodExtras can appear faster.
                // Also if doing this on initialization, in iOS 17 specifically, the navTitle glitches
                attrString = Self.createAttrString(html: html)
            }
        }
        
        private static func createAttrString(html: String) -> NSAttributedString? {
            if let data = html.data(using: .utf8) {
                if let nsAttrString: NSAttributedString = try? NSAttributedString(data: data, options: [
                    .documentType: NSAttributedString.DocumentType.html,
                    .characterEncoding: String.Encoding.utf8.rawValue,
                ], documentAttributes: nil) {
                    let result = removingFontFaces(attrString: nsAttrString)
                    
                    return result
                } else {
                    return nil
                }
            } else {
                return nil
            }
        }

        private static func removingFontFaces(attrString: NSAttributedString) -> NSAttributedString {
            let result = NSMutableAttributedString(attributedString: attrString)
            
            result.enumerateAttribute(.font, in: NSRange(location: 0, length: result.length)) { value, range, _ in
                guard let oldFont = value as? UIFont else { return }
                
                // Keep the same symbolic traits (bold, italic) but reset family/size
                let traits = oldFont.fontDescriptor.symbolicTraits
                let fontSize = oldFont.pointSize
                
                // Use system font as base
                var textStyle: UIFont.TextStyle {
                    switch fontSize {
                    case 0..<14:
                        return .footnote
                    case 14..<18:
                        return .body
                    case 18..<22:
                        return .headline
                    case 22..<28:
                        return .title3
                    default:
                        return .title1
                    }
                }
                
                var newDescriptor = UIFontDescriptor.preferredFontDescriptor(withTextStyle: textStyle)
                
                if let withTraits = newDescriptor.withSymbolicTraits(traits) {
                    newDescriptor = withTraits
                }
                
                let newFont = UIFont(descriptor: newDescriptor, size: fontSize * 1.35)
                
                result.removeAttribute(.font, range: range)
                result.addAttribute(.font, value: newFont, range: range)
                result.addAttribute(.foregroundColor, value: UIColor.label, range: range)
            }
            
            return result
        }

        /*
        private static func createSwiftAttrString(html: String) -> AttributedString? {
            if let nsAttrString: NSAttributedString = createAttrString(html: html) {
                let attrString: AttributedString = AttributedString(nsAttrString)
                return attrString
            } else {
                return nil
            }
        }
        */
    }

    struct AttributedText: View {
        @State var attrString: NSAttributedString
        
        var body: some View {
            // For reasons unknown to me, placing an NSAttributedString inside Text("\()") preserves images, while AttributedString() in Text() cant display them...
            VStack {
                UIKitAttributedText(attrString: attrString)
            }
            .background {
                GeometryReader { geo in
                    Color.clear
                        // I have no idea why, but to make the images correctly sized,
                        // the "- 10" are required
                        .onAppear {
                            resizeImages(maxWidth: geo.size.width - 10)
                        }
                        .onChange(of: geo.size.width) { newWidth in
                            resizeImages(maxWidth: newWidth - 10)
                        }
                }
            }
        }
        
        private func resizeImages(maxWidth: CGFloat? = nil, scale: CGFloat = 1.0) {
            
            // attrString isn't modified in-place so that UIKitAttributeView recalculates its size
            let newAttrString = NSAttributedString(attributedString: attrString)
            newAttrString.enumerateAttribute(.attachment, in: NSRange(location: 0, length: newAttrString.length)) { value, range, _ in
                guard let attachment = value as? NSTextAttachment else { return }
                
                if let imageSize: CGSize = {
                    if let image = attachment.image {
                        return image.size
                    }
                    
                    // If no image, check if there is a file wrapper
                    if let wrapper = attachment.fileWrapper,
                          let data = wrapper.regularFileContents,
                          let img = UIImage(data: data) {
                        return img.size
                    }
                    
                    return nil
                }() {
                    var newSize: CGSize
                    
                    // Option 1: Scale to maxWidth
                    if let maxWidth = maxWidth {
                        let ratio = imageSize.height / imageSize.width
                        newSize = CGSize(width: maxWidth,
                                           height: maxWidth * ratio)
                    } else {
                        // Option 2: Apply scale factor only
                        newSize = CGSize(width: imageSize.width * scale,
                                         height: imageSize.height * scale)
                    }
                    
                    attachment.bounds = CGRect(origin: .zero, size: newSize)
                    //attachment.bounds = CGRect(x: 0, y: 0, width: 10, height: 10)
                }
            }
            
            attrString = newAttrString
        }
    }

    struct UIKitAttributedText: UIViewRepresentable {
        let attrString: NSAttributedString
        
        func makeUIView(context: Context) -> UITextView {
            let textView = UITextView()
            textView.isEditable = false   // Optional, if you only want display
            textView.isScrollEnabled = false
            textView.backgroundColor = .clear
            return textView
        }
        
        func updateUIView(_ uiView: UITextView, context: Context) {
            uiView.attributedText = attrString
            //uiView.invalidateIntrinsicContentSize()
        }
        
        func sizeThatFits(_ proposal: ProposedViewSize, uiView: UITextView, context: Context) -> CGSize? {
            let targetWidth = proposal.width ?? 0
            let size = uiView.sizeThatFits(CGSize(width: targetWidth, height: .greatestFiniteMagnitude))
            return CGSize(width: size.width, height: size.height)
        }
    }
}


#Preview("FoodExtras") {
    NavigationStack {
        FoodExtras()
    }
    .environmentObject(MensaService(previewMode: true))
}

#Preview("LocationsView") {
    NavigationStack {
        FoodExtras.LocationView()
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Image(systemSymbol: .chevronLeft)
                }
            }
    }
    .environmentObject(MensaService(previewMode: true))
}

#Preview("FoodView") {
    FoodView()
        .environmentObject(MensaService(previewMode: true))
}
