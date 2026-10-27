package tuda.campus.mobile.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import tuda.campus.mobile.PlatformImage

@Composable
expect fun PlatformImageDisplay(
    platformImage: PlatformImage,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier
)