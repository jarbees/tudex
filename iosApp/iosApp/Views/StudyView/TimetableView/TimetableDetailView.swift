//  Created on 17.12.25.

import SwiftUI
import Shared
import MapKit

struct MarkerPlaceholder: Identifiable {
    let id = UUID()          // stable identity
    var coordinate: CLLocationCoordinate2D
}

struct TimetableDetailView: View {
    let date: EventDate
    
    let optAnnotation: CustomAnnotation?

    init(date: EventDate) {
        self.date = date
        
        if let courseDate = date as? EventDate.CourseDate {
            self.optAnnotation = annotations
                .filter({ annotation in
                    courseDate.room
                        .replacingOccurrences(of: "|", with: "")
                        .contains(annotation.title?.replacingOccurrences(of: "|", with: "") ?? "Unmatchable text")
                }).first
        } else {
            self.optAnnotation = nil
        }
    }
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading) {
                VStack(alignment: .leading, spacing: 0) {
                    Text(date.courseName)
                        .font(.title2)
                        .fontWeight(.semibold)
                    
                    if let courseDate = date as? EventDate.CourseDate {
                        Text(courseDate.teachers)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.bottom, 8)
                    }
                    if let examDate = date as? EventDate.ExamDate {
                        Text(examDate.examType)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                            .padding(.bottom, 8)
                    }
                    
                    Text("\(date.date), \(date.startTime)-\(date.endTime)")
                        .font(.callout)
                        .padding(.bottom, 2)
                    
                    if let courseDate = date as? EventDate.CourseDate {
                        Text(courseDate.room)
                            .font(.callout)
                    }
                }
                .padding()
                
                TimetableCutout(date: date, startTimeMins: Int(date.startTimeMins))
                    .rounded()
                    .padding(.bottom)
                
                if let annotation = optAnnotation {
                    Button {
                        let placemark = MKPlacemark(coordinate: annotation.coordinate)
                        let mapItem = MKMapItem(placemark: placemark)
                        mapItem.name = "\(annotation.title ?? "") - \(annotation.buildingName)"
                        mapItem.pointOfInterestCategory = .university
                        
                        mapItem.openInMaps()
                    } label: {
                        GeneralMapView(
                            region: Binding(
                                get: {
                                    MKCoordinateRegion(
                                        center: annotation.coordinate,
                                        span: MKCoordinateSpan(latitudeDelta: 0.002, longitudeDelta: 0.002)
                                    )}, set: { _ in }),
                            selectedAnnotation: Binding(get: { annotation }, set: { _ in }), annotations: [annotation], filteredAnnotations: [annotation])
                        .frame(height: 200)
                        .overlay {
                            Color.clear
                                .contentShape(Rectangle())
                        }
                    }
                    .rounded()
                }
                
                
                Spacer()
            }
            .padding(.horizontal)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        // These are used to add an invisible navigationTitle, so that the back button can label the views
        .navigationTitle(date.courseName)
        .navigationBarTitleDisplayMode(.inline)
        .navigationTitleAccessoryCleanup()
        .toolbar {
            ToolbarItem(placement: .principal) {
                Text("")
            }
        }
        .background(Color(.systemGroupedBackground))
    }
}

extension TimetableDetailView {
    struct MarkerPlaceholder: Identifiable {
        let id = "Placeholder"
    }
    
    struct TimetableCutout: View {
        let date: EventDate
        let startTimeMins: Int
        
        @State private var metrics = TimetableView.TimeGridMetrics()
        
        @EnvironmentObject var tucanService: TucanService
        @ScaledMetric private var rowPadding: CGFloat = 12
        
        var body: some View {
            TimetableView.TimeGridView(metrics: $metrics)
            .overlay(alignment: .top) {
                ZStack(alignment: .top) {
                    if let datesListSortedWrapper = tucanService.datesListSortedWrapper,
                       let dateFormatter = TimetableView.getDateFormatter(locale: datesListSortedWrapper.locale)
                    {
                        let dayString = dateFormatter.string(from: .now)
                        
                        if dayString == date.date {
                            TimetableView.TimeMarker(
                                topOffset: metrics.topOffset,
                                rowHeight: metrics.rowHeight
                            )
                            .padding(.leading, -8)
                            .padding(.trailing, -16)
                        }
                        
                        let sortedDatesLists = datesListSortedWrapper.datesListSorted
                        let datesMap: [String: [EventDate]] =
                            Dictionary(
                                uniqueKeysWithValues:
                                    sortedDatesLists.compactMap { datesList in
                                        guard let first = datesList.first else { return nil }
                                        return (first.date, datesList)
                                    }
                            )
                        
                        
                        if let dates = datesMap[date.date] {
                            TimetableView.TimeSlotViewCoordinator(
                                dates: dates,
                                topOffset: metrics.topOffset,
                                rowHeight: metrics.rowHeight,
                                selectedDate: date
                            )
                            .padding(.leading, metrics.leadingPadding + 16)
                        }
                    }
                }
            }
            .padding(.horizontal)
            .offset(y: -metrics.rowHeight * (CGFloat(startTimeMins)/60))
            .frame(height: 200, alignment: .top)
            .clipped()
            .background(Color(.secondarySystemGroupedBackground))
        }
    }
}

#Preview {
    NavigationStack {
        TimetableDetailView(date: EventDate.CourseDate(courseName: "Mathematik II (für ET)", date: "Mi, 23. Dez. 2025", startTime: "13:40", endTime: "15:10", startTimeMins: 0, endTimeMins: 0, room: "S310|051", teachers: "Mr Teacher"))
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Image(systemSymbol: .chevronLeft)
                }
            }
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
    .environmentObject(SettingsService())
}
