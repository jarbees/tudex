//  Created on 28.06.25.

import SwiftUI
import Shared
import Charts

struct ExamDetailView: View {
    @State private var selectedGrade: (String, Int)?
    @State private var selectedGradePositionX: CGFloat = 0
    @State private var barMiddleXList: [CGFloat] = []
    
    @EnvironmentObject var tucanService: TucanService
    
    let exam: Exam
        
    var body: some View {
        ScrollView {
            VStack(alignment: .leading) {
                HeaderText("Assessment")
                Text(exam.grade == "" ? "Unknown" : "\(exam.gradeName) (\(exam.grade))")
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.vertical, 12)
                    .padding(.leading)
                    .background(Color(.secondarySystemGroupedBackground))
                    .rounded()
                    //.cornerRadius(10)
                    .padding(.horizontal)
                    .padding(.bottom)
                
                if let gradeStats = exam.gradeStats {
                    // Convert exam.gradeStats.grades: [KotlinPair<NSString, KotlinInt>] to [(String, Int)]
                    let grades: [(String, Int)] = {
                        gradeStats.grades.map { (String($0.first ?? "Name"), Int(truncating: $0.second ?? -10)) }
                    }()
                    
                    HeaderText("Grade Report")
                    if !(gradeStats.grades.isEmpty && gradeStats.gradeAvg == nil && gradeStats.resultCount == nil) {
                        VStack(alignment: .leading) {
                            ChartInfoView(selectedGrade: selectedGrade, selectedGradePositionX: selectedGradePositionX, exam: exam)
                                .zIndex(1)
                            if (!gradeStats.grades.isEmpty) {
                                ChartView(barMiddleXList: $barMiddleXList, selectedGradePositionX: selectedGradePositionX, selectedGrade: selectedGrade, grades: grades)
                                // Update cursor position when user taps or drags across chart
                                    .gesture (
                                        SpatialTapGesture()
                                            .onEnded { value in
                                                updateSelectedGrade(at: value.location.x - 16)
                                            }
                                            .exclusively(before: DragGesture(minimumDistance: 0)
                                                .onChanged { value in
                                                    updateSelectedGrade(at: value.location.x - 16)
                                                })
                                    )
                                
                                FailPassBar(grades: grades, gradeStats: gradeStats)
                            }
                        }
                        .padding()
                        .background(Color(.secondarySystemGroupedBackground))
                        .rounded()
                        //.cornerRadius(10)
                        .padding(.horizontal)
                        .padding(.bottom, 8)
                        // Update cursor position when Chart is resized
                        .onChange(of: barMiddleXList) { newBarMiddleXList in
                            if let index: Int = grades.firstIndex(where: { $0 == selectedGrade ?? ("No Match", -1) }) {
                                selectedGradePositionX = newBarMiddleXList[index]
                            } else {
                                selectedGradePositionX = 0
                            }
                        }
                    }
                    
                    AdditionalInfoView(exam: exam)
                        .padding(.bottom)
                }
            }
            // reset Chart Focus when tapping outside of Chart
            .onTapGesture {
                selectedGradePositionX = 0
                selectedGrade = nil
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitleAdaptive(exam.courseName) {
            VStack {
                Text("\(exam.courseID)")
                    .foregroundStyle(.secondary)
                Text("\(exam.courseName)")
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
                Text(exam.examinationDateTime)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .center)
            .modify { view in
                if #available(iOS 26.0, *) {
                    view.padding(.top, -16)
                } else {
                    view
                }
            }
        }
        .navigationTitleAccessoryCleanup()
        .onAppear {
            tucanService.updateViewedExam(exam: exam)
        }
        .onDisappear {
            tucanService.updateViewedExam(exam: nil)
        }
    }
    
    private func updateSelectedGrade(at locationX: CGFloat) {
        var closestIndex = 0
        var minDifference = abs(locationX - barMiddleXList[0])
        
        for (index, value) in barMiddleXList.enumerated() {
            let difference = abs(locationX - value)
            
            if (difference <= minDifference) {
                minDifference = difference
                closestIndex = index
            }
            else {
                break
            }
        }
        
        if closestIndex >= 0 && closestIndex < exam.gradeStats?.grades.count ?? -1 {
            let grade = exam.gradeStats!.grades[closestIndex]
            
            selectedGradePositionX = barMiddleXList[closestIndex]
            selectedGrade = (String(grade.first!), Int(truncating: grade.second!))
        } else {
            selectedGradePositionX = 0
            selectedGrade = nil
        }
    }
}

extension ExamDetailView {
    
    struct ChartInfoView: View {
        @State private var bubbleWidth: CGFloat = 0
        
        let selectedGrade: (String, Int)?
        let selectedGradePositionX: CGFloat
        
        let exam: Exam
        
