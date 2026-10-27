package tuda.campus.mobile.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import tuda.campus.mobile.LocalNativeViewFactory
import tuda.campus.mobile.MapAnnotationData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.MKMapItem
import platform.MapKit.MKPlacemark
import platform.MapKit.MKPointOfInterestCategoryUniversity

@Composable
actual fun MapContent(modifier: Modifier) {
    val factory = LocalNativeViewFactory.current
    UIKitViewController(
        modifier = modifier,
        factory = {
            factory.createGeneralMapView()
        }
    )
}

@Composable
actual fun AnnotatedMap(
    selectedMarker: MapAnnotationData,
    modifier: Modifier
) {
    val factory = LocalNativeViewFactory.current
    Box(modifier = modifier) {
        UIKitViewController(
            factory = {
                factory.createGeneralMapView()
            }
        )
    }
}

class IosMapLauncher : MapLauncher {
    @OptIn(ExperimentalForeignApi::class)
    override fun launchMap(mapAnnotation: MapAnnotationData) {
        val title = mapAnnotation.annotationTitle
        val buildingName = mapAnnotation.buildingName
        val latitude = mapAnnotation.latitude
        val longitude = mapAnnotation.longitude

        val coordinate = CLLocationCoordinate2DMake(latitude, longitude)
        val placemark = MKPlacemark(coordinate = coordinate)
        val mapItem = MKMapItem(placemark = placemark)
        mapItem.name = "$title - $buildingName"
        mapItem.pointOfInterestCategory = MKPointOfInterestCategoryUniversity

        mapItem.openInMapsWithLaunchOptions(null)
    }
}

@Composable
actual fun mapLauncher(): MapLauncher = IosMapLauncher()