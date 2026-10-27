//  Created on 16.06.25.

import SwiftUI
import Shared
import SFSafeSymbols

// TODO make the Spacer in VStack animate shrinking
struct ExamRegisterView: View {
    @EnvironmentObject private var tucanService: TucanService
    @EnvironmentObject private var identityService: IdentityService
    @StateObject var examRegHelper = ExamRegHelper()
    
    @State private var isTabBarShown: Bool = true

    var body: some View {
        Group {
            if (identityService.tuLoginStatus == .mfaUnconfirmed) {
                LoginLoadingView()
            } else if tucanService.examRegStatus == .loading && tucanService.examRegistrations.isEmpty {
                RequestLoadingView(itemName: "Exam Registrations")
            } else {
                RefreshableScrollView(action: {
                    await tucanService.updateExamRegistrations()
                }, content: {
                    ScrollViewContent()
                })
                .safeAreaInset(edge: .bottom) {
                    ModifyButton()
                }
            }
        }
        .environmentObject(examRegHelper)
        
        .disabled(examRegHelper.isLoading)
        .frame(maxWidth: .infinity)
        .background(Color(.systemGroupedBackground).ignoresSafeArea())
        
        .onChange(of: examRegHelper.isSelectionMode) { isSelectionMode in
            withAnimation {
                isTabBarShown = !(isSelectionMode || examRegHelper.isLoading)
            }
        }
        .onChange(of: examRegHelper.isLoading) { isLoading in
            withAnimation {
                isTabBarShown = !(examRegHelper.isSelectionMode || isLoading)
            }
        }
        
        .navigationTitle(examRegHelper.isSelectionMode ? "Select Exams" : "Exam Registration")
        .navigationBarTitleDisplayMode(.inline)
        .navigationTitleAccessoryCleanup()
        .navigationBarBackButtonHidden(examRegHelper.isSelectionMode || examRegHelper.isLoading)
        .toolbar((isTabBarShown) ? .visible : .hidden, for: .tabBar)
        .toolbar {
            if (examRegHelper.isSelectionMode) {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Cancel", action: {
                        examRegHelper.isSelectionMode.toggle()
                        examRegHelper.selection.removeAll()
                        examRegHelper.selectionType = RegistrationOption.unavailable
                    })
                    .disabled(examRegHelper.isLoading)
                }
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Deselect All", action: {
                        examRegHelper.selection.removeAll()
                        examRegHelper.selectionType = RegistrationOption.unavailable
                    })
                    .disabled(examRegHelper.isLoading)
                }
            } else {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Select", action: {
                        examRegHelper.isSelectionMode.toggle()
                    })
                    .disabled(examRegHelper.isLoading)
                }
                if (examRegHelper.isLoading) {
                    ToolbarItem(placement: .navigationBarLeading) {
                        ProgressView()
                    }
                }
            }
        }
    }
}

extension ExamRegisterView {
    class ExamRegHelper: ObservableObject {
        @Published var isLoading: Bool = false
        @Published var isSelectionMode: Bool = false
        @Published var selectionType: RegistrationOption = RegistrationOption.unavailable
        @Published var selection: [ExamRegistration] = []
        
        func isSelected(examReg: ExamRegistration) -> Bool {
            selection.contains(where: { $0.id == examReg.id })
        }
    }
    
    struct ScrollViewContent: View {
        @EnvironmentObject private var tucanService: TucanService
        @EnvironmentObject private var examRegHelper: ExamRegHelper
        
        @State private var isSelected = false
        
        var body: some View {
            VStack {
                // For each semester
                ForEach(Array(tucanService.examRegistrations.enumerated()), id: \.element) { semesterIndex, moduleList in
                    HeaderTextNotLocal(moduleList.first?.first?.semester ?? "")
                    
                    GroupedList(padding: 12) {
                        ForEach(moduleList, id: \.self) { examRegList in
                            if (examRegList.count == 1) {
                                let examReg = examRegList[0]
                                let isSelected = examRegHelper.isSelected(examReg: examReg)
                                
                                RegisterItem(isSelected: isSelected, examReg: examReg, padding: 0)
                            }
                            else {
                                GroupDisclosureGroup(padding: 12, style: .dividerAligned) {
                                    Text(examRegList[0].courseName)
                                        .font(.subheadline)
                                } content: {
                                    ForEach(examRegList, id: \.id) { examReg in
                                        let isSelected = examRegHelper.isSelected(examReg: examReg)
                                        
                                        RegisterItem(isSelected: isSelected, examReg: examReg, padding: 36-8)
                                            .padding(.leading, -36)
                                    }
                                }
                                .padding(.leading, 36)
                            }
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
            }
        }
    }

    struct ModifyButton: View {
        @EnvironmentObject private var examRegHelper: ExamRegHelper
        @EnvironmentObject private var tucanService: TucanService
        
        @State private var modifyIDs: [String] = []
        @State private var alertRegText: String = ""
        @State private var modifyAlert: Bool = false
        
        var body: some View {
            VStack {
                if (examRegHelper.isSelectionMode && examRegHelper.selectionType != RegistrationOption.unavailable) {
                    let isRegister = (examRegHelper.selectionType == RegistrationOption.register)
                    
                    BigBlueButton(isRegister ? "Register" : "Unregister") {
                        getExamModifyPrompts(isRegister: isRegister)
                    }
                    .padding()
                    .disabled(examRegHelper.isLoading)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    
                    /*
                    Button(action: {
                        Task {
                            examRegHelper.isLoading = true
                            
                            var newModifyIDs: [String] = []
                            alertRegText = ""
                            
                            // This cannot be done concurrently as TUCaN would throw an ExecutionError
                            for examReg in examRegHelper.selection {
                                let modifyID = await tucanService.getExamModifyPrompt(isRegister: isRegister, args: examReg.registerArgs)
                                if (modifyID != "") {
                                    alertRegText.append("\n\n\(examReg.courseID) \(examReg.courseName)\n\(examReg.examType)")
                                    newModifyIDs.append(modifyID)
                                } else {
                                    print("Failure")
                                }
                            }
                            
                            modifyIDs = newModifyIDs
                            modifyAlert = true
                            examRegHelper.isLoading = false
                        }
                    }) {
                        Text(isRegister ? "Register" : "Unregister")
                            .padding(.vertical, 8)
                            .frame(maxWidth: .infinity)
                            .fontWeight(.semibold)
                    }
                    .buttonStyle(BorderedProminentButtonStyle())
                    .glassEffectIfAvailable()
                    .padding()
                    .disabled(examRegHelper.isLoading)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    */
                    
                    .alert(Text(isRegister ? "Confirm Registration" : "Confirm Unregistration"),
                           isPresented: $modifyAlert,
                           actions: {
                        Button("Cancel", role: .cancel, action: {})
                        Button(isRegister ? "Register" : "Unregister", action: { confirmExamModify() })
                            .keyboardShortcut(.defaultAction)
                    }, message: {
                        let actionWord = isRegister ? String(localized: "register") : String(localized: "unregister")
                        Text("You are about to \(actionWord) for:\(alertRegText)")
                    })
                }
            }
            .animation(.default.speed(1.5), value: examRegHelper.selectionType)
        }
        
        private func getExamModifyPrompts(isRegister: Bool) {
            Task {
                examRegHelper.isLoading = true
                
                var newModifyIDs: [String] = []
                alertRegText = ""
                
                // This cannot be done concurrently as TUCaN would throw an ExecutionError
                // The error would be displayed as a webpage with the text "ExecutionError"
                for examReg in examRegHelper.selection {
                    let modifyID = await tucanService.getExamModifyPrompt(isRegister: isRegister, args: examReg.registerArgs)
                    if (modifyID != "") {
                        alertRegText.append("\n\n\(examReg.courseID) \(examReg.courseName)\n\(examReg.examType)")
                        newModifyIDs.append(modifyID)
                    } else {
                        print("Failure")
                    }
                }
                
                modifyIDs = newModifyIDs
                modifyAlert = true
                examRegHelper.isLoading = false
            }
        }
        
        @MainActor
        private func completeModify() {
            examRegHelper.isLoading = false
            examRegHelper.isSelectionMode = false
            examRegHelper.selection.removeAll()
            examRegHelper.selectionType = RegistrationOption.unavailable
        }
        
        private func confirmExamModify() {
            Task {
                examRegHelper.isLoading = true
                
                // This might be possible concurrently
                for modifyID in modifyIDs {
                    _ = await tucanService.confirmExamModify(registerID: modifyID)
                }

                await withTaskGroup(of: Void.self) { group in
                    group.addTask {
                        await tucanService.updateExams()
                    }
                    group.addTask {
                        await tucanService.updateExamRegistrations()
                        await completeModify()
                    }
                }
            }
        }
    }
    
    struct RegisterItem: View {
        @EnvironmentObject private var examRegHelper: ExamRegHelper
        @State var isSelected: Bool
        
        let examReg: ExamRegistration
        let padding: CGFloat
        
        var body: some View {
            ZStack {
                InteractionLayer(isSelected: $isSelected, examReg: examReg, padding: padding) {
                    ContentLayer(courseName: examReg.courseName, examType: examReg.examType, isSelectionMode: examRegHelper.isSelectionMode)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .padding(.leading, padding)
                }
                ButtonLayer(isSelected: $isSelected, examReg: examReg)
                    .padding(.leading, padding)
            }
        }
    }
    
    
    struct ButtonLayer: View {
        @EnvironmentObject private var tucanService: TucanService
        @EnvironmentObject private var examRegHelper: ExamRegHelper

        @State private var isLoading: Bool = false
        @Binding var isSelected: Bool
        @State private var registerID: String = ""
        @State private var modifyAlert: Bool = false
        
        let examReg: ExamRegistration
        
        var body: some View {
            HStack {
                let isRegister = (examReg.registerOption == RegistrationOption.register)
                
                if (examReg.registerOption == RegistrationOption.unavailable) {
                    Spacer()
                        .frame(width: 36, height: 36)
                } else if (isLoading || (isSelected && examRegHelper.isLoading)) {
                    ProgressView()
                        .frame(width: 28, height: 28)
                }
                else if (examRegHelper.isSelectionMode) {
                    Button(action: {
                        selectExam()
                    }) {
                        if (isSelected) {
                            Image(systemName: isRegister ? "plus.circle.fill" : "minus.circle.fill")
                                .resizable()
                                .frame(width: 28, height: 28)
                                .fontWeight(.semibold)
                                .foregroundStyle(Color.accentColor)
                        } else {
                            ZStack {
                                Image(systemName: isRegister ? "plus" : "minus")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 12, height: 12)
                                    .fontWeight(.heavy)
                                    .foregroundStyle(Color.accentColor)
                                Image(systemSymbol: .circle)
                                    .resizable()
                                    .frame(width: 28, height: 28)
                                    .fontWeight(.light)
                                    .foregroundStyle(.tertiary)
                            }
                        }
                    }
                    .buttonStyle(PlainButtonStyle())
                    .disabled(examRegHelper.selectionType != examReg.registerOption && examRegHelper.selectionType != RegistrationOption.unavailable)
                    
                } else {
                        Button(action: {
                            getExamModifyPrompt(isRegister: isRegister)
                        }) {
                            ZStack {
                                Circle()
                                    .fill(Color(.tertiarySystemGroupedBackground))
                                    .frame(width: 28, height: 28)
                                Image(systemName: isRegister ? "plus" : "minus")
                                    .resizable()
                                    .aspectRatio(contentMode: .fit)
                                    .frame(width: 12, height: 12)
                                    .fontWeight(.heavy)
                                    .foregroundStyle(Color.accentColor)
                            }
                            /*Image(systemName: isRegister ? "plus" : "minus")
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                                .fontWeight(.bold)
                                .padding(.horizontal, 2)
                                .frame(width: 28, height: 28)
                                .background(Circle().fill(Color(.tertiarySystemGroupedBackground)))*/
                        }
                        
                        .alert(Text(isRegister ? "Confirm Registration" : "Confirm Unregistration"), isPresented: $modifyAlert,
                               actions: {
                            Button("Cancel", role: .cancel, action: {})
                            Button(isRegister ? "Register" : "Unregister", action: { confirmExamModify() })
                                .keyboardShortcut(.defaultAction)
                        }, message: {
                            let actionWord = isRegister ? String(localized: "register") : String(localized: "unregister")
                            Text("You are about to \(actionWord) for:\n\n\(examReg.courseID) \(examReg.courseName)\n\(examReg.examType)")
                        })
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            
            .onChange(of: examRegHelper.selection) { selection in
                isSelected = selection.contains(where: { $0.id == examReg.id })
            }
        }
        
        
        private func selectExam() {
            if (examRegHelper.selectionType == examReg.registerOption || examRegHelper.selectionType == RegistrationOption.unavailable) {
                if (isSelected) {
                    if let index = examRegHelper.selection.firstIndex(where: { $0.id == examReg.id }) {
                        examRegHelper.selection.remove(at: index)
                    }
                    if (examRegHelper.selection.isEmpty) {
                        examRegHelper.selectionType = RegistrationOption.unavailable
                    }
                }
                else {
                    examRegHelper.selection.append(examReg)
                    if (examRegHelper.selectionType == RegistrationOption.unavailable) {
                        examRegHelper.selectionType = examReg.registerOption
                    }
                }
            }
        }
        
        private func getExamModifyPrompt(isRegister: Bool) {
            Task {
                isLoading = true
                examRegHelper.isLoading = true
                
                registerID = await tucanService.getExamModifyPrompt(isRegister: isRegister, args: examReg.registerArgs)
                
                if (registerID != "") {
                    modifyAlert = true
                }
                
                examRegHelper.isLoading = false
                isLoading = false
            }
        }
        
        private func confirmExamModify() {
            Task {
                isLoading = true
                let success = await tucanService.confirmExamModify(registerID: registerID)
                
                if success {
                    await withTaskGroup(of: Void.self) { group in
                        group.addTask {
                            await tucanService.updateExams()
                        }
                        group.addTask {
                            await tucanService.updateExamRegistrations()
                        }
                    }
                }
            }
        }
    }
        
    struct InteractionLayer<Content: View>: View {
        @EnvironmentObject private var examRegHelper: ExamRegHelper
        
        @State private var isNavigating: Bool = false
        @Binding var isSelected: Bool
        
        let examReg: ExamRegistration
        let padding: CGFloat
        
        let content: () -> Content
        
        var body: some View {
            NavigationLinkButton(action: {
                if (examRegHelper.isSelectionMode) {
                    selectExam()
                } else {
                    Task {
                        isNavigating = true
                    }
                }
            }, content: {
                content()
            }, destination: {
                ExamRegDetailView(examReg: examReg)
            }, isNavigating: $isNavigating)
        }
        
        private func selectExam() {
            if (examRegHelper.selectionType == examReg.registerOption || examRegHelper.selectionType == RegistrationOption.unavailable) {
                if (isSelected) {
                    if let index = examRegHelper.selection.firstIndex(where: { $0.id == examReg.id }) {
                        examRegHelper.selection.remove(at: index)
                    }
                    if (examRegHelper.selection.isEmpty) {
                        examRegHelper.selectionType = RegistrationOption.unavailable
                    }
                }
                else {
                    examRegHelper.selection.append(examReg)
                    if (examRegHelper.selectionType == RegistrationOption.unavailable) {
                        examRegHelper.selectionType = examReg.registerOption
                    }
                }
            }
        }
    }
    
    struct ContentLayer: View {
        //@EnvironmentObject private var examRegHelper: ExamRegHelper
        //let examReg: ExamRegistration
        
        let courseName: String
        let examType: String
        let isSelectionMode: Bool
        
        var body: some View {
            HStack {
                VStack(alignment: .leading) {
                    //Text(examReg.courseName)
                    Text(courseName)
                        .lineLimit(1)
                        .font(.subheadline)
                    Text(examType)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(.leading, 36)
                
                Spacer()

                //if (!examRegHelper.isSelectionMode) {
                if (!isSelectionMode) {
                    Image(systemSymbol: .chevronRight)
                            .foregroundStyle(Color(.tertiaryLabel))
                            .font(.system(size: 14, weight: .semibold))
                            .padding(.trailing)
                            .transition(.move(edge: .trailing))
                } else {
                    Spacer()
                        .frame(width: 24)
                }
            }
            //.animation(.default.speed(2.5), value: examRegHelper.isSelectionMode)
            .animation(.default.speed(2.5), value: isSelectionMode)
        }
    }
}

#Preview("ContentLayer") {
    VStack {
        ExamRegisterView.ContentLayer(courseName: "Introduction to Preview", examType: "oral/written exam", isSelectionMode: false)
            .background(Color(.secondarySystemGroupedBackground))
    }
    .frame(maxHeight: .infinity)
    .background(Color(.systemGroupedBackground))
}


#Preview("Exam Register") {
    NavigationStack {
        ExamRegisterView()
    }
    .environmentObject(TucanService())
    .environmentObject(IdentityService(previewMode: true))
}
