import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        UIPageControl.appearance().currentPageIndicatorTintColor = .label
        UIPageControl.appearance().pageIndicatorTintColor = .secondaryLabel
    }
    
    var body: some Scene {
        WindowGroup {
        #if DEBUG
            EntryView()
                .environmentObject(TucanService())
                .environmentObject(MensaService())
                .environmentObject(KeyboardHeightHelper())
                .environmentObject(IdentityService())
                .environmentObject(SettingsService())
        #else
            ContentView()
                .environmentObject(TucanService())
                .environmentObject(MensaService())
                .environmentObject(KeyboardHeightHelper())
                .environmentObject(IdentityService())
                .environmentObject(SettingsService())
        #endif
        }
    }
}

#if DEBUG
private struct EntryView: View {
    @EnvironmentObject var settingsService: SettingsService
    @State private var uiType = SettingsManager.shared.uiType.value
    
    var body: some View {
        if (settingsService.uiType == UIType.swiftUi) {
            ContentView()
        } else if (settingsService.uiType == UIType.compose) {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}

private class IOSNativeViewFactory: NativeViewFactory {
    func createGeneralMapView() -> UIViewController {
        UIHostingController(rootView: ComposeMapView(annotations: annotations, filteredAnnotations: annotations))
    }
}

private struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(nativeViewFactory: IOSNativeViewFactory())
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
#endif


#Preview() {
    ContentView()
        .environmentObject(TucanService())
        .environmentObject(IdentityService(previewMode: true))
        .environmentObject(MensaService())
        .environmentObject(KeyboardHeightHelper())
        .environmentObject(SettingsService())
}
