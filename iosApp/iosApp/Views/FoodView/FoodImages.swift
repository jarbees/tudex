//  Created on 26.08.25.

import SwiftUI

struct FoodImage: View {
    let image: UIImage
    let sliceHeight: CGFloat = 100
        
    init(_ image: UIImage) {
        self.image = image
    }
    
    var body: some View {
        VStack(spacing: 0) {
            Image(uiImage: image)  // Placeholder image, it will first fit, then crop to height 300. It is only for setting frame size
                .resizable()
                .aspectRatio(contentMode: .fill)
                .frame(maxHeight: 300, alignment: .top)
                .clipped()
                .scaledToFit()
                .hidden()
                .overlay(alignment: .top) {
                    // using CGImage makes the image flexible (for matchGeoEffect) with the correct frame
                    if let preCGImage = image.cgImage {
                        let stretchMultiplier = CGFloat(preCGImage.height) / sliceHeight
                        
                        let sliceCGImage = stretchedSlice(from: preCGImage, sliceHeight: sliceHeight, stretchMultiplier: stretchMultiplier) ?? preCGImage
                        
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            
                            // This is a streched extension at the bottom, basically a stretched backgroundExtension
                            .background {
                                ZStack {
                                    Image(uiImage: UIImage(cgImage: sliceCGImage))
                                        .resizable()
                                        .scaleEffect(y: -1, anchor: .bottom)
                                    
                                    Image(uiImage: UIImage(cgImage: sliceCGImage))
                                        .resizable()
                                        .scaleEffect(y: -1, anchor: .bottom)
                                        .blur(radius: 20)
                                }
                                .offset(y: -1)
                                .allowsHitTesting(false)
                            }
                        
                            // This is a manual version of iOS26 backgroundExtensionEffect which works with matchGeometry
                            .overlay {
                                ZStack {
                                    Image(uiImage: image)      // EdgeEffect image
                                        .resizable()
                                    Image(uiImage: image)      // EdgeEffect image
                                        .resizable()
                                        .blur(radius: 10, opaque: false)
                                }
                                .scaledToFit()
                                .scaleEffect(y: -1, anchor: .top)
                                .offset(y: 1)
                                .allowsHitTesting(false)
                            }
                    }
                }
            
                // This fades the entirety into transparency (the values used to be 0.33-0.66 with anchor at .top)
                .mask(
                    LinearGradient(
                        gradient: Gradient(stops: [
                            .init(color: .black, location: 0.58),
                            .init(color: .clear, location: 0.91)
                        ]),
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .scaleEffect(y: 2, anchor: .center)
                )
        }
    }
    
    private func stretchedSlice(from preCGImage: CGImage, sliceHeight: CGFloat, stretchMultiplier: CGFloat) -> CGImage? {
        let sliceRect = CGRect(
            x: 0,
            y: CGFloat(preCGImage.height) - sliceHeight,
            width: CGFloat(preCGImage.width),
            height: sliceHeight
        )
        
        guard let slice = preCGImage.cropping(to: sliceRect) else {
            return nil
        }
        
        return slice
        
        
        // Originally, the plan was to slice and stretch a CGImage,
        // however the stretching can be done in SwiftUI, which is significantly faster
        
        // Create context for stretched image
        /*
        let width = Int(sliceRect.width)
        let height = Int(sliceHeight * abs(stretchMultiplier))
        
        guard let context = CGContext(
            data: nil,
            width: width,
            height: height,
            bitsPerComponent: slice.bitsPerComponent,
            bytesPerRow: 0,
            space: slice.colorSpace ?? CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: slice.bitmapInfo.rawValue
        ) else {
            return nil
        }
        
        if (stretchMultiplier < 0) {
            context.translateBy(x: 0, y: CGFloat(height))
            context.scaleBy(x: 1, y: -1)
            
        }
        
        // Stretch slice vertically by drawing it into a larger rect
        let drawRect = CGRect(x: 0, y: 0, width: CGFloat(width), height: CGFloat(height))
        context.draw(slice, in: drawRect)
        
        return context.makeImage()*/
    }

}

// This was a View-based version of iOS 26 BackgroundExtensionEffect
/*
struct ReflectionImage: View {
    let image: String
    
    init(_ image: String) {
        self.image = image
    }
    
    var body: some View {
        GeometryReader { geo in
            ZStack {
                Image("UnknownFood")      // EdgeEffect image
                    .resizable()
                Image("UnknownFood")      // EdgeEffect image
                    .resizable()
                    .blur(radius: 10, opaque: false)
            }
            .scaledToFill()
            .frame(width: geo.size.width)
            .frame(height: geo.safeAreaInsets.top + 1, alignment: .top) // The extra pixel prevents a white pixel line appearing in transitions
            .scaleEffect(y: -1)
            .ignoresSafeArea()
        }
    }
}*/

struct FoodImage_Previews: PreviewProvider {
    static var previews: some View {
        let image = UIImage(named: "UnknownFood")!
        
        VStack {
            FoodImage(image)
                //.aspectRatio(contentMode: .fit)
            Text("Content would start here")
                .font(.largeTitle)
            Spacer()
        }
    }
}

@available(iOS 17.0, *)
#Preview("FoodImage Hit Testing") {
    @Previewable @State var lastTap = "None"

    let image = UIImage(named: "UnknownFood")!

    VStack(spacing: 20) {
        Text("Last tap: \(lastTap)")
            .font(.headline)
        
        Spacer()
            .frame(height: 100)

        HStack(spacing: 8) {
            FoodImage(image)
                .border(.red, width: 2)
                .onTapGesture {
                    lastTap = "LEFT"
                }

            FoodImage(image)
                .border(.blue, width: 2)
                .onTapGesture {
                    lastTap = "RIGHT"
                }
        }
        .padding()

        Text("""
        Tap close to the inner edges.

        A tap visibly inside the red border should always report LEFT.
        A tap visibly inside the blue border should always report RIGHT.
        """)
        .font(.caption)
        .padding()
    }
}
