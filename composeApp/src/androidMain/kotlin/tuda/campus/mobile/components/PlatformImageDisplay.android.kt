package tuda.campus.mobile.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import tuda.campus.mobile.PlatformImage

@Composable
actual fun PlatformImageDisplay(
    platformImage: PlatformImage,
    contentScale: ContentScale,
    modifier: Modifier
) {
    Image(
        bitmap = platformImage.bitmap.asImageBitmap(),
        contentDescription = null,
        contentScale = contentScale,
        modifier = modifier
    )
}