        var body: some View {
            HStack(spacing: 0) {
                Spacer()
                    .frame(maxWidth: max(selectedGradePositionX - (bubbleWidth / 2), 0))
                VStack {
                    ZStack {
                        ZStack {
                            VStack(alignment: .leading) {
                                Text("_")
                                    .font(.callout)
                                Text("_")
                                    .font(.title2.bold())
                            }
                            .padding(.all)
                            
                            VStack(alignment: .leading) {
                                Text("_")
                                    .font(.caption)
                                    .lineLimit(1)
                                    .padding(.bottom, 2)
                                
                                HStack {
                                    Text("_")
                                    Text("_")
                                        .font(.title3.bold())
                                }
                                HStack {
                                    Text("_")
                                    Text("_")
                                        .font(.title3.bold())
                                }
                            }
                            .padding(.bottom, 8)
                        }
                        .foregroundStyle(.clear)
                        
                        if let selectedGrade = selectedGrade {
                            VStack(alignment: .leading) {
                                Text("\(selectedGrade.0)")
                                    .font(.callout)
                                    .foregroundStyle(.secondary)
                                
                                let gradeCount = Double(selectedGrade.1)
                                if let totalCountStr = exam.gradeStats?.resultCount,
                                   let totalCount = Double(totalCountStr),
                                   totalCount != 0
                                {
                                    let percentage = Int(round((gradeCount / totalCount) * 100))
                                    Text("\(selectedGrade.1) (\(percentage)%)")
                                        .font(.title2.bold())
                                        .foregroundColor(.primary)
                                } else {
                                    Text("\(selectedGrade.1)")
                                        .font(.title2.bold())
                                        .foregroundColor(.primary)
                                }
                            }
                            .padding(.all)
                            .background(Color(.tertiarySystemGroupedBackground))
                            .rounded(liquidCornerRadius: 16)
                            //.cornerRadius(10)
                            .background {
                                GeometryReader { geometry in
                                    Color.clear
                                        .onAppear {
                                            bubbleWidth = geometry.size.width
                                        }
                                        .onChange(of: geometry.size.width) { newWidth in
                                            bubbleWidth = newWidth
                                        }
                                }
                            }
                        } else {
                            VStack(alignment: .leading) {
                                Text("\(exam.courseName)")
                                    .font(.caption)
                                    .lineLimit(1)
                                    .foregroundStyle(.secondary)
                                    .padding(.bottom, 2)
                                
                                if !(exam.gradeStats?.resultCount == nil && exam.gradeStats?.gradeAvg == nil) {
                                    HStack {
                                        Text("Exams")
                                            .foregroundStyle(.secondary)
                                        Text(exam.gradeStats?.resultCount ?? "???")
                                            .font(.title3.bold())
                                            .foregroundStyle(.primary)
                                    }
                                    HStack {
                                        Text("Average")
                                            .foregroundStyle(.secondary)
                                        Text(exam.gradeStats?.gradeAvg ?? "???")
                                            .font(.title3.bold())
                                            .foregroundStyle(.primary)
                                    }
                                } else {
                                    Spacer()
                                }
                            }
                            .padding(.bottom, 8)
                        }
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
    
    struct ChartView: View {
        @Binding var barMiddleXList: [CGFloat]
        
        let selectedGradePositionX: CGFloat
        let selectedGrade: (String, Int)?
        let grades: [(String, Int)]
        
        var body: some View {
            
            Chart {
                ForEach(grades, id: \.0) { grade in
                    BarMark (
                        x: .value("Grade", grade.0),
                        y: .value("Count", grade.1)
                    )
                }
            }
            .frame(height: 300)
            .chartXAxis {
                AxisMarks(values: .automatic) {
                    AxisValueLabel()
                }
            }
            .chartOverlay { proxy in
                let newMiddleX: [CGFloat] = grades.map { grade in
                    proxy.position(forX: grade.0) ?? 0
                }
                
                Task {
                    barMiddleXList = newMiddleX
                }
                
                return Color.clear
            }
            .chartBackground { proxy in
                GeometryReader { geometry in
                    if selectedGrade != nil {
                        let lineHeight = geometry[proxy.plotAreaFrame].maxY + 16
                        
                        Rectangle()
                            .fill(.quaternary)
                            .frame(width: 2, height: lineHeight)
                            .offset(x: selectedGradePositionX - 1, y: -16)
                    }
                }
            }
        }
    }
    
    struct FailPassBar: View {
        let grades: [(String, Int)]
        let gradeStats: GradeStats
        
        var body: some View {
            if let resultCountStr = gradeStats.resultCount,
               let resultCount = Float(resultCountStr),
               resultCount != 0,
               let failedGrade = grades.first(where: { $0.0 == "nb" || $0.0 == "5,0" })
            {
                HStack {
                    Text("Pass\n\(100 - Int(round(Float(failedGrade.1 * 100) / resultCount)))%")
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundStyle(Color.accentColor)
                    
                    Chart {
                        Plot {
                            BarMark(
                                x: .value("Pass", 1 - (Float(failedGrade.1) / resultCount))
                            )
                            .foregroundStyle(Color.accentColor)
                        }
                    }
                    .chartPlotStyle { plotArea in
                        plotArea
                            .background(Color(.systemFill))
                            .cornerRadius(8)
                    }
                    .chartXAxis(.hidden)
                    .chartXScale(domain: 0...1)
                    .frame(height: 24)
                    
                    Text("Fail\n\(Int(round(Float(failedGrade.1 * 100) / resultCount)))%")
                        .font(.subheadline)
                        .fontWeight(.bold)
                }
            }
        }
    }
    
    struct AdditionalInfoView: View {
        let exam: Exam
        
        var body: some View {
            if let otherCounts: [(String, Int?)] = exam.gradeStats?.otherCounts.map({ count in
                let string: String = String(count.first!)
                let int: Int? = if let int = count.second { Int(truncating: int) } else { nil }
                return (string, int)
            }) {
                GroupedList(padding: 12) {
                    ForEach(otherCounts, id: \.0) { otherCount in
                        if let amount = otherCount.1 {
                            Text("\(otherCount.0): \(amount)")
                        } else {
                            Text(otherCount.0)
                        }
                    }
                }
                .padding(.horizontal)
            }
        }
    }
}

#Preview("D1") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [
                KotlinPair(first: "1,0", second: 1),
                KotlinPair(first: "1,3", second: 3),
                KotlinPair(first: "1,7", second: 5),
                KotlinPair(first: "2,0", second: 7),
                KotlinPair(first: "2,3", second: 10),
                KotlinPair(first: "2,7", second: 20),
                KotlinPair(first: "3,0", second: 30),
                KotlinPair(first: "3,3", second: 40),
                KotlinPair(first: "3,7", second: 70),
                KotlinPair(first: "4,0", second: 100),
                KotlinPair(first: "5,0", second: 150)
            ],
            gradeAvg: "3,98",
            resultCount: "436",
            otherCounts: [
                KotlinPair(first: "Ergebnisse mit abweichendem BWS", second: 6),
                KotlinPair(first: "Fehlend (Krankschreibung)", second: 6),
                KotlinPair(first: "Fehlend (Ohne Grund)", second: 6)
            ]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}

#Preview("D2") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [
                KotlinPair(first: "b", second: 20),
                KotlinPair(first: "nb", second: 50)
            ],
            gradeAvg: "1,0",
            resultCount: "70",
            otherCounts: [KotlinPair(first: "fehlend (Krankschreibung)", second: 6)]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}

#Preview("A1") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [],
            gradeAvg: nil,
            resultCount: nil,
            otherCounts: [KotlinPair(first: "nicht gesetzt", second: nil)]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}

