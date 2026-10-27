//  Created on 24.08.25.

import SwiftUI

struct RatingStarsView: View {
    @Binding var rating: Double
    var isEditable: Bool = false
    
    var body: some View {
        let trueRating = Int(rating * 2)
        
        //GeometryReader { geo in
            HStack {
                ForEach(0..<5) { item in
                    Image(systemName: (trueRating - (item * 2)) >= 2 ? "star.fill" : (trueRating - (item * 2)) == 1 ? "star.leadinghalf.filled" : "star")
                        .padding(.trailing, -4)
                        .onTapGesture {
                            if isEditable {
                                rating = Double(item + 1)
                            }
                        }
                }
            }
            /*gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if isEditable {
                            let locationX = value.location.x
                            let totalWidth = geo.size.width
                            
                            // Clamp location
                            /*let clampedX = max(0, min(locationX, totalWidth))
                            let percent = clampedX / totalWidth*/
                            /*let newRating = (percent * 5).rounded(.toNearestOrAwayFromZero) * 0.05
                            
                            rating = newRating*/
                        }
                    }
            )*/
        //}
    }
}
