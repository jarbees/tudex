//  Created on 03.05.25.

import SwiftUI
import Shared
import MapKit
import SFSafeSymbols

// This conversion to a type conforming to MKAnnotation and NSObject is only necessary for UIKitMapView aka for specifically iOS 16
// TODO: (When deprecating iOS 16) Remove CustomAnnotation Class + UIKitMapView  and use mapAnnotations directly
let annotations = mapAnnotations.map { annotation in
    CustomAnnotation(annotation.annotationTitle, coordinate: CLLocationCoordinate2D(latitude: annotation.latitude, longitude: annotation.longitude), clusteringID: annotation.clusteringID, buildingName: annotation.buildingName)
}

struct MapView: View {
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 49.87615, longitude: 8.6575),
        span: MKCoordinateSpan(latitudeDelta: 0.0087, longitudeDelta: 0.0087)
    )
    
    // Annotations are in MapAnnotations.swift
    // static let annotations: [CustomAnnotation] = ...
    
    @State private var maxSheetHeight: CGFloat = 0
    @State private var bottomSafeArea: CGFloat = 0
    @State private var sheetOffset: CGFloat = 0
    @State private var pinnedHeight: CGFloat = 0
    @State private var isPresented = false
    @FocusState private var isTextFieldFocused: Bool
    @State private var searchText = ""
    @State private var selectedAnnotation: CustomAnnotation? = nil
    
    @Namespace var animation
    
    
    var filteredAnnotations: [CustomAnnotation] {
        if searchText.isEmpty {
            return annotations
        } else {
            return annotations.filter { annotation in
                if let title = annotation.title {
                    let altTitle: String = title.replacingOccurrences(of: "|", with: "")
                    
                    return title.localizedCaseInsensitiveContains(searchText) ||
                           altTitle.localizedCaseInsensitiveContains(searchText) ||
                           annotation.buildingName.localizedCaseInsensitiveContains(searchText)
                } else {
                    return annotation.buildingName.localizedCaseInsensitiveContains(searchText)
                }
            }
        }
    }
    
    var body: some View {
        ZStack(alignment: .bottom) {
            GeneralMapView(region: $region, selectedAnnotation: $selectedAnnotation, annotations: annotations, filteredAnnotations: filteredAnnotations)
            VStack(spacing: 4) {
                VStack {
                    CampusSwitcher(region: $region)
                        .modify { view in
                            if #available(iOS 26.0, *) {
                                view.glassEffect(.regular.interactive())
                            } else {
                                view.background {
                                    Circle()
                                        .fill(Color(.secondarySystemGroupedBackground))
                                }
                            }
                        }
                    
                    if !isPresented {
                        Button(action: {
                            sheetOffset = 0
                            
                            withAnimation {
                                isPresented = true
                            }
                        }, label: {
                            Label("Search", systemSymbol: .magnifyingglass)
                                .frame(width: 44, height: 44)
                        })
                        .modify { view in
                            if #available(iOS 26.0, *) {
                                view.glassEffect(.regular.interactive())
                            } else {
                                view.background {
                                    Circle()
                                        .fill(Color(.secondarySystemGroupedBackground))
                                    
                                }
                            }
                        }
                        .matchedGeometryEffect(id: "mapSearch", in: animation)
                    }
                }
                .foregroundStyle(Color(.label))
                .labelStyle(.iconOnly)
                .font(.title3)
                .frame(maxWidth: .infinity, alignment: .trailing)
                .padding(8)
                .onChange(of: isPresented) { newPresentedState in
                    if newPresentedState == false {
                        searchText = ""
                    }
                }
                
                VStack {
                    if isPresented {
                        SheetView(
                            content: {
                                List {
                                    Section {
                                        ForEach(filteredAnnotations, id: \.self) { annotation in
                                                Button {
                                                    selectedAnnotation = annotation
                                                    withAnimation {
                                                        sheetOffset = 0
                                                    }
                                                    // dismiss keyboard
                                                    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder),
                                                                                    to: nil, from: nil, for: nil)
                                                } label: {
                                                    HStack(alignment: .top) {
                                                        Text("\(annotation.title ?? "Unknown")")
                                                        Text("\(annotation.buildingName)")
                                                    }
                                                }
                                                .foregroundStyle(Color(.label))
                                            }
                                            .listRowBackground(Color(.secondarySystemGroupedBackground).opacity(0.75))
                                        } header: {
                                            Spacer(minLength: 0).listRowInsets(EdgeInsets())
                                        } footer: {
                                            Spacer()
                                                .frame(height: bottomSafeArea)
                                        }
                                    }
                                    .environment(\.defaultMinListHeaderHeight, 0)
                                    .scrollContentBackground(.hidden)
                            }, pinned: {
                                VStack {
                                    FrozenSearchBar(animation: animation, searchText: $searchText)
                                        .focused($isTextFieldFocused)
                                        .onChange(of: isTextFieldFocused) { newFocusState in
                                            if newFocusState == true {
                                                withAnimation {
                                                    sheetOffset = maxSheetHeight
                                                }
                                            }
                                        }
                                        .onChange(of: sheetOffset) { newOffset in
                                            if sheetOffset <= 0 {
                                                withAnimation {
                                                    isTextFieldFocused = false
                                                }
                                            }
                                        }
                                }
                            }, maxSheetHeight: maxSheetHeight, bottomSafeArea: bottomSafeArea, pinnedHeight: $pinnedHeight, isPresented: $isPresented, sheetOffset: $sheetOffset
                        )
                    }
                }
            }
            .ignoresSafeArea()
        }
        .ignoresSafeArea(.keyboard)
        .background {
            GeometryReader { geo in
                Color.clear
                    .onAppear {
                        maxSheetHeight = geo.size.height - pinnedHeight
                        bottomSafeArea = geo.safeAreaInsets.bottom
                    }
                    .onChange(of: geo.size.height) { newHeight in
                        maxSheetHeight = newHeight - pinnedHeight
                    }
                    .onChange(of: pinnedHeight) { newPinnedHeight in
                        maxSheetHeight = geo.size.height - newPinnedHeight
                    }
                    .onChange(of: geo.safeAreaInsets.bottom) { newInset in
                        bottomSafeArea = newInset
                    }
            }
            .ignoresSafeArea(.keyboard)
        }
        .toolbarBackground(.visible, for: .tabBar)
        .toolbar(.hidden, for: .navigationBar)
    }
}

