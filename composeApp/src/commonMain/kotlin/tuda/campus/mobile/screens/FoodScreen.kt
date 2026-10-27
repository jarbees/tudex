package tuda.campus.mobile.screens

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.AsyncImage
import tuda.campus.mobile.CanteenExtras
import tuda.campus.mobile.CanteenMenuItemDetail
import tuda.campus.mobile.CanteenTeaserDetail
import tuda.campus.mobile.screens.food.FoodTypeIcon
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.managers.MensaManager
import tuda.campus.mobile.managers.MenuItem
import tuda.campus.mobile.managers.Teaser
import tuda.campus.mobile.managers.mensaManager.FoodIcons
import tuda.campus.mobile.screens.food.FoodImage
import tuda.campus.mobile.screens.food.RatingStarsDisplay
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.food_bio
import tudex.composeapp.generated.resources.fri
import tudex.composeapp.generated.resources.hiddenMenuItemsMsg
import tudex.composeapp.generated.resources.ic_arrow_drop_down_24px
import tudex.composeapp.generated.resources.ic_check_24px
import tudex.composeapp.generated.resources.ic_menu_24px
import tudex.composeapp.generated.resources.mon
import tudex.composeapp.generated.resources.noImageFound
import tudex.composeapp.generated.resources.selectCanteen
import tudex.composeapp.generated.resources.showAll
import tudex.composeapp.generated.resources.thu
import tudex.composeapp.generated.resources.tue
import tudex.composeapp.generated.resources.wed
import kotlin.collections.chunked
import kotlin.collections.forEachIndexed
import kotlin.time.Clock

class FoodScreenViewModel(): ViewModel() {
    var pagerState: PagerState? by mutableStateOf(null)

