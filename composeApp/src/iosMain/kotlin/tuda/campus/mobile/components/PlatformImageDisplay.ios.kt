package tuda.campus.mobile.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.UIKitView
import tuda.campus.mobile.PlatformImage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.UIKit.UIImageView
import platform.UIKit.UIViewContentMode

/*
private fun ContentScale.toUIViewContentMode(): UIViewContentMode =
    when (this) {
        ContentScale.Crop -> UIViewContentModeScaleAspectFill
        ContentScale.FillBounds -> UIViewContentModeScaleToFill
        else -> UIViewContentModeScaleAspectFit
    }
*/

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun PlatformImageDisplay(
    platformImage: PlatformImage,
    contentScale: ContentScale,
    modifier: Modifier
) {
    val aspectRatio = remember(platformImage.uiImage) {
        val cgImage = platformImage.uiImage.CGImage ?: return@remember 1f

        val width = CGImageGetWidth(cgImage).toFloat()
        val height = CGImageGetHeight(cgImage).toFloat()

        return@remember if (height != 0f) width / height else 1f
    }

    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
    ) {
        UIKitView(
            factory = {
                val uiContentMode = when (contentScale) {
                    ContentScale.Fit -> UIViewContentMode.UIViewContentModeScaleAspectFit
                    ContentScale.Crop -> UIViewContentMode.UIViewContentModeScaleAspectFill
                    else -> TODO()
                }

                UIImageView().apply {
                    contentMode = uiContentMode
                    clipsToBounds = true
                    image = platformImage.uiImage
                }
            },
            update = { imageView ->
                imageView.image = platformImage.uiImage
            },
            modifier = Modifier
        )
    }
}