extension UIColor {
    func lighter(by percentage: CGFloat = 30.0) -> UIColor? {
        return self.adjust(by: abs(percentage) )
    }
    
    func darker(by percentage: CGFloat = 30.0) -> UIColor? {
        return self.adjust(by: -1 * abs(percentage) )
    }
    
    func adjust(by percentage: CGFloat = 30.0) -> UIColor? {
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0, alpha: CGFloat = 0
        if self.getRed(&red, green: &green, blue: &blue, alpha: &alpha) {
            return UIColor(red: min(red + percentage/100, 1.0),
                           green: min(green + percentage/100, 1.0),
                           blue: min(blue + percentage/100, 1.0),
                           alpha: alpha)
        } else {
            return nil
        }
    }
    
    func mix(with target: UIColor, amount: CGFloat) -> UIColor {
        var r1: CGFloat = 0, g1: CGFloat = 0, b1: CGFloat = 0, a1: CGFloat = 0
        var r2: CGFloat = 0, g2: CGFloat = 0, b2: CGFloat = 0, a2: CGFloat = 0
        
        /*
        let `self` = usingColorSpace(.sRGB)!
        let target = target.usingColorSpace(.sRGB)!
         */
        
        self.getRed(&r1, green: &g1, blue: &b1, alpha: &a1)
        target.getRed(&r2, green: &g2, blue: &b2, alpha: &a2)
        
        return Self(
            red: r1 * (1.0 - amount) + r2 * amount,
            green: g1 * (1.0 - amount) + g2 * amount,
            blue: b1 * (1.0 - amount) + b2 * amount,
            alpha: a1
        )
    }
}

class CustomAnnotation: NSObject, MKAnnotation {
    var title: String?
    var buildingName: String
    var coordinate: CLLocationCoordinate2D
    var clusteringIdentifier: String?

    init(_ title: String? = nil, coordinate: CLLocationCoordinate2D, clusteringID: String? = nil, buildingName: String = "") {
        self.title = title
        self.buildingName = buildingName
        self.coordinate = coordinate
        self.clusteringIdentifier = clusteringID
    }
}

/// A MapView that automatically decides between SwiftUI or UIKit Maps
struct GeneralMapView: View {
    @Binding var region: MKCoordinateRegion
    @Binding var selectedAnnotation: CustomAnnotation?
    var annotations: [CustomAnnotation]
    var filteredAnnotations: [CustomAnnotation]
    
    var body: some View {
        if #available(iOS 17, *) {
            SwiftUIMapView(region: $region, selectedAnnotation: $selectedAnnotation, annotations: annotations, filteredAnnotations: filteredAnnotations)
        } else {
            UIKitMapView(region: $region, selectedAnnotation: $selectedAnnotation, annotations: annotations, filteredAnnotations: filteredAnnotations)
                .ignoresSafeArea()
        }
    }
}

