//  Created on 26.10.25.

import SwiftUI

struct AllergiesView: View {
    @State var isOnStates: [Bool] = Array(repeating: false, count: 25)
    
    @AppStorage("canteenEnabledAllergies") private var enabledAllergies: String = ""
    
    let allergies: [(id: String, title: String)] = [
        ("A1", String(localized: "Wheat")),
        ("A2", String(localized: "Spelt")),
        ("A3", String(localized: "Rye")),
        ("A4", String(localized: "Barley")),
        ("A5", String(localized: "Oats")),
        ("B", String(localized: "Crustaceans and crustacean products")),
        ("C", String(localized: "Eggs and egg products")),
        ("D", String(localized: "Fish and fish products")),
        ("E", String(localized: "Peanuts and peanut products")),
        ("F", String(localized: "Soybeans")),
        ("G", String(localized: "Milk and milk products")),
        ("H1", String(localized: "Almonds")),
        ("H2", String(localized: "Hazelnuts")),
        ("H3", String(localized: "Walnuts")),
        ("H4", String(localized: "Cashew nuts")),
        ("H5", String(localized: "Pecan nuts")),
        ("H6", String(localized: "Brazil nuts")),
        ("H7", String(localized: "Pistachio nuts")),
        ("H8", String(localized: "Macadamia nuts")),
        ("I", String(localized: "Celery and celery products")),
        ("J", String(localized: "Mustard and mustard products")),
        ("K", String(localized: "Sesame and sesame products")),
        ("L", String(localized: "Sulfur dioxides and sulfides")),
        ("M", String(localized: "Lupines")),
        ("N", String(localized: "Molluscs"))
    ]
    
    var body: some View {
        List {
            Section {
                ForEach(allergies.indices, id: \.self) { index in
                    let allergy = allergies[index]
                    AllergyToggle(allergy.id, title: allergy.title, isOn: $isOnStates[index])
                }
            } header: {
                Text("What are you allergic to?")
            }
        }
        .onAppear {
            let enabledAllergiesList = enabledAllergies.split(separator: ",")
            for enabledAllergy in enabledAllergiesList {
                let optIndex = allergies.firstIndex(where: { $0.id == enabledAllergy})
                
                if let index = optIndex {
                    isOnStates[index] = true
                }
            }
        }
        .onChange(of: isOnStates) { newStates in
            enabledAllergies = zip(allergies, isOnStates)
                .filter { $0.1 }
                .map { $0.0.id }
                .joined(separator: ",")
        }
        .navigationTitle("Allergies")
        .navigationBarTitleDisplayMode(.inline)
    }
}

extension AllergiesView {
    struct AllergyToggle: View {
        let id: String
        let title: String
        
        @Binding var isOn: Bool
        
        init(_ id: String, title: String, isOn: Binding<Bool>) {
            self.id = id
            self.title = title
            self._isOn = isOn
        }
        
        var body: some View {
            Toggle(isOn: $isOn) {
                HStack(alignment: .top) {
                    Text(id)
                    Text("-")
                        .foregroundStyle(Color(.secondaryLabel))
                    Text(title)
                        .foregroundStyle(Color(.secondaryLabel))
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        AllergiesView()
    }
}
