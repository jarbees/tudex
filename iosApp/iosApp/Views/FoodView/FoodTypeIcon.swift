//  Created on 17.10.26.

import SwiftUI
import Shared
import PocketSVG

struct FoodTypeIcon: View {
    @EnvironmentObject private var mensaService: MensaService

    let type: FoodType

    var body: some View {
        if let svg = mensaService.foodIcons[type] {
            SVGView(source: svg, tint: Color(.label))
        } else {
            ProgressView()
        }
    }
}

struct SVGView: View {
    let source: SVGGeometry
    var tint: Color? = nil
    var alignment: UnitPoint = .center

    var body: some View {
        GeometryReader { geometry in
            let viewBox = source.viewBox

            if viewBox.width > 0 && viewBox.height > 0 {
                let scale = min(
                    geometry.size.width / viewBox.width,
                    geometry.size.height / viewBox.height
                )

                let x = (geometry.size.width - viewBox.width * scale)
                    * alignment.x - viewBox.minX * scale

                let y = (geometry.size.height - viewBox.height * scale)
                    * alignment.y - viewBox.minY * scale

                let transform = CGAffineTransform(
                    translationX: x,
                    y: y
                )
                .scaledBy(x: scale, y: scale)

                ForEach(source.elements.indices, id: \.self) { index in
                    let element = source.elements[index]

                    element.path
                        .applying(transform)
                        .fill(tint ?? element.color)
                }
            }
        }
    }
}
