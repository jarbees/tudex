//  Created on 30.12.25.

import SwiftUI

struct TotpCodeView: View {
    @EnvironmentObject var identityService: IdentityService
    
    var body: some View {
        Group {
            if !identityService.totpSecretExists {
                LoginLockedView()
            } else {
                VStack {
                    TotpCodeCard(getTotpCodePair: {
                        return identityService.getTotpCodePair() ?? (current: "Error", next: "Error")
                    })
                    Spacer()
                }
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("2FA Code")
        .navigationTitleAccessory(accessory: { isLarge in
            Group {
                if identityService.tuLoginStatus == .mfaUnconfirmed {
                    ProgressView()
                        .scaleEffect(isLarge ? 1.2 : 1)
                        .padding(.top, isLarge ? 0 : 2)
                        .padding(.leading, isLarge ? 8 : 4)
                }
            }
        })
    }
}

struct TotpCodeCard: View {
    let getTotpCodePair: () -> (current: String, next: String)
    
    @State private var timeRemaining = 29 - Calendar.current.component(.second, from: .now) % 30
    
    @Environment(\.colorScheme) private var colorScheme
    
    @State private var totpCodePair = (current: "", next: "")
    
    var body: some View {
        TimelineView(.periodic(from: .now, by: 1)) { context in
            VStack {
                VStack(alignment: .leading) {
                    Spacer()
                    
                    Text("Code for 2FA Option \"**TUPlan**\"")
                        .padding(.bottom, 4)
                    
                    HStack {
                        Text(totpCodePair.current)
                            .font(.largeTitle)
                            .bold()
                        
                        Button(action: {
                            UIPasteboard.general.string = totpCodePair.current
                        }) {
                            if #available(iOS 18.0, *) {
                                Image(systemSymbol: .documentOnDocument)
                            } else {
                                Image(systemSymbol: .docOnDoc)
                            }
                        }
                        
                        Spacer()
                    }
                    .padding(.bottom, 8)
                    
                    HStack {
                        Text("Next Code: \(totpCodePair.next)")
                        
                        Button(action: {
                            UIPasteboard.general.string = totpCodePair.next
                        }) {
                            if #available(iOS 18.0, *) {
                                Image(systemSymbol: .documentOnDocument)
                            } else {
                                Image(systemSymbol: .docOnDoc)
                            }
                        }
                        .font(.caption)
                        
                        Spacer()
                    }
                    .foregroundStyle(.secondary)
                    
                    Spacer()
                }
                .padding()
                .overlay(alignment: .topTrailing) {
                    Text("2FA Code")
                        .fontWeight(.semibold)
                        .foregroundStyle(.black)
                        .frame(height: 32)
                        .padding([.top, .trailing], colorScheme == .dark ? 12 : 16)
                        .padding(.trailing, 2)
                        .padding([.bottom, .leading], 10)
                        .background(colorScheme == .dark ? .white : .clear)
                        .clipShape(UnevenRoundedRectangle(cornerRadii: .init(bottomLeading: 20)))
                }
                .overlay(alignment: .bottomTrailing) {
                    Circle()
                        .trim(from: 0.0, to: CGFloat(timeRemaining)/29)
                    //.trim(from: 0.0, to: 0.8)
                        .stroke(style: StrokeStyle(lineWidth: 12, lineCap: .round))
                        .rotationEffect(Angle(degrees: 270.0))
                        .foregroundStyle(Color.accentColor)
                        .frame(width: 64, height: 64)
                        .animation(.default, value: timeRemaining)
                        .overlay {
                            Text("\(timeRemaining)")
                                .font(.title)
                                .bold()
                        }
                        .padding(6)
                        .padding()
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
            .onAppear {
                totpCodePair = getTotpCodePair()
                timeRemaining = 29 - Calendar.current.component(.second, from: .now) % 30
            }
            .onChange(of: context.date) { _ in
                timeRemaining = 29 - Calendar.current.component(.second, from: .now) % 30

                if timeRemaining == 29 {
                    totpCodePair = getTotpCodePair()
                }
            }
        }
    }
}

#Preview {
    NavigationStack {
        TotpCodeView()
            .environmentObject(IdentityService(previewMode: true))
    }
}