#Preview("A2") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [
                KotlinPair(first: "1,0", second: 1),
                KotlinPair(first: "1,3", second: 3),
                KotlinPair(first: "1,7", second: 5),
                KotlinPair(first: "2,0", second: 7),
                KotlinPair(first: "2,3", second: 10),
                KotlinPair(first: "2,7", second: 20),
                KotlinPair(first: "3,0", second: 30),
                KotlinPair(first: "3,3", second: 40),
                KotlinPair(first: "3,7", second: 70),
                KotlinPair(first: "4,0", second: 100),
                KotlinPair(first: "5,0", second: 150)
            ],
            gradeAvg: nil,
            resultCount: nil,
            otherCounts: [KotlinPair(first: "nicht gesetzt", second: nil)]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}

#Preview("A3") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [],
            gradeAvg: "1,0",
            resultCount: nil,
            otherCounts: [KotlinPair(first: "nicht gesetzt", second: nil)]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}

#Preview("A4") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [],
            gradeAvg: "1,0",
            resultCount: "0",
            otherCounts: [KotlinPair(first: "nicht gesetzt", second: nil)]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}

#Preview("A5") {
    let exam = Exam(
        semester: "SoSe 2029",
        courseID: "20-0000-01",
        courseName: "Introduction to Computer",
        examType: "Oral/Written Exam",
        examinationDateTime: "",
        grade: "1,0",
        gradeName: "sehr gut",
        gradeSheetArgs: nil,
        gradeStats: GradeStats(
            grades: [
                KotlinPair(first: "1,0", second: 1),
                KotlinPair(first: "1,3", second: 3),
                KotlinPair(first: "1,7", second: 5),
                KotlinPair(first: "2,0", second: 7),
                KotlinPair(first: "2,3", second: 10),
                KotlinPair(first: "2,7", second: 20),
                KotlinPair(first: "3,0", second: 30),
                KotlinPair(first: "3,3", second: 40),
                KotlinPair(first: "3,7", second: 70),
                KotlinPair(first: "4,0", second: 100),
                KotlinPair(first: "5,0", second: 150)
            ],
            gradeAvg: nil,
            resultCount: "0",
            otherCounts: [KotlinPair(first: "nicht gesetzt", second: nil)]
        )
    )
    
    NavigationStack {
        ExamDetailView(exam: exam)
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}
