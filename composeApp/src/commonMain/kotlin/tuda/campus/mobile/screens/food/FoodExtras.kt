package tuda.campus.mobile.screens.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.CanteenLocationDetail
import tuda.campus.mobile.CanteenLocations
import tuda.campus.mobile.HtmlText
import tuda.campus.mobile.components.BigButton
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.PlatformImageDisplay
import tuda.campus.mobile.components.SmallNavTitle
import tuda.campus.mobile.components.rememberNFCHandler
import tuda.campus.mobile.managers.Location
import tuda.campus.mobile.managers.LocationType
import tuda.campus.mobile.managers.MensaManager
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.balance
import tudex.composeapp.generated.resources.beerGarden
import tudex.composeapp.generated.resources.bistros
import tudex.composeapp.generated.resources.cancel
import tudex.composeapp.generated.resources.canteens
import tudex.composeapp.generated.resources.coffeeBars
import tudex.composeapp.generated.resources.dateAtTime
import tudex.composeapp.generated.resources.holdCardNearMsg
import tudex.composeapp.generated.resources.information
import tudex.composeapp.generated.resources.lastChargeIs
import tudex.composeapp.generated.resources.locations
import tudex.composeapp.generated.resources.openingHours
import tudex.composeapp.generated.resources.overview
import tudex.composeapp.generated.resources.scanCard
import tudex.composeapp.generated.resources.waitingForCard

@Composable
@Preview
private fun Preview() {
    FoodExtras(
        backAction = {},
        navigateTo = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Composable
fun FoodExtras(
    backAction: () -> Unit,
    navigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBalance by MensaManager.cardBalance.collectAsStateWithLifecycle()
    val cardLastBalance by MensaManager.cardLastBalance.collectAsStateWithLifecycle()
    val cardLastScanDate by MensaManager.cardLastScanDate.collectAsStateWithLifecycle()
    val cardLastScanTime by MensaManager.cardLastScanTime.collectAsStateWithLifecycle()
    val infoItems by MensaManager.infoItems.collectAsStateWithLifecycle()

    val nfcHandler = rememberNFCHandler()
    val isNfcEnabled by nfcHandler.isEnabled.collectAsStateWithLifecycle()

    val filteredInfoItems = infoItems.filter {
        it.title != "Impressum" && it.title != "Datenschutz"
    }

    SmallNavTitle(
        title = stringResource(Res.string.overview),
        backAction = backAction,
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderText(
                text = stringResource(Res.string.balance),
                modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
            )
            Column(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "$cardBalance€",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(Res.string.dateAtTime, cardLastScanDate, cardLastScanTime),
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                BigButton(
                    title = stringResource(Res.string.scanCard),
                    onClick = { nfcHandler.startScan() },
                )
            }

            Text(
                text = stringResource(Res.string.lastChargeIs, cardLastBalance),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            )

            HeaderText(
                text = stringResource(Res.string.information),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.locations)) },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { navigateTo(CanteenLocations) }
                )

                filteredInfoItems.forEach { infoItem ->
                    ListItem(
                        headlineContent = { Text(infoItem.title) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { navigateTo(HtmlText(title = infoItem.title, html = infoItem.content)) }
                    )
                }
            }
        }
    }

    if (isNfcEnabled) {
        var dotCount by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            while (true) {
                if (dotCount == 3) {
                    delay(500)
                } else {
                    delay(300)
                }
                dotCount = (dotCount + 1) % 4
            }
        }

        AlertDialog(
            onDismissRequest = {},
            confirmButton = {
                TextButton(
                    onClick = { nfcHandler.stopScan() }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            title = { Text(stringResource(Res.string.waitingForCard) + ".".repeat(dotCount)) },
            text = { Text(stringResource(Res.string.holdCardNearMsg)) }
        )
    }
}

@Composable
expect fun HtmlTextView(
    navigateBack: () -> Unit,
    title: String,
    html: String
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FoodLocationsList(
    navigateBack: () -> Unit,
    navigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val locations by MensaManager.locations.collectAsStateWithLifecycle()

    SmallNavTitle(
        backAction = navigateBack
    ) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val types: List<Pair<LocationType, String>> = listOf(
                Pair(LocationType.BISTRO, stringResource(Res.string.bistros)),
                Pair(LocationType.KAFFEEBAR, stringResource(Res.string.coffeeBars)),
                Pair(LocationType.BIERGARTEN, stringResource(Res.string.beerGarden)),
                Pair(LocationType.MENSA, stringResource(Res.string.canteens))
            )

            types.forEach { (type, title) ->
                val typeLocations = locations.filter { it.type == type }
                if (!typeLocations.isEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmallEmphasized,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    typeLocations.forEach { location ->
                        LocationCell(
                            location = location,
                            navigateTo = navigateTo
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun LocationCell(
    location: Location,
    navigateTo: (NavKey) -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomStart,
        modifier = Modifier.clip(RoundedCornerShape(20.dp))
    ) {
        location.image.platformImage?.let { platformImage ->
            PlatformImageDisplay(
                platformImage = platformImage,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(150.dp)
                    .clickable { navigateTo(CanteenLocationDetail(location)) }
            )
        }

        Text(
            text = location.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
    }
}


@Composable
fun CanteenLocationDetail(
    location: Location,
    navigateBack: () -> Unit
) {
    SmallNavTitle(
        title = location.name,
        backAction = navigateBack
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            location.image.platformImage?.let { platformImage ->
                PlatformImageDisplay(
                    platformImage = platformImage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Column(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.openingHours),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = location.openingHours,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Text(
                text = location.description,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}