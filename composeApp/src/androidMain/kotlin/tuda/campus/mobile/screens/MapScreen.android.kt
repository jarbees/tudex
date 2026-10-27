package tuda.campus.mobile.screens

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import tuda.campus.mobile.MapAnnotationData
import tuda.campus.mobile.mapAnnotations
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.ic_distance_24px
import tudex.composeapp.generated.resources.ic_location_on_fill_24px
import tudex.composeapp.generated.resources.ic_search_24px
import tudex.composeapp.generated.resources.search
import androidx.core.net.toUri
import org.jetbrains.compose.resources.stringResource

class AndroidMapLauncher(private val context: Context) : MapLauncher {
    override fun launchMap(mapAnnotation: MapAnnotationData) {
        val title = mapAnnotation.annotationTitle
        val latitude = mapAnnotation.latitude
        val longitude = mapAnnotation.longitude

        val geoUri = "geo:$latitude,$longitude?q=$latitude,$latitude(${title.replace(" ", "+")})"
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri.toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        }
    }
}

@Composable
actual fun mapLauncher(): MapLauncher {
    val context = LocalContext.current
    return remember(context) { AndroidMapLauncher(context) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
actual fun MapContent(modifier: Modifier) {

    /*
    val regions = listOf(
        Pair("Stadtmitte", CameraPosition.fromLatLngZoom(LatLng(49.87615, 8.6575), 16f)),
        Pair("Lichtwiese", CameraPosition.fromLatLngZoom(LatLng(49.861, 8.6805), 15.5f)),
        Pair("Botanischer Garten", CameraPosition.fromLatLngZoom(LatLng(49.8692, 8.68105), 17f)),
        Pair("Hochschulstadion", CameraPosition.fromLatLngZoom(LatLng(49.8602, 8.6713), 16.25f)),
        Pair("Windkanal", CameraPosition.fromLatLngZoom(LatLng(49.8582, 8.5923), 16f)),
    )
     */

    val initialPosition = LatLng(49.87615, 8.6575)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 16f)
    }

    var selectedMarker by remember { mutableStateOf<MapAnnotationData?>(null) }
    val sheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false
        ),
    )
    val scope = rememberCoroutineScope()

    var fabExpanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    val textFieldState = rememberTextFieldState()

    val filteredAnnotations = mapAnnotations.filter { annotation ->
        if (textFieldState.text.isEmpty()) {
            return@filter true
        } else {
            val altTitle = annotation.annotationTitle.replace("|", "")

            return@filter annotation.annotationTitle.contains(textFieldState.text, ignoreCase = true) ||
                    altTitle.contains(textFieldState.text, ignoreCase = true) ||
                    annotation.buildingName.contains(textFieldState.text, ignoreCase = true)
        }
    }


    LaunchedEffect(sheetState.bottomSheetState.targetValue) {
        fabExpanded = false

        if (sheetState.bottomSheetState.targetValue == SheetValue.PartiallyExpanded) {
            searchExpanded = false
        } else if (sheetState.bottomSheetState.targetValue == SheetValue.Hidden) {
            textFieldState.setTextAndPlaceCursorAtEnd("")
            selectedMarker = null
        }
    }

    BottomSheetScaffold(
        scaffoldState = sheetState,
        sheetPeekHeight = 120.dp,
        sheetContent = {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SearchBar(
                        inputField = {
                            SearchBarDefaults.InputField(
                                state = textFieldState,
                                onSearch = {
                                    searchExpanded = false
                                },
                                expanded = searchExpanded,
                                onExpandedChange = { isExpanded ->
                                    searchExpanded = isExpanded
                                    if (isExpanded) {
                                        scope.launch {
                                            sheetState.bottomSheetState.expand()
                                        }
                                    }
                                },
                                placeholder = { Text(stringResource(Res.string.search)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_search_24px),
                                        contentDescription = null
                                    )
                                },
                            )
                        },
                        expanded = searchExpanded,
                        onExpandedChange = { searchExpanded = it },
                        windowInsets = WindowInsets(),
                        colors = SearchBarDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color =
                                        if (isSystemInDarkTheme())
                                            MaterialTheme.colorScheme.surfaceContainerLow
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .fillMaxSize()
                        )
                    }
                }

                Column {
                    Spacer(modifier = Modifier.height(74.dp))

                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.padding(top = 16.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            filteredAnnotations.forEach { annotation ->
                                ListItem(
                                    headlineContent = { Text(annotation.annotationTitle) },
                                    supportingContent = { Text(annotation.buildingName) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    ),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            scope.launch {
                                                sheetState.bottomSheetState.partialExpand()
                                            }
                                            selectedMarker = annotation
                                        }
                                )
                            }
                        }
                    }
                }
            }
        },
        sheetContainerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            AnnotatedMap(
                cameraPositionState = cameraPositionState,
                filteredAnnotations = filteredAnnotations,
                selectedMarker = selectedMarker,
                updateSelectedMarker = { selectedMarker = it }
            )

            MapOverlay(
                bottomSheetState = sheetState.bottomSheetState,
                cameraPositionState = cameraPositionState,
                fabExpanded = fabExpanded,
                updateFabExpanded = { fabExpanded = it }
            )
        }
    }
}

@Composable
actual fun AnnotatedMap(
    selectedMarker: MapAnnotationData,
    modifier: Modifier
) {
    val initialPosition = LatLng(selectedMarker.latitude, selectedMarker.longitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 17f)
    }

    AnnotatedMap(
        cameraPositionState = cameraPositionState,
        filteredAnnotations = mapAnnotations,
        selectedMarker = selectedMarker,
        updateSelectedMarker = {},
        modifier = modifier
    )
}

@Composable
private fun AnnotatedMap(
    cameraPositionState: CameraPositionState,
    filteredAnnotations: List<MapAnnotationData>,
    selectedMarker: MapAnnotationData?,
    updateSelectedMarker: (MapAnnotationData?) -> Unit,
    modifier: Modifier = Modifier
) {

    val mapStyle ="""[{
                        "featureType": "poi",
                        "stylers": [
                          { "visibility": "off" }
                        ]
                     }]"""


    val scope = rememberCoroutineScope()
    //var selectedMarker by remember { mutableStateOf<MapAnnotationData?>(null) }

    // This is needed so that GoogleMap recomposes on change
    val darkTheme = isSystemInDarkTheme()
    val mapColors = remember(isSystemInDarkTheme()) {
        if (darkTheme) ComposeMapColorScheme.DARK else ComposeMapColorScheme.LIGHT
    }

    var firstLaunch by remember { mutableStateOf(true) }
    LaunchedEffect(selectedMarker) {
        if (!firstLaunch) {
            selectedMarker?.let { annotation ->
                scope.launch {
                    val latLng = LatLng(annotation.latitude, annotation.longitude)

                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newCameraPosition(
                            CameraPosition.fromLatLngZoom(latLng, 17f)
                        ),
                        durationMs = 300,
                    )
                }
            }
        } else {
            firstLaunch = false
        }
    }

    GoogleMap(
        cameraPositionState = cameraPositionState,
        properties = MapProperties(mapStyleOptions = MapStyleOptions(mapStyle)),
        uiSettings = MapUiSettings(
            mapToolbarEnabled = false,
            zoomControlsEnabled = false
        ),
        onMapClick = { updateSelectedMarker(null) },
        mapColorScheme = mapColors,
        modifier = modifier
    ) {
        mapAnnotations.forEach { mapAnnotation ->
            mapAnnotation.annotationTitle.let { title ->
                CustomMarker(
                    title = title,
                    latitude = mapAnnotation.latitude,
                    longitude = mapAnnotation.longitude,
                    isHighlighted = filteredAnnotations.any { it.annotationTitle == title },
                    isSelected = selectedMarker == mapAnnotation,
                    onClick = { _ ->
                        updateSelectedMarker(mapAnnotation)
                        return@CustomMarker false
                    }
                )
            }
        }
    }
}

@GoogleMapComposable
@Composable
private fun CustomMarker(
    title: String,
    latitude: Double,
    longitude: Double,
    isHighlighted: Boolean,
    isSelected: Boolean,
    onClick: (Marker) -> Boolean
) {
    val position = LatLng(latitude, longitude)
    val markerState = rememberUpdatedMarkerState(position = position)
    val markerSize = animateDpAsState(if (isSelected) 60.dp else 40.dp).value

    MarkerComposable(
        state = markerState,
        anchor = Offset(0.5f, animateFloatAsState(if (isSelected) 0.68f else 0.6f).value),
        onClick = onClick,
        keys = arrayOf<Any>(title, markerSize, isSystemInDarkTheme(), isHighlighted)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-4).dp),
            modifier = Modifier.alpha(if (isHighlighted) 1f else 0.5f)
        ) {
            val painter = painterResource(Res.drawable.ic_location_on_fill_24px)

            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
                )
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.size(markerSize + 4.dp)
                )
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = if (isHighlighted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(markerSize)
                )
            }


            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    fontWeight = FontWeight.Bold,
                    style = TextStyle(
                        drawStyle = Stroke(
                            width = 10f,
                            miter = 10f,
                            join = StrokeJoin.Round
                        )
                    )
                )

                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MapOverlay(
    bottomSheetState: SheetState,
    cameraPositionState: CameraPositionState,
    fabExpanded: Boolean,
    updateFabExpanded: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    val regions = listOf(
        Pair("Stadtmitte", CameraPosition.fromLatLngZoom(LatLng(49.87615, 8.6575), 16f)),
        Pair("Lichtwiese", CameraPosition.fromLatLngZoom(LatLng(49.861, 8.6805), 15.5f)),
        Pair("Botanischer Garten", CameraPosition.fromLatLngZoom(LatLng(49.8692, 8.68105), 17f)),
        Pair("Hochschulstadion", CameraPosition.fromLatLngZoom(LatLng(49.8602, 8.6713), 16.25f)),
        Pair("Windkanal", CameraPosition.fromLatLngZoom(LatLng(49.8582, 8.5923), 16f)),
    )

    Column(
        horizontalAlignment = Alignment.End
    ) {
        FloatingActionButtonMenu(
            expanded = fabExpanded,
            button = {
                SmallFloatingActionButton(
                    onClick = { updateFabExpanded(!fabExpanded) },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier
                        .padding(bottom = animateDpAsState(targetValue =
                            if (bottomSheetState.targetValue == SheetValue.PartiallyExpanded) 64.dp else 0.dp
                        ).value)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_distance_24px),
                        contentDescription = null
                    )
                }
            },
        ) {
            regions.forEach { region ->
                FloatingActionButtonMenuItem(
                    onClick = {
                        updateFabExpanded(false)
                        scope.launch {
                            cameraPositionState.animate(
                                update = CameraUpdateFactory.newCameraPosition(region.second),
                                durationMs = 300,
                            )
                        }
                    },
                    text = { Text(region.first) },
                    icon = {},
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }


        SmallFloatingActionButton(
            onClick = { scope.launch {
                bottomSheetState.partialExpand()
            } },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_search_24px),
                contentDescription = null
            )
        }
    }
}
