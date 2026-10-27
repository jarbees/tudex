//  Created on 03.05.25.

import SwiftUI
import SFSafeSymbols

struct AppIconPickerView: View {
    @State private var selectedIcon = UIApplication.shared.alternateIconName
    
    let appIcons: [(label: String, name: String?)] = [
        (String(localized: "Default Icon"), nil),
        (String(localized: "Blue"), "AppIcon 1 Blue"),
        (String(localized: "Light Blue"), "AppIcon 2 LightBlue"),
        (String(localized: "Blue-Green"), "AppIcon 3 BlueGreen"),
        (String(localized: "Green"), "AppIcon 4 Green"),
        (String(localized: "Lime"), "AppIcon 5 Lime"),
        (String(localized: "Yellow"), "AppIcon 6 Yellow"),
        (String(localized: "Orange"), "AppIcon 7 Orange"),
        (String(localized: "Orange-Red"), "AppIcon 8 OrangeRed"),
        (String(localized: "Red"), "AppIcon 9 Red"),
        (String(localized: "Pink"), "AppIcon 10 Pink"),
        (String(localized: "Purple"), "AppIcon 11 Purple"),
        (String(localized: "Gray"), "AppIcon 12 Gray"),
        (String(localized: "White"), "AppIcon 13 White")
    ]
    
    var body: some View {
        List(appIcons, id: \.label) { icon in
            Button(action: {changeAppIcon(to: icon.name)}) {
                HStack {
                    Image(icon.name ?? "AppIcon 0 Default")
                        .resizable()
                        .scaledToFit()
                        .frame(width: 40, height: 40)
                        .rounded(liquidCornerRadius: 12)
                    Text(icon.label)
                        .foregroundStyle(Color(.label))
                    Spacer()
                    
                    if (icon.name == selectedIcon) {
                        Image(systemSymbol: .checkmark)
                    }
                }
            }
        }
        .navigationTitle("App Icon")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    func changeAppIcon(to iconName: String?) {
        selectedIcon = iconName
        UIApplication.shared.setAlternateIconName(iconName) { error in
            if let error = error {
                print("Error setting icon: \(error.localizedDescription)")
            }
        }
    }
}

struct AppIconPickerView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            AppIconPickerView()
        }
    }
}
