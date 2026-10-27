package tuda.campus.mobile.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import tuda.campus.mobile.MapAnnotationData

@Preview
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    MapContent(modifier = modifier.fillMaxSize())
}

@Composable
expect fun MapContent(modifier: Modifier = Modifier)

@Composable
expect fun AnnotatedMap(
    selectedMarker: MapAnnotationData,
    modifier: Modifier = Modifier
)

interface MapLauncher {
    fun launchMap(mapAnnotation: MapAnnotationData)
}

@Composable
expect fun mapLauncher(): MapLauncher