    suspend fun resetState() {
        pagerState?.scrollToPage(0)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FoodScreen(
    navigateTo: (NavKey) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier = Modifier,
    viewModel: FoodScreenViewModel
) {
    val teasers: List<Teaser> by MensaManager.teasers.collectAsStateWithLifecycle()
    val menuItems: List<MenuItem> by MensaManager.menuItems.collectAsStateWithLifecycle()
    val selectedCanteen: Int by MensaManager.selectedCanteen.collectAsStateWithLifecycle()
    val cardBalance by MensaManager.cardBalance.collectAsStateWithLifecycle()

    val disabledFoodTypes by MensaManager.disabledFoodTypes.collectAsStateWithLifecycle()
    val enabledAllergies by MensaManager.enabledAllergies.collectAsStateWithLifecycle()

    val pagerState = viewModel.pagerState ?: rememberPagerState(pageCount = { teasers.size }).also {
        viewModel.pagerState = it
    }

    val weekday = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.isoDayNumber - 1
    val normalWeekday = if (weekday > 4) 0 else weekday

    var selectedDay by rememberSaveable { mutableStateOf(normalWeekday) }

    val daysOfWeek = listOf(
        stringResource(Res.string.mon),
        stringResource(Res.string.tue),
        stringResource(Res.string.wed),
        stringResource(Res.string.thu),
        stringResource(Res.string.fri)
    )

    val todayMenuItems = remember(menuItems, selectedDay) {
        val instant = Clock.System.now()
        val midnightEpochSeconds = instant.toLocalDateTime(TimeZone.UTC).date.atStartOfDayIn(TimeZone.UTC).epochSeconds

        val SECONDS_PER_DAY = 86400
        val selectedMidnight = midnightEpochSeconds + (selectedDay - normalWeekday) * SECONDS_PER_DAY + (if (weekday == 5) 2 else if (weekday == 6) 1 else 0) * SECONDS_PER_DAY

        menuItems.filter { menuItem ->
            menuItem.date == selectedMidnight.toInt()
        }
    }

    var filterEnabled by rememberSaveable { mutableStateOf(true) }

    var filteredMenuitems = remember(todayMenuItems, filterEnabled) {
        if (filterEnabled) {
            todayMenuItems.filter { menuItem ->
                if (disabledFoodTypes.any { it.displayName.contains(menuItem.dish.type.displayName) }) {
                    return@filter false
                }

                if (menuItem.dish.allergics.contains("A")) {
                    val wheats: List<String> = menuItem.dish.specificAllergics?.filter { it.startsWith("A") } ?: emptyList()
                    if (wheats.isEmpty() && enabledAllergies.any { it.startsWith("A") }) {
                        return@filter false
                    }
                }
                if (menuItem.dish.allergics.contains("H")) {
                    val nuts: List<String> = menuItem.dish.specificAllergics?.filter { it.startsWith("H") } ?: emptyList()
                    if (nuts.isEmpty() && enabledAllergies.any { it.startsWith("H") }) {
                        return@filter false
                    }
                }

                return@filter menuItem.dish.allergics.none { enabledAllergies.contains(it) } &&
                    menuItem.dish.specificAllergics?.none { enabledAllergies.contains(it) } ?: true
            }
        } else {
            todayMenuItems
        }
    }

    val shortCanteenName: Map<Int, String> = mapOf(
        Pair(1, "Stadtmitte"),
        Pair(2, "Lichtwiese"),
        Pair(3, "Schöfferstraße"),
        Pair(4, "Dieburg"),
        Pair(5, "Haardtring"),
        Pair(7, "Schöffers")
    )

    // I am using these two states, which might seem convoluted because otherwise in the expanded state
    // of MediumNavTitle the Dropdown would be shown for both collapsed and expanded Titles simultaneously
    var canteenSelectorExpandedFirst by remember { mutableStateOf(false) }
    var canteenSelectorExpandedSecond by remember { mutableStateOf(false) }

    MediumNavTitle(
        titleComp = { expanded ->
            Row(
                horizontalArrangement = Arrangement.spacedBy((-2).dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        if (expanded) {
                            canteenSelectorExpandedFirst = !canteenSelectorExpandedFirst
                        } else {
                            canteenSelectorExpandedSecond = !canteenSelectorExpandedSecond
                        }
                    }
            ) {
                Text(
                    text = shortCanteenName[selectedCanteen] ?: stringResource(Res.string.selectCanteen),
                    style = MaterialTheme.typography.headlineSmall
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_drop_down_24px),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
            }

            val canteens: List<Pair<Int, String>> = listOf(
                Pair(1, "Mensa Stadtmitte"),
                Pair(2, "Mensa Lichtwiese"),
                Pair(3, "Mensa Schöfferstraße"),
                Pair(4, "Mensa Dieburg"),
                Pair(5, "Bistro Haardtring"),
                Pair(7, "Campusrestaurant Schöffers")
            )

            DropdownMenuPopup(
                expanded = if (expanded) canteenSelectorExpandedFirst else canteenSelectorExpandedSecond,
                onDismissRequest = {
                    if (expanded) {
                        canteenSelectorExpandedFirst = false
                    } else {
                        canteenSelectorExpandedSecond = false
                    }
                }
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes()
                ) {
                    canteens.forEach { canteen ->
                        DropdownMenuItem(
                            selected = selectedCanteen == canteen.first,
                            onClick = {
                                GlobalScope.launch {
                                    MensaManager.updateSelectedCanteen(canteen.first)
                                }
                                if (expanded) {
                                    canteenSelectorExpandedFirst = false
                                } else {
                                    canteenSelectorExpandedSecond = false
                                }
                            },
                            text = { Text(canteen.second) },
                            leadingIcon =
                                if (selectedCanteen == canteen.first) {
                                    {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_check_24px),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else null,
                            shapes = MenuDefaults.itemShapes(),
                        )
                    }
                }
            }
        },
        actions = {
            if (cardBalance == "0.00") {
                IconButton(
                    onClick = { navigateTo(CanteenExtras) },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_menu_24px),
                        contentDescription = null
                    )
                }
            } else {
                TextButton(
                    onClick = { navigateTo(CanteenExtras) },
                ) {
                    Text("$cardBalance€")
                }
            }
        }
    ) {
        Column(
            modifier = modifier.verticalScroll(rememberScrollState())
        ) {
            if (teasers.isNotEmpty()) {
                HorizontalPager(pagerState) { pageOffset ->
                    val teaser = teasers[pageOffset]

                    TeaserCard(
                        teaser = teaser,
                        //showTeaserDetail = { navigateTo(CanteenTeaserDetail(pageOffset)) },
                        showTeaserDetail = { navigateTo(CanteenTeaserDetail(viewModel)) },
                        sharedTransitionScope = sharedTransitionScope
                    )
                }
            } else {
                Spacer(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .aspectRatio(16/10f)
                )
            }

            SecondaryTabRow(
                selectedTabIndex = selectedDay,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.padding(8.dp)
            ) {
                daysOfWeek.forEachIndexed { index, day ->
                    Tab(
                        selected = selectedDay == index,
                        onClick = {
                            selectedDay = index
                            filterEnabled = true
                        }
                    ) {
                        Text(
                            text = day,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .padding(bottom = 2.dp)
                        )
                    }
                }
            }

            val listItems = filteredMenuitems.chunked(2)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                listItems.forEach { menuItemPair ->
                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                    ) {
                        menuItemPair.forEachIndexed { index, menuItem ->
                            MenuItemCard(
                                menuItem = menuItem,
                                showMenuItemDetail = { navigateTo(CanteenMenuItemDetail(menuItem.id)) },
                                sharedTransitionScope = sharedTransitionScope,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = if (index == 0) 8.dp else 0.dp)
                            )
                        }

                        if (menuItemPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            val filteredItemCount = todayMenuItems.count() - filteredMenuitems.count()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                if (filteredItemCount > 0) {
                    Text(pluralStringResource(Res.plurals.hiddenMenuItemsMsg, filteredItemCount, filteredItemCount))
                    TextButton(
                        onClick = { filterEnabled = false },
                    ) {
                        Text(stringResource(Res.string.showAll))
                    }
                }
            }
        }
    }
}

