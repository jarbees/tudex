//  Created on 06.12.25.

import SwiftUI
import Shared
import CoreImage.CIFilterBuiltins
import PassKit
#if DEBUG
import PhotosUI
import CropViewController
#endif

struct StudentIDView: View {
    //private let codeGenerator: Code25Generator = Code25Generator()
    @EnvironmentObject private var identityService: IdentityService
    @EnvironmentObject private var settingsService: SettingsService
    @Environment(\.colorScheme) private var colorScheme
    @Namespace private var animation
    
    @State private var showStudentCardActions = true
    @State private var showSheet = false
    
    #if DEBUG
    @State private var selectedItem: PhotosPickerItem?
    @State private var tempSelectedItem: PhotosPickerItem?
//    @State private var image: UIImage?
    @State private var imageData: Foundation.Data?
    #endif
    
    var body: some View {
        ZStack {
            if (identityService.studentID == nil) {
                if (identityService.tuLoginStatus == .mfaUnconfirmed) {
                    LoginLoadingView()
                } else if (identityService.tuLoginStatus != .mfa) {
                    LoginLockedView()
                } else if identityService.studentIDStatus == .loading && identityService.studentID == nil {
                    RequestLoadingView(itemName: "Student IDs")
                } else {
                    EmptyView()
                }
            } else {
                RefreshableScrollView(action: {
                    Task {
                        await identityService.updateStudentID()
                    }
                }, content: {
                    VStack {
                        TabView {
                            #if DEBUG
                            if let studentCard = identityService.studentID?.studentCard {
                                VStack(spacing: 0) {
                                    StudentCardView(image: settingsService.userImage, studentCard: studentCard)
                                    
                                    /*
                                     if (identityManager.userImage != nil) {
                                     AddToWalletButton()
                                     }
                                     */
                                    
                                    Spacer()
                                }
                                .matchedGeometryEffect(id: "StudentIDActions", in: animation, anchor: .bottom, isSource: true)
                                .onDisappear {
                                    showStudentCardActions = false
                                }
                                .onAppear {
                                    showStudentCardActions = true
                                }
                            }
                            #endif
                            
                            if let libraryNumber = identityService.studentID?.libraryCardID {
                                VStack(spacing: 0) {
                                    LibraryCardView(libraryNumber: libraryNumber)
                                    //    .padding(.bottom, 32)
                                    
                                    /*
                                     AddToWalletButton()
                                     */
                                    
                                    Spacer()
                                }
                            }
                            
                            if let euroStudentID = identityService.studentID?.euroStudentID {
                                VStack(spacing: 0) {
                                    UselessESICardView(euroStudentID: euroStudentID)
                                    
                                    /*
                                     AddToWalletButton()
                                     */
                                    
                                    Spacer()
                                }
                            }
                        }
                        .tabViewStyle(.page)
                        //.frame(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.width * 10/16 + 96)
                        .frame(width: UIScreen.main.bounds.width, height: UIScreen.main.bounds.width * 10/16 + 42)
                        
                        #if DEBUG
                        if identityService.studentID?.studentCard != nil {
                            PhotosPicker(selection: $tempSelectedItem, matching: .images) {
                                // Custom button view
                                Text("Change image")
                                    .padding(.vertical, 8)
                                    .frame(maxWidth: .infinity)
                                    .fontWeight(.semibold)
                            }
                            .onChange(of: tempSelectedItem) { newItem in
                                // tempState is used so PhotosPicker doesn't preselect previous item
                                selectedItem = newItem
                                tempSelectedItem = nil
                                
                                Task {
//                                    if let data = try? await newItem?.loadTransferable(type: Data.self),
//                                       let uiImage = UIImage(data: data) {
//                                        image = uiImage
                                    if let data = try? await newItem?.loadTransferable(type: Data.self) {
                                        imageData = data
                                        showSheet = true
                                    }
                                }
                            }
                            .buttonStyle(.bordered)
                            .padding(.top, 16)
                            .padding(.horizontal)
                            .matchedGeometryEffect(id: "StudentIDActions", in: animation, properties: [.position], anchor: .top, isSource: false)
                            .opacity(showStudentCardActions ? 1 : 0)
                            
                            /*
                            .onChange(of: selectedItem) { newItem in
                                Task {
                                    if let data = try? await newItem?.loadTransferable(type: Data.self),
                                       let uiImage = UIImage(data: data) {
                                        image = uiImage
                                        showSheet = true
                                    }
                                }
                            }
                            */
                            // This is done to ensure that image was correctly set
                            /*
                            .onChange(of: image) { _ in
                                showSheet = true
                            }
                            */
                        }
                        #endif
                    }
                })
//                .fullScreenCover(isPresented: $showSheet) { [image] in
//                    ImageCropper(image: image) { readyImage in
//                        identityService.setUserImage(to: readyImage)
//                    }
//                }
                #if DEBUG
                .fullScreenCover(isPresented: $showSheet) { [imageData] in
                    if let data = imageData {
                        ImageCropper(image: UIImage(data: data)) { readyImage in
                            if let readyData = readyImage.pngData() {
                                settingsService.setUserImageData(to: readyData)
                            }
                        }
                    } else {
                        VStack {
                            Text("There was an error selecting the image, please try again")
                                .padding(.bottom)
                            BigBlueButton("Dismiss") {
                                showSheet = false
                            }
                        }
                    }
                }
                #endif
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Identity Cards")
        .navigationTitleAccessory(accessory: { isLarge in
            Group {
                if identityService.studentID != nil {
                    if (identityService.tuLoginStatus == .mfaUnconfirmed ||
                        identityService.studentIDStatus == .loading
                    ) {
                        ProgressView()
                            .scaleEffect(isLarge ? 1.2 : 1)
                            .padding(.top, isLarge ? 0 : 2)
                            .padding(.leading, isLarge ? 8 : 4)
                    }
                }
            }
        })
    }
}

extension StudentIDView {
    struct AddToWalletButton: View {
        var body: some View {
            UIKitWalletButton()
                .rounded()
                .fixedSize(horizontal: false, vertical: true)
                .frame(width: 260)
        }
        
        struct UIKitWalletButton: UIViewRepresentable {
            
            func makeUIView(context: Context) -> PKAddPassButton {
                return PKAddPassButton(addPassButtonStyle: .black)
            }
            
            func updateUIView(_ uiView: PKAddPassButton, context: Context) {}
        }
    }
    
    #if DEBUG
    struct ImageCropper: UIViewControllerRepresentable{
        let image: UIImage?
        let done: (UIImage) -> Void
        
        func updateUIViewController(_ uiViewController: UIViewControllerType, context: Context) {}
        
        func makeUIViewController(context: Context) -> some UIViewController {
            let img = self.image ?? UIImage()
            let cropViewController = CropViewController(image: img)
            cropViewController.delegate = context.coordinator
            
            cropViewController.aspectRatioPreset = CGSize(width: 3, height: 4)
            cropViewController.aspectRatioLockEnabled = true
            cropViewController.resetAspectRatioEnabled = false
            cropViewController.aspectRatioPickerButtonHidden = true
            
            
            return cropViewController
        }
        
        class Coordinator: NSObject, CropViewControllerDelegate {
            let parent: ImageCropper
            
            init(_ parent: ImageCropper){
                self.parent = parent
            }
            
            func cropViewController(_ cropViewController: CropViewController, didCropToImage image: UIImage, withRect cropRect: CGRect, angle: Int) {
                parent.done(image)
                cropViewController.dismiss(animated: true, completion: nil)
            }
        }
          
        func makeCoordinator() -> Coordinator {
            return Coordinator(self)
        }
    }
    
    struct StudentCardView: View {
        @Environment(\.colorScheme) private var colorScheme
        
        let image: UIImage?
        let studentCard: StudentCard
        
        var body: some View {
            VStack(alignment: .leading, spacing: 0) {
                Text("Student ID \(studentCard.semester)")
                    .padding(.leading, 2)
                
                HStack {
                    if let image = image {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(3/4, contentMode: .fit)
                            .rounded(liquidCornerRadius: 16)
                            .padding(.vertical, 12)
                    } else {
                        Text("No image found")
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background(Color(.systemGray4))
                            .aspectRatio(3/4, contentMode: .fit)
                            .rounded(liquidCornerRadius: 16)
                            .padding(.vertical, 12)
                    }
                    
                    VStack(spacing: 6) {
                        VStack(alignment: .leading, spacing: 6) {
                            VStack(alignment: .leading, spacing: 0) {
                                Text("First Name")
                                    .font(.caption2)
                                    .lineLimit(1)
                                Text(studentCard.firstName)
                                    .lineLimit(1)
                            }
                            
                            VStack(alignment: .leading, spacing: 0) {
                                Text("Last Name")
                                    .font(.caption2)
                                    .lineLimit(1)
                                Text(studentCard.lastName)
                                    .lineLimit(1)
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        
                        Grid(alignment: .leading, horizontalSpacing: 16, verticalSpacing: 6) {
                            GridRow {
                                VStack(alignment: .leading, spacing: 0) {
                                    Text("Birthdate")
                                        .font(.caption2)
                                        .lineLimit(1)
                                    Text(studentCard.birthdate)
                                        .lineLimit(1)
                                }
                                VStack(alignment: .leading, spacing: 0) {
                                    Text("Matr. Number")
                                        .font(.caption2)
                                        .lineLimit(1)
                                    Text(studentCard.matriNumber)
                                        .lineLimit(1)
                                }
                            }
                            
                            
                            GridRow {
                                VStack(alignment: .leading, spacing: 0) {
                                    Text("Valid Until")
                                        .font(.caption2)
                                        .lineLimit(1)
                                    Text(studentCard.lastValidDate)
                                        .lineLimit(1)
                                }
                            }
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(.leading, 4)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding()
            .overlay(alignment: .bottomTrailing) {
                Text("VALID")
                    .bold()
                    .padding(.horizontal)
                    .padding(.vertical, 8)
                    .background(Color(.systemGray5))
                    .rounded(liquidCornerRadius: 12)
                    .padding()
            }
            .overlay(alignment: .topTrailing) {
                Image("TudaLogo")
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(.black)
                    .frame(height: 32)
                    .padding([.top, .trailing], colorScheme == .dark ? 12 : 16)
                    .padding(.trailing, 2)
                    .padding([.bottom, .leading], 10)
                    .background(colorScheme == .dark ? .white : .clear)
                    .clipShape(UnevenRoundedRectangle(cornerRadii: .init(bottomLeading: 20)))
                
//                Text("Logo here")
//                    .padding(.horizontal)
//                    .padding(.vertical, 12)
//                    .background(Color(.systemGray5))
//                    .padding(.top)
            }
            .aspectRatio(16/10, contentMode: .fit)
            .rounded(liquidCornerRadius: 20)
            .modify { view in
                if #available(iOS 26.0, *) {
                    view.glassEffect(.regular.tint(Color(.secondarySystemGroupedBackground)), in: RoundedRectangle(cornerRadius: 20))
                        .padding()
                } else {
                    view.background(.yellow.opacity(0.01))
                        .background(.white)
                        .rounded()
                        .padding()
                        .shadow(color: .gray.opacity(0.5), radius: 5, x: 5, y: 5)
                }
            }
        }
    }
    #endif
    
    struct LibraryCardView: View {
        @Environment(\.colorScheme) private var colorScheme
        
        let libraryNumber: String
        
        var body: some View {
            VStack {
                VStack(alignment: .leading) {
                    Text("Library Card")
                        .padding(.leading, 2)
                        if !libraryNumber.isEmpty {
                            Group {
                                if let img = platformImageFromInterleaved25(data: libraryNumber, height: 60)?.uiImage {
                                    Image(uiImage: img)
                                        .interpolation(.none)
                                        .resizable()
                                        .scaledToFit()
                                } else {
                                    Text("Invalid I-25 input")
                                        .foregroundColor(.red)
                                }
                            }
                            .padding(colorScheme == .dark ? .all : .vertical)
                            .background(.white)
                            .rounded(liquidCornerRadius: 16)
                        }
                    Text("Card Number: \(libraryNumber)")
                        .padding(.leading, 2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .overlay(alignment: .topTrailing) {
                    Image("ULBLogo")
                        .resizable()
                        .scaledToFit()
                        .foregroundStyle(.black)
                        .frame(height: 32)
                        .padding([.top, .trailing], colorScheme == .dark ? 12 : 16)
                        .padding(.trailing, 2)
                        .padding([.bottom, .leading], 10)
                        .background(colorScheme == .dark ? .white : .clear)
                        .clipShape(UnevenRoundedRectangle(cornerRadii: .init(bottomLeading: 20)))
                }
            }
            .aspectRatio(16/10, contentMode: .fit)
            .rounded(liquidCornerRadius: 20)
            .modify { view in
                if #available(iOS 26.0, *) {
                    view.glassEffect(.regular.tint(Color(.secondarySystemGroupedBackground)), in: RoundedRectangle(cornerRadius: 20))
                        .padding()
                } else {
                    view.background(.yellow.opacity(0.01))
                        .background(.white)
                        .rounded()
                        .padding()
                        .shadow(color: .gray.opacity(0.5), radius: 5, x: 5, y: 5)
                }
            }
        }
    }
    
    struct UselessESICardView: View {
        @Environment(\.colorScheme) private var colorScheme
        
        let euroStudentID: String
        
        var body: some View {
            VStack {
                VStack(alignment: .leading) {
                    Text("European Student Identifier")
                        .padding(.leading, 2)
                    HStack {
                        QRCodeView(text: euroStudentID)
                        
                        //Label("This QR Code is useless and only shown because TUDa App shows it as well.", systemSymbol: .exclamationmarkTriangleFill)
                        
                    }
                    .padding(.vertical, 8)
                    
                    Text(euroStudentID)
                        .font(.caption)
                        .padding(.leading, 2)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .overlay(alignment: .topTrailing) {
                    Image("ESILogoVertical")
                        .resizable()
                        .scaledToFit()
                        .foregroundStyle(.black)
                        .frame(height: 20)
                        .padding([.top, .trailing], colorScheme == .dark ? 12 : 16)
                        .padding(.trailing, 2)
                        .padding([.bottom, .leading], 10)
                        .background(colorScheme == .dark ? .white : .clear)
                        .clipShape(UnevenRoundedRectangle(cornerRadii: .init(bottomLeading: 20)))
                }
                    
            }
            .aspectRatio(16/10, contentMode: .fit)
            .rounded(liquidCornerRadius: 20)
            .modify { view in
                if #available(iOS 26.0, *) {
                    view.glassEffect(.regular.tint(Color(.secondarySystemGroupedBackground)), in: RoundedRectangle(cornerRadius: 20))
                        .padding()
                } else {
                    view.background(.yellow.opacity(0.01))
                        .background(.white)
                        .rounded()
                        .padding()
                        .shadow(color: .gray.opacity(0.5), radius: 5, x: 5, y: 5)
                }
            }
        }
    }

    // This class was generated by ChatGPT
    struct QRCodeView: View {
        let text: String
        private let context = CIContext()
        private let filter = CIFilter.qrCodeGenerator()

        var body: some View {
            if let image = generateQRCode() {
                Image(uiImage: image)
                    .interpolation(.none)   // crisp pixels
                    .resizable()
                    .scaledToFit()
            } else {
                Text("QR generation failed")
                    .foregroundColor(.red)
            }
        }

        private func generateQRCode() -> UIImage? {
            let data = Data(text.utf8)
            filter.setValue(data, forKey: "inputMessage")

            // adjust error correction: L, M, Q, H
            filter.setValue("M", forKey: "inputCorrectionLevel")

            guard let output = filter.outputImage else { return nil }

            // scale up to look sharp
            let scaled = output.transformed(by: CGAffineTransform(scaleX: 10, y: 10))

            if let cgimg = context.createCGImage(scaled, from: scaled.extent) {
                return UIImage(cgImage: cgimg)
            }
            return nil
        }
    }
}

#Preview {
    NavigationStack {
        StudentIDView()
            .environmentObject(IdentityService(previewMode: true))
    }
}

#if DEBUG
#Preview("Cropper") {
    StudentIDView.ImageCropper(image: UIImage(named: "UnknownFood"), done: {_ in })
        .ignoresSafeArea()
}
#endif
