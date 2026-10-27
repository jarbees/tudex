//  Created on 07.09.25.

import SwiftUI
import Shared
import PocketSVG

class MensaService: ObservableObject {
    private let mensaManager: MensaManager = MensaManager.shared
    private let foodIconsSource: FoodIcons = FoodIcons.shared
    
    @Published private(set) var teasers: [Teaser] = []
    @Published private(set) var menuItems: [MenuItem] = []
    @Published private(set) var infoItems: [InfoItem] = []
    @Published private(set) var locations: [Location] = []
    
    @Published private(set) var foodIcons: [FoodType: SVGGeometry] = FoodIcons.shared.icons.value.compactMapValues(parseSVG)
    @Published private(set) var vitalLogo: SVGGeometry? = FoodIcons.shared.mensaVital.value.flatMap(parseSVG)
    
    @Published private(set) var status: String = MensaManager.shared.status.value
    @Published private(set) var lang: String = MensaManager.shared.lang.value
    @Published private(set) var defaultCanteen: Int = Int(truncating: MensaManager.shared.defaultCanteen.value)
    @Published private(set) var selectedCanteen: Int = Int(truncating: MensaManager.shared.selectedCanteen.value)
    
    @Published private(set) var cardID: String = MensaManager.shared.cardID.value
    @Published private(set) var cardBalance: String = MensaManager.shared.cardBalance.value
    @Published private(set) var cardLastBalance: String = MensaManager.shared.cardLastBalance.value
    @Published private(set) var cardLastScanDate: String = MensaManager.shared.cardLastScanDate.value
    @Published private(set) var cardLastScanTime: String = MensaManager.shared.cardLastScanTime.value
    
    @MainActor
    convenience init() {
        self.init(previewMode: false)
    }
    
    init(previewMode: Bool) {
        if previewMode {
            Task {
                await initializeDebugStates()
            }
            Task {
                await syncStates()
            }
        } else {
            Task {
                await initializeStates()
            }
            Task {
                await syncStates()
            }
        }
        Task {
            try? await foodIconsSource.load()
        }
    }
    
    @MainActor
    private func initializeDebugStates() async {
        Task {
            await createDebugMenuItems()
            await createDebugTeasers()
            await createDebugLocations()
        }
    }
    
    @MainActor
    private func initializeStates() async {
        try? await mensaManager.initializeStates()
    }
    
    @MainActor
    private func syncStates() async {
        Task {
            for await newTeasers in mensaManager.teasers {
                self.teasers = newTeasers
            }
        }
        Task {
            for await newMenuItems in mensaManager.menuItems {
                self.menuItems = newMenuItems
            }
        }
        Task {
            for await newInfoItems in mensaManager.infoItems {
                self.infoItems = newInfoItems
            }
        }
        Task {
            for await newLocations in mensaManager.locations {
                self.locations = newLocations
            }
        }
        Task {
            for await newFoodIcons in foodIconsSource.icons {
                self.foodIcons = newFoodIcons.compactMapValues(parseSVG)
            }
        }
        Task {
            for await newVitalLogo in foodIconsSource.mensaVital {
                self.vitalLogo = newVitalLogo.flatMap(parseSVG)
            }
        }
        Task {
            for await newStatus in mensaManager.status {
                self.status = newStatus
            }
        }
        Task {
            for await newLang in mensaManager.lang {
                self.lang = newLang
            }
        }
        Task {
            for await newDefaultCanteen in mensaManager.defaultCanteen {
                self.defaultCanteen = Int(truncating: newDefaultCanteen)
            }
        }
        Task {
            for await newSelectedCanteen in mensaManager.selectedCanteen {
                self.selectedCanteen = Int(truncating: newSelectedCanteen)
            }
        }
        Task {
            for await newCardID in mensaManager.cardID {
                self.cardID = newCardID
            }
        }
        Task {
            for await newCardBalance in mensaManager.cardBalance {
                self.cardBalance = newCardBalance
            }
        }
        Task {
            for await newCardLastBalance in mensaManager.cardLastBalance {
                self.cardLastBalance = newCardLastBalance
            }
        }
        Task {
            for await newCardLastScanDate in mensaManager.cardLastScanDate {
                self.cardLastScanDate = newCardLastScanDate
            }
        }
        Task {
            for await newCardLastScanTime in mensaManager.cardLastScanTime {
                self.cardLastScanTime = newCardLastScanTime
            }
        }
    }
    
    func getTeasers() async {
        try? await mensaManager.getTeasers()
    }
    
    /*
    func getMenuItems(canteenId: Int) async {
            try? await mensaManager.getCurrentMenuItems(canteenId: Int32(canteenId), status: status, lang: lang)
    }
    */
    func updateCard(cardID: String, cardBalance: String, cardLastBalance: String) {
        mensaManager.updateCard(cardID: cardID, cardBalance: cardBalance, cardLastBalance: cardLastBalance)
    }
    
    func updateStatus(to newStatus: String) async {
        try? await mensaManager.updateStatus(newStatus: newStatus)
    }
    
    func updateLang(to newLang: String) async {
        try? await mensaManager.updateLang(newLang: newLang)
    }
    
    func updateDefaultCanteen(to newCanteen: Int) async {
        try? await mensaManager.updateDefaultCanteen(newCanteen: Int32(newCanteen))
    }
    
    func updateSelectedCanteen(to newCanteen: Int) async {
        try? await mensaManager.updateSelectedCanteen(newCanteen: Int32(newCanteen))
    }
    
    
    func getCurrentMenuItems() async {
        try? await mensaManager.getCurrentMenuItems()
    }
    
    /*
    func getSpecificMenuItems(ids: [Int], lang: String, status: String) async -> [MenuItem] {
        do {
            return try await mensaManager.getSpecificMenuItems(ids: ids.map { KotlinInt(int: Int32($0)) }, lang: lang, status: status)
        } catch {
            return []
        }
    }
    */
    
    func getInfoItems() async {
        try? await mensaManager.getInfoItems()
    }
    
    func getLocations() async {
        try? await mensaManager.getLocations()
    }
    
    func sendFeedback(tasteRating: Int, priceRating: Int, message: String, location: String, dishId: Int) async {
        try? await mensaManager.sendFeedback(tasteRating: Int32(tasteRating), priceRating: Int32(priceRating), message: message, location: location, dishId: Int32(dishId))
    }
    
    
    private func createDebugMenuItems() async {
        try? await mensaManager.createDebugMenuItems()
    }
    
    private func createDebugTeasers() async {
        try? await mensaManager.createDebugTeasers()
    }
    
    private func createDebugLocations() async {
        try? await mensaManager.createDebugLocations()
    }
}


struct SVGGeometry {
    struct Element {
        let path: Path
        let color: Color
    }

    let viewBox: CGRect
    let elements: [Element]
}

private func parseSVG(_ svg: String) -> SVGGeometry? {
    guard let data = svg.data(using: .utf8) else { return nil }

    let url = FileManager.default.temporaryDirectory
        .appendingPathComponent(UUID().uuidString)
        .appendingPathExtension("svg")

    defer { try? FileManager.default.removeItem(at: url) }

    do {
        try data.write(to: url)

        let paths = SVGBezierPath.pathsFromSVG(at: url)
        guard let first = paths.first else { return nil }

        return SVGGeometry(
            viewBox: first.viewBox,
            elements: paths.map {
                SVGGeometry.Element(
                    path: Path($0.cgPath),
                    color: $0.svgAttributes["fill"].map { Color(cgColor: $0 as! CGColor) } ?? .black
                )
            }
        )
    } catch {
        return nil
    }
}