@Composable
private fun TeaserCard(
    teaser: Teaser,
    showTeaserDetail: () -> Unit,
    sharedTransitionScope: SharedTransitionScope
) {
    with(sharedTransitionScope) {
        Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .sharedElement(
                    sharedContentState = rememberSharedContentState(key = "teaserImage"),
                    animatedVisibilityScope = LocalNavAnimatedContentScope.current
                )
        ) {
            teaser.image.platformImage?.bytes?.decodeToImageBitmap()?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .fillMaxWidth()
                        .clickable { showTeaserDetail() }
                )
            } ?: run {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .aspectRatio(16/10f)
                ) {
                    Text(stringResource(Res.string.noImageFound))
                }
            }

            Text(
                text = teaser.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
private fun MenuItemCard(
    menuItem: MenuItem,
    showMenuItemDetail: (MenuItem) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    modifier: Modifier
) {
    val mensaVital by FoodIcons.mensaVital.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceBright)
            .clickable { showMenuItemDetail(menuItem) }
            .padding(4.dp)
    ) {
        with(sharedTransitionScope) {
            Box(
                contentAlignment = Alignment.TopStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 4.dp)
                ) {
                    FoodImage(
                        image = menuItem.dish.image,
                        modifier = Modifier
//                            .sharedElement(
//                                sharedContentState = rememberSharedContentState(key = "menuItemImage ${menuItem.id}"),
//                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
//                            )
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
//                        .sharedElement(
//                            sharedContentState = rememberSharedContentState(
//                                key = "menuItemPrice ${menuItem.id}"
//                            ),
//                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
//                        )
                        .clip(RoundedCornerShape(bottomEnd = 16.dp))
                        .background(MaterialTheme.colorScheme.surfaceBright)
                        .padding(
                            start = 4.dp,
                            end = 6.dp,
                            top = 2.dp,
                            bottom = 4.dp
                        )
                ) {
                    FoodTypeIcon(
                        type = menuItem.dish.type,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = " ${menuItem.dish.price}0€",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.matchParentSize()) {
                    Spacer(modifier = Modifier.weight(1f))

                    Row(
                        modifier = Modifier
//                            .sharedElement(
//                                sharedContentState = rememberSharedContentState(key = "menuItemLabels ${menuItem.id}"),
//                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
//                            )
                            .height(36.dp)
                            .padding(4.dp)
                            .padding(bottom = 4.dp)
                    ) {
                        if (menuItem.dish.bio) {
                            Image(
                                painter = painterResource(Res.drawable.food_bio),
                                contentDescription = null
                            )
                        }
                        if (menuItem.dish.mensa_vital) {
                            mensaVital?.let { mensaVital ->
                                AsyncImage(
                                    model = mensaVital.encodeToByteArray(),
                                    contentDescription = null
                                )
                            } ?: run {
                                Text("MensaVital")
                            }
                        }
                    }
                }
            }
        }

        RatingStarsDisplay(
            rating = menuItem.dish.rating,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            starModifier = Modifier
                .weight(1f)
                .aspectRatio(1 / 1f)
        )

        Text(
            text = menuItem.dish.name,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp)
        )
    }
}