@available(iOS 17.0, *)
private struct SwiftUIMapView: View {
    @Binding var region: MKCoordinateRegion
    @Binding var selectedAnnotation: CustomAnnotation?
    
    @State private var mapCameraPosition: MapCameraPosition
    
    var annotations: [CustomAnnotation]
    var filteredAnnotations: [CustomAnnotation]
    
    
    init(region: Binding<MKCoordinateRegion>, selectedAnnotation: Binding<CustomAnnotation?>, annotations: [CustomAnnotation], filteredAnnotations: [CustomAnnotation]) {
        self._region = region
        self._selectedAnnotation = selectedAnnotation
        self.annotations = annotations
        self.filteredAnnotations = filteredAnnotations
        self._mapCameraPosition = State(initialValue: .region(region.wrappedValue))
    }
            
    var body: some View {
        Map(position: $mapCameraPosition, selection: $selectedAnnotation) {
            ForEach(annotations, id: \.self) { annotation in
                if filteredAnnotations.contains(annotation) {
                    Marker(coordinate: annotation.coordinate, label: { Text(annotation.title ?? "") })
                } else {
                    Marker(coordinate: annotation.coordinate, label: { Text(annotation.title ?? "") })
                        .tint(.gray)
                }
            }
        }
        .mapStyle(.standard(pointsOfInterest: .excludingAll))
        
        // Setting region to 1000 ensures that every time campus is switched, the region changes
        .onAppear {
            region.center.latitude = 1000
        }
        .onChange(of: region.center.latitude) { newRegion in
            if region.center.latitude != 1000 {
                withAnimation {
                    mapCameraPosition = .region(region)
                }
                region.center.latitude = 1000
            }
        }
        .onChange(of: selectedAnnotation) { newOptSelectedAnnotation in
            if let newSelectedAnnotation = newOptSelectedAnnotation {
                region.center = newSelectedAnnotation.coordinate
                region.span = MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
            }
        }
    }
}

private struct UIKitMapView: UIViewRepresentable {
    @Binding var region: MKCoordinateRegion
    @Binding var selectedAnnotation: CustomAnnotation?
    var annotations: [CustomAnnotation]
    var filteredAnnotations: [CustomAnnotation]
    
    func makeUIView(context: Context) -> MKMapView {
        let mapView = MKMapView()
        mapView.delegate = context.coordinator
        mapView.region = region
        mapView.pointOfInterestFilter = .excludingAll
        
        mapView.addAnnotations(annotations)
        //mapView.selectableMapFeatures = [.pointsOfInterest]
        
        return mapView
    }
    
    func updateUIView(_ uiView: MKMapView, context: Context) {
        //uiView.addAnnotations(annotations)
        uiView.setRegion(region, animated: true)
        
        for annotation in annotations {
            if let view = uiView.view(for: annotation) as? MKMarkerAnnotationView {
                if filteredAnnotations.contains(where: { $0 === annotation }) {
                    view.markerTintColor = nil      // Makes them a specific default red
                } else {
                    view.markerTintColor = UIColor.systemGray
                }
            }
        }
        
        if let annotation = selectedAnnotation {
            if let annotation = annotations.first(where: { $0 == annotation }) {
                uiView.selectAnnotation(annotation, animated: true)
                
                // TODO Make span differ for each Location
                let newRegion = MKCoordinateRegion(center: annotation.coordinate, span: uiView.region.span)
                uiView.setRegion(newRegion, animated: true)
                
                // Reset selectedAnnotation so that when the selection is dismissed, the View doesn't reselect the Annotation
                DispatchQueue.main.async {
                    selectedAnnotation = nil
                }
            }
        }
    }
    
    
    func makeCoordinator() -> Coordinator {
        return Coordinator(self)
    }
    
    class Coordinator: NSObject, MKMapViewDelegate {
        var parent: UIKitMapView
        
        init(_ parent: UIKitMapView) {
            self.parent = parent
        }
        
        func mapView(_ mapView: MKMapView, regionDidChangeAnimated animated: Bool) {
            parent.region = mapView.region
        }
        
        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            if annotation is MKUserLocation {
                return nil
            }
            
            if let cluster = annotation as? MKClusterAnnotation {
                // Get clustering ID from the first member annotation, if any
                let clusteringID = (cluster.memberAnnotations.first as? CustomAnnotation)?.clusteringIdentifier ?? "Cluster"
                let clusterCount = cluster.memberAnnotations.count
                
                cluster.title = "\(clusteringID)"
                cluster.subtitle = "\(clusterCount) buildings"
                
                let clusterView = MKMarkerAnnotationView(annotation: cluster, reuseIdentifier: "cluster")
                //clusterView.markerTintColor = .systemBlue
                clusterView.glyphText = "\(cluster.memberAnnotations.count)"
                clusterView.markerTintColor = .systemRed
                return clusterView
            }
            
            let reuseID = "customAnnotationView"
            var annotationView = mapView.dequeueReusableAnnotationView(withIdentifier: reuseID) as? MKMarkerAnnotationView
            
            if annotationView == nil {
                annotationView = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: reuseID)
            } else {
                annotationView?.annotation = annotation
            }
            
            if parent.filteredAnnotations.contains(where: { $0 === annotation }) {
                annotationView?.markerTintColor = nil       // Makes them a specific default red
            } else {
                annotationView?.markerTintColor = .systemGray
            }
            
            /*
            if let customAnnotation = annotation as? CustomAnnotation {
                //if parent.selectedAnnotation == customAnnotation {
                //    annotationView?.clusteringIdentifier = nil
                //} else {
                annotationView?.clusteringIdentifier = customAnnotation.clusteringIdentifier
                //}
            }
            */
            
            return annotationView
        }

        
        /*
        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            // Dequeue a reusable annotation view or create a new one
            if let dequeuedView = mapView.dequeueReusableAnnotationView(withIdentifier: "customAnnotation") {
                return dequeuedView
            }
            
            // Create and configure a custom annotation view
            let annotationView = CustomAnnotationView(annotation: annotation, reuseIdentifier: "customAnnotation")
            return annotationView
        }*/
    }
}


// This is used for displaying a map in Compose Multiplatform
#if DEBUG
struct ComposeMapView: View {
    @State private var region = MKCoordinateRegion(
        center: CLLocationCoordinate2D(latitude: 49.87615, longitude: 8.6575),
        span: MKCoordinateSpan(latitudeDelta: 0.0087, longitudeDelta: 0.0087)
    )
    
    @State private var selectedAnnotation: CustomAnnotation? = nil
    
    let annotations: [CustomAnnotation]
    let filteredAnnotations: [CustomAnnotation]
    
    var body: some View {
        MapView()
    }
}
#endif


private struct CampusSwitcher: View {
    let orderedRegions = ["Stadtmitte", "Lichtwiese", "Botanischer Garten", "Hochschulstadion", "Windkanal"]
    let regions: [String: (center: CLLocationCoordinate2D, span: MKCoordinateSpan)] = [
        "Stadtmitte":
            (CLLocationCoordinate2D(latitude: 49.87615, longitude: 8.6575),
             MKCoordinateSpan(latitudeDelta: 0.0087, longitudeDelta: 0.0087)),
        "Lichtwiese":
            (CLLocationCoordinate2D(latitude: 49.861, longitude: 8.6805),
             MKCoordinateSpan(latitudeDelta: 0.01075, longitudeDelta: 0.01075)),
        "Botanischer Garten":
            (CLLocationCoordinate2D(latitude: 49.8692, longitude: 8.68105),
             MKCoordinateSpan(latitudeDelta: 0.003795, longitudeDelta: 0.003795)),
        "Hochschulstadion":
            (CLLocationCoordinate2D(latitude: 49.8602, longitude: 8.6713),
             MKCoordinateSpan(latitudeDelta: 0.00675, longitudeDelta: 0.00675)),
        "Windkanal":
            (CLLocationCoordinate2D(latitude: 49.8582, longitude: 8.5923),
             MKCoordinateSpan(latitudeDelta: 0.0098, longitudeDelta: 0.0098))
    ]
    
    @Binding var region: MKCoordinateRegion
    
    var body: some View {
        Menu(content: {
            ForEach(orderedRegions.reversed(), id: \.self) { regionName in
                Button(regionName) {
                    let newRegion = regions[regionName]!
                    region = MKCoordinateRegion(
                        center: newRegion.center,
                        span: newRegion.span
                    )
                }
            }
        }, label: {
            Label("Switch Campus", systemSymbol: .mappinAndEllipse)
                .frame(width: 44, height: 44)
        })
    }
}

private struct SheetView<Content: View, Pinned: View>: View {
    let content: () -> Content
    let pinned: () -> Pinned
    
    let maxSheetHeight: CGFloat
    let bottomSafeArea: CGFloat
    
    @Binding var pinnedHeight: CGFloat
    @Binding var isPresented: Bool
    @Binding var sheetOffset: CGFloat
    
    @GestureState private var dragTranslation: CGFloat = 0
    
    // This view is built in this way so that by default only the upper VStack is draggable and visible, but everything reacts to gesture
    var body: some View {
        VStack(spacing: 0) {
            FakeGripHandle(sheetOffset: $sheetOffset, maxSheetHeight: maxSheetHeight)
            pinned()
        }
        .background {
            GeometryReader { geo in
                Color.clear
                    .onAppear {
                        pinnedHeight = geo.size.height
                    }
                    .onChange(of: geo.size.height) { newHeight in
                        pinnedHeight = newHeight
                    }
            }
        }
        .offset(y: -sheetOffset + dragTranslation)
        .highPriorityGesture(
            DragGesture()
                .updating($dragTranslation) { value, state, _ in
                    state = value.translation.height
                }
                .onEnded { value in
                    let velocity = value.velocity.height
                    let translationHeight = value.translation.height
                    
                    sheetOffset = sheetOffset - translationHeight
                    
                    if sheetOffset < -50 {
                        withAnimation {
                            isPresented = false
                        }
                    } else if sheetOffset < 0 {
                        withAnimation {
                            sheetOffset = 0
                        }
                    } else if abs(velocity) > 700 {
                        withAnimation(.bouncy) {
                            sheetOffset = velocity < 0 ? maxSheetHeight : 0
                        }
                    } else {
                        withAnimation {
                            sheetOffset = sheetOffset > maxSheetHeight/2 ? maxSheetHeight : 0
                        }
                    }
                }
        )
        .frame(maxWidth: .infinity)
        .background(alignment: .top) {
            content()
                .frame(height: max(sheetOffset + bottomSafeArea - dragTranslation, 0))
                /*
                .modify { view in
                    if #available(iOS 26.0, *) {
                        //view.frame(height: max(sheetOffset + percentage * bottomSafeArea, percentage * bottomSafeArea))
                        //view.frame(height: sheetOffset + percentage * bottomSafeArea)
                        view.frame(height: max(sheetOffset + bottomSafeArea - dragTranslation, 0))
                        //.clipped()
                    } else {
                        //view.frame(height: max(sheetOffset + bottomSafeArea, bottomSafeArea))
                        view.frame(height: sheetOffset + bottomSafeArea - dragTranslation)
                    }
                }
                */
                .padding(.top, pinnedHeight)
                .modify { view in
                    if #available(iOS 26.0, *) {
                        view.background (
                            .regularMaterial,
                            in: ConcentricRectangle(corners: .concentric(minimum: 28))
                        )
                    } else {
                        view.background (
                            .regularMaterial,
                            in: UnevenRoundedRectangle(topLeadingRadius: 10, bottomLeadingRadius: 0, bottomTrailingRadius: 0, topTrailingRadius: 10)
                        )
                    }
                }
                .offset(y: -sheetOffset + dragTranslation)
        }
        .transition(.move(edge: .bottom))
    }
}

private struct FakeGripHandle: View {
    @Binding var sheetOffset: CGFloat
    let maxSheetHeight: CGFloat
    
    var body: some View {
        Rectangle()
            .fill(Color(.tertiaryLabel))
            .frame(width: 36, height: 5)
            .cornerRadius(12)
            .padding(.top, 5)
            .padding(.bottom, 12)
            .onTapGesture {
                withAnimation {
                    sheetOffset = maxSheetHeight
                }
            }
    }
}

private struct FrozenSearchBar: View {
    let animation: Namespace.ID
    
    @Binding var searchText: String
    
    var body: some View {
        HStack {
            Image(systemSymbol: .magnifyingglass)
                .foregroundStyle(Color(.secondaryLabel))
                .matchedGeometryEffect(id: "mapSearch", in: animation)
            
            TextField(text: $searchText) {
                Text("Building...")
                    .foregroundStyle(Color(.secondaryLabel))
            }
            .autocorrectionDisabled()
        }
        .padding(8)
        .background(Color(.secondarySystemGroupedBackground).opacity(0.75))
        .rounded(oldCornerRadius: 8)
        .padding(.top, -4)
        .padding(.bottom)
        .padding(.horizontal)
    }
}

#Preview {
    TabView {
        NavigationStack {
            MapView()
        }
        .tabItem {
            Label("Map", systemSymbol: .map)
        }
        DemoView()
            .tabItem {
                Label("Demo", systemSymbol: .globe)
            }
        DemoView()
            .tabItem {
                Label("Demo", systemSymbol: .globe)
            }
        DemoView()
            .tabItem {
                Label("Demo", systemSymbol: .globe)
            }
        DemoView()
            .tabItem {
                Label("Demo", systemSymbol: .globe)
            }
    }
}


