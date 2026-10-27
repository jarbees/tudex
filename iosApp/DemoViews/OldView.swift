//  Created on 03.05.25.

import SwiftUI

struct OldView: View {
    var body: some View {
        NavigationView {
            ScrollView(showsIndicators: false) {
                VStack {
                    Text("Studies")
                        .font(.system(.largeTitle, weight: .bold))
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .clipped()
                        .padding(.leading)
                        .padding(.bottom, 8)
                    ScrollView(.horizontal) {
                        VStack {
                            HStack {
                                Text("News".uppercased())
                                    .padding(.leading, 28)
                                    .font(.system(.caption, weight: .medium))
                                    .padding(.top, 8)
                                    .frame(width: 197, alignment: .leading)
                                    .clipped()
                                    .foregroundStyle(.secondary)
                                Text("Up Next".uppercased())
                                    .padding(.leading, 8)
                                    .font(.system(.caption, weight: .medium))
                                    .padding(.top, 8)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .clipped()
                                    .foregroundStyle(.secondary)
                            }
                            HStack {
                                VStack {
                                    ZStack(alignment: .leading) {
                                        Text("Hello, World!")
                                            .frame(alignment: .leading)
                                            .clipped()
                                            .padding(.leading, 8)
                                            .padding(.top, 8)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .clipped()
                                    Spacer()
                                        .clipped()
                                }
                                .cornerRadius(10)
                                .foregroundStyle(Color(.secondarySystemGroupedBackground))
                                .frame(maxWidth: .infinity, alignment: .center)
                                .frame(height: 100)
                                .padding(.leading)
                                VStack {
                                    Text("Funktionale und objektorientierte Programmierkonzepte")
                                        .frame(maxHeight: .infinity, alignment: .center)
                                        .clipped()
                                        .padding(.horizontal, 8)
                                        .foregroundStyle(.primary)
                                        .font(.system(.body, weight: .bold))
                                        .padding(.top, 8)
                                        .font(.body)
                                    HStack {
                                        Text("Donnerstag,  08:00-24:00")
                                            .padding(.bottom, 8)
                                            .font(.caption)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .clipped()
                                            .padding(.leading, 8)
                                            .foregroundStyle(.secondary)
                                        Text("Übung S202/D120")
                                            .font(.caption)
                                            .padding(.trailing, 8)
                                            .padding(.bottom, 8)
                                            .foregroundStyle(.secondary)
                                            .multilineTextAlignment(.trailing)
                                    }
                                }
                                .background {
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .fill(Color(.secondarySystemGroupedBackground))
                                        .foregroundStyle(Color(.systemBackground))
                                        .frame(height: 100)
                                        .clipped()
                                }
                                .frame(maxWidth: .infinity, maxHeight: 100)
                                .clipped()
                                VStack {
                                    Text("Funktionale und objektorientierte Programmierkonzepte")
                                        .frame(maxHeight: .infinity, alignment: .center)
                                        .clipped()
                                        .padding(.horizontal, 8)
                                        .foregroundStyle(.primary)
                                        .font(.system(.body, weight: .bold))
                                        .padding(.top, 8)
                                        .font(.body)
                                    HStack {
                                        Text("Donnerstag,  08:00-24:00")
                                            .padding(.bottom, 8)
                                            .font(.caption)
                                            .frame(maxWidth: .infinity, alignment: .leading)
                                            .clipped()
                                            .padding(.leading, 8)
                                            .foregroundStyle(.secondary)
                                        Text("Übung S202/D120")
                                            .font(.caption)
                                            .padding(.trailing, 8)
                                            .padding(.bottom, 8)
                                            .foregroundStyle(.secondary)
                                            .multilineTextAlignment(.trailing)
                                    }
                                }
                                .background {
                                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                                        .fill(Color(.secondarySystemGroupedBackground))
                                        .foregroundStyle(Color(.systemBackground))
                                        .frame(height: 100)
                                        .clipped()
                                }
                                .frame(maxWidth: .infinity, maxHeight: 100)
                                .clipped()
                            }
                        }
                    }
                    Text("Frequently used".uppercased())
                        .padding(.leading, 28)
                        .font(.system(.caption, weight: .medium))
                        .padding(.top, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .clipped()
                        .foregroundStyle(.secondary)
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())]) {
                        ForEach(0..<5) { _ in // Replace with your data model here
                            HStack {
                                Image(systemName: "paperplane")
                                    .padding(.vertical, 8)
                                    .padding(.trailing, 8)
                                    .padding(.leading)
                                Text("Hello, World!")
                                    .padding(.vertical)
                                    .font(.subheadline)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .symbolRenderingMode(.monochrome)
                                    .padding(.trailing)
                            }
                            .background {
                                RoundedRectangle(cornerRadius: 10, style: .continuous)
                                    .fill(Color(.secondarySystemGroupedBackground))
                            }
                            .frame(maxWidth: .infinity, maxHeight: 50)
                            .clipped()
                        }
                    }
                    .padding(.horizontal)
                    Text("TUCaN".uppercased())
                        .padding(.leading, 28)
                        .font(.system(.caption, weight: .medium))
                        .padding(.top, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .clipped()
                        .foregroundStyle(.secondary)
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 0, alignment: .top), GridItem(.flexible(), spacing: 0, alignment: .top), GridItem(.flexible(), spacing: 0, alignment: .top), GridItem(.flexible(), spacing: 0, alignment: .top)], spacing: 16) {
                        ForEach(0..<5) { _ in // Replace with your data model here
                            VStack {
                                Image(systemName: "text.page.badge.magnifyingglass")
                                    .imageScale(.large)
                                    .symbolRenderingMode(.monochrome)
                                    .opacity(1)
                                    .padding(16)
                                    .background {
                                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                                            .fill(Color(.displayP3, red: 114/255, green: 155/255, blue: 68/255))
                                            .frame(width: 60, height: 60, alignment: .center)
                                            .clipped()
                                            .opacity(1)
                                    }
                                    .foregroundStyle(.white)
                                    .frame(width: 60, height: 60)
                                    .clipped()
                                    .font(.system(.body, weight: .medium))
                                Spacer()
                                    .frame(height: 4)
                                    .clipped()
                                Text("Hello, World!")
                                    .font(.caption)
                                    .multilineTextAlignment(.center)
                            }
                        }
                    }
                    .padding()
                    .background {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color(.secondarySystemGroupedBackground))
                            .padding(.horizontal)
                            .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
                    }
                    Text("External Services".uppercased())
                        .padding(.leading, 28)
                        .font(.system(.caption, weight: .medium))
                        .padding(.top, 8)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .clipped()
                        .foregroundStyle(.secondary)
                    LazyVGrid(columns: [GridItem(.flexible(), spacing: 0)], spacing: 0) {
                        ForEach(0..<5) { _ in // Replace with your data model here
                            HStack {
                                Image(systemName: "paperplane")
                                    .padding(.vertical, 8)
                                    .padding(.trailing, 8)
                                    .padding(.leading, 20)
                                    .frame(width: 40, alignment: .center)
                                    .clipped()
                                Text("Hello, World!")
                                    .padding(.vertical)
                                    .font(.subheadline)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .symbolRenderingMode(.monochrome)
                                    .padding(.trailing)
                            }
                            Divider()
                                .padding(.leading, 52)
                        }
                    }
                    .background {
                        RoundedRectangle(cornerRadius: 10, style: .continuous)
                            .fill(Color(.secondarySystemGroupedBackground))
                            .shadow(color: .black.opacity(0.05), radius: 8, x: 0, y: 4)
                    }
                    .padding(.horizontal)
                    Spacer()
                        .frame(height: 0)
                        .clipped()
                }
                .frame(maxWidth: .infinity)
                .clipped()
                .padding(.top, 98)
                .padding(.bottom, 100)
            }
            
            .overlay(alignment: .top) {
                // Status Bar
                HStack {
                    Text("9:41")
                        .frame(width: 109)
                        .clipped()
                        .font(.system(.body, weight: .semibold))
                    Spacer()
                    HStack(spacing: 5) {
                        Image(systemName: "cellularbars")
                            .imageScale(.small)
                        Image(systemName: "wifi")
                            .imageScale(.small)
                        Image(systemName: "battery.100")
                            .symbolRenderingMode(.hierarchical)
                            .font(.system(.body, weight: .light))
                    }
                    .frame(width: 109)
                    .clipped()
                    .font(.system(.body, weight: .semibold))
                }
                .padding(.horizontal)
                .padding(.top, 5)
                .frame(maxWidth: .infinity)
                .clipped()
                .frame(height: 53)
                .clipped()
                .background(Color(.systemGroupedBackground))
            }
            .overlay(alignment: .bottom) {
                // Tab Bar
                VStack(spacing: 0) {
                    Divider()
                    HStack(spacing: 10) {
                        ForEach(0..<5) { _ in // Replace with your data model here
                            VStack(spacing: 4) {
                                Image(systemName: "play.circle.fill")
                                    .imageScale(.large)
                                    .frame(height: 26)
                                    .clipped()
                                Text("Listen Now")
                                    .font(.caption2)
                            }
                            .frame(maxWidth: .infinity)
                            .clipped()
                            .frame(height: 45)
                            .clipped()
                            .foregroundStyle(.secondary)
                        }
                    }
                    .padding(.horizontal, 15)
                    .padding(.top, 5)
                }
                .frame(height: 84, alignment: .top)
                .clipped()
                .background {
                    Rectangle()
                        .fill(.clear)
                        .background(Material.bar)
                }
            }
            .background {
                RoundedRectangle(cornerRadius: 4, style: .continuous)
                    .fill(Color(.systemGroupedBackground))
            }
        }
    }
}

#Preview {
    OldView()
}
