//  Created on 26.10.25.

import SwiftUI
import Shared

struct FoodTypesView: View {
    @State var placeholder: Bool = true
    @State var isOnStates: [Bool] = Array(repeating: true, count: 7)    // 7 = foodTypes.count
    
    @AppStorage("canteenDisabledFoodTypes") private var disabledFoodTypes: String = ""
    
    let foodTypes: [(title: String, id: FoodType)] = [     // foodTypes.count used above
        (String(localized: "Vegan"), FoodType.vegan),
        (String(localized: "Vegetarian"), FoodType.meatless),
        (String(localized: "Fish and fish products"), FoodType.fish),
        (String(localized: "Pork"), FoodType.pork),
        (String(localized: "Beef"), FoodType.beef),
        (String(localized: "Poultry"), FoodType.poultry),
        (String(localized: "Venison"), FoodType.venison)
    ]
    
    var body: some View {
        List {
            Section {
                ForEach(foodTypes.indices, id: \.self) { index in
                    let foodType = foodTypes[index]
                    FoodTypeToggle(foodType.id, title: foodType.title, isOn: $isOnStates[index])
                }
            } header: {
                Text("What do you like to eat?")
            }
            
            Section {
                FoodTypeToggle(nil, title: String(localized: "Lamb/Sheep"), isOn: $placeholder)
                FoodTypeToggle(nil, title: String(localized: "Veal"), isOn: $placeholder)
                FoodTypeToggle(nil, title: String(localized: "Insect"), isOn: $placeholder)
            } header: {
                Text("Unimplemented")
            }
            .disabled(true)
        }
        .onAppear {
            let disabledFoodTypesList = disabledFoodTypes.split(separator: ",")
            for disabledFoodType in disabledFoodTypesList {
                let optIndex = foodTypes.firstIndex(where: { $0.title == disabledFoodType})
                
                if let index = optIndex {
                    isOnStates[index] = false
                }
            }
        }
        .onChange(of: isOnStates) { newStates in
            disabledFoodTypes = zip(foodTypes, isOnStates)
                .compactMap { foodType, isOn in
                    isOn ? nil : foodType.id.displayName
                }
                .joined(separator: ",")
        }
        .navigationTitle("Food Types")
        .navigationBarTitleDisplayMode(.inline)
    }
}

private struct FoodTypeToggle: View {
    let foodType: FoodType?
    let title: String
    
    @Binding var isOn: Bool
    
    init(_ foodType: FoodType?, title: String, isOn: Binding<Bool>) {
        self.foodType = foodType
        self.title = title
        self._isOn = isOn
    }
    
    var body: some View {
        Toggle(isOn: $isOn) {
            HStack {
                Group {
                    if let foodType {
                        FoodTypeIcon(type: foodType)
                    } else {
                        EmptyView()
                    }
                }
                .frame(width: 32, height: 32)
                .padding(.vertical, -16)
                
                Text(title)
            }
        }
    }
}

#Preview {
    NavigationStack {
        FoodTypesView()
    }
}
