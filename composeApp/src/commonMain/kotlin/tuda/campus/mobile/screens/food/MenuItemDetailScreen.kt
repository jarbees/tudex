package tuda.campus.mobile.screens.food

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import coil3.compose.AsyncImage
import tuda.campus.mobile.components.BigButton
import tuda.campus.mobile.components.NoNavTitle
import tuda.campus.mobile.managers.MensaManager
import tuda.campus.mobile.managers.MenuItem
import tuda.campus.mobile.managers.mensaManager.FoodIcons
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.additives
import tudex.composeapp.generated.resources.allergens
import tudex.composeapp.generated.resources.almonds
import tudex.composeapp.generated.resources.antioxidants
import tudex.composeapp.generated.resources.barley
import tudex.composeapp.generated.resources.blackened
import tudex.composeapp.generated.resources.brazilNuts
import tudex.composeapp.generated.resources.cashewNuts
import tudex.composeapp.generated.resources.celeryAndProducts
import tudex.composeapp.generated.resources.crustaceansAndProducts
import tudex.composeapp.generated.resources.discardFeedback
import tudex.composeapp.generated.resources.discardFeedbackQ
import tudex.composeapp.generated.resources.eggsAndProducts
import tudex.composeapp.generated.resources.feedback
import tudex.composeapp.generated.resources.feedbackNeedsTastePriceMsg
import tudex.composeapp.generated.resources.fishAndProducts
import tudex.composeapp.generated.resources.flavourEnhancer
import tudex.composeapp.generated.resources.foodColoring
import tudex.composeapp.generated.resources.food_bio
import tudex.composeapp.generated.resources.glutenContainingGrains
import tudex.composeapp.generated.resources.hazelnuts
import tudex.composeapp.generated.resources.incompleteFeedback
import tudex.composeapp.generated.resources.keepEditing
import tudex.composeapp.generated.resources.lupines
import tudex.composeapp.generated.resources.macadamiaNuts
import tudex.composeapp.generated.resources.milkAndProducts
import tudex.composeapp.generated.resources.molluscs
import tudex.composeapp.generated.resources.mustardAndProducts
import tudex.composeapp.generated.resources.noAllergicsOrAdditivesListed
import tudex.composeapp.generated.resources.numberCharactersRemaining
import tudex.composeapp.generated.resources.nuts
import tudex.composeapp.generated.resources.oats
import tudex.composeapp.generated.resources.ok
import tudex.composeapp.generated.resources.peanutsAndProducts
import tudex.composeapp.generated.resources.pecanNuts
import tudex.composeapp.generated.resources.phenylalanine
import tudex.composeapp.generated.resources.phosphates
import tudex.composeapp.generated.resources.pistachioNuts
import tudex.composeapp.generated.resources.preservatives
import tudex.composeapp.generated.resources.priceFeedbackQuestion
import tudex.composeapp.generated.resources.rye
import tudex.composeapp.generated.resources.sendFeedback
import tudex.composeapp.generated.resources.sesameAndProducts
import tudex.composeapp.generated.resources.soybeans
import tudex.composeapp.generated.resources.spelt
import tudex.composeapp.generated.resources.sulfides
import tudex.composeapp.generated.resources.sulfurDioxidesAndSulfides
import tudex.composeapp.generated.resources.sweeteners
import tudex.composeapp.generated.resources.tasteFeedbackQuestion
import tudex.composeapp.generated.resources.textFeedbackMsg
import tudex.composeapp.generated.resources.unknown
import tudex.composeapp.generated.resources.unsentFeedbackMsg
import tudex.composeapp.generated.resources.walnuts
import tudex.composeapp.generated.resources.waxed
import tudex.composeapp.generated.resources.wheat
import kotlin.Pair

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MenuItemDetailScreen(
    menuItem: MenuItem,
    navigateBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope
) {
    val mensaVital by FoodIcons.mensaVital.collectAsStateWithLifecycle()

    val allergics: List<String> = run {
        menuItem.dish.specificAllergics?.let { specificAllergics ->
            return@run menuItem.dish.allergics.flatMap { string ->
                when (string) {
                    "A" -> {
                        val str = specificAllergics.filter { it.startsWith("A") }//.joinToString(", ")
                        return@flatMap str.ifEmpty { listOf("A") }
                    }

                    "H" -> {
                        val str = specificAllergics.filter { it.startsWith("H") }//.joinToString(", ")
                        return@flatMap str.ifEmpty { listOf("H") }
                    }

                    else -> {
                        return@flatMap listOf(string)
                    }
                }
            }
        } ?: run {
            return@run menuItem.dish.allergics
        }
    }

    val allergicsText: String = run {
        val additives: List<String> = menuItem.dish.additionals
        val allergicsString = allergics.joinToString(", ")
        val additivesString = additives.joinToString(", ")

        val resultString = listOf(allergicsString, additivesString)
            .filter { !it.trim().isEmpty() }
            .joinToString(separator = "), (")

        return@run "($resultString)"
    }

    val allergyMap: Map<String, String> = mapOf(
        Pair("A", stringResource(Res.string.glutenContainingGrains)),
        Pair("A1", stringResource(Res.string.wheat)),
        Pair("A2", stringResource(Res.string.spelt)),
        Pair("A3", stringResource(Res.string.rye)),
        Pair("A4", stringResource(Res.string.barley)),
        Pair("A5", stringResource(Res.string.oats)),
        Pair("B", stringResource(Res.string.crustaceansAndProducts)),
        Pair("C", stringResource(Res.string.eggsAndProducts)),
        Pair("D", stringResource(Res.string.fishAndProducts)),
        Pair("E", stringResource(Res.string.peanutsAndProducts)),
        Pair("F", stringResource(Res.string.soybeans)),
        Pair("G", stringResource(Res.string.milkAndProducts)),
        Pair("H", stringResource(Res.string.nuts)),
        Pair("H1", stringResource(Res.string.almonds)),
        Pair("H2", stringResource(Res.string.hazelnuts)),
        Pair("H3", stringResource(Res.string.walnuts)),
        Pair("H4", stringResource(Res.string.cashewNuts)),
        Pair("H5", stringResource(Res.string.pecanNuts)),
        Pair("H6", stringResource(Res.string.brazilNuts)),
        Pair("H7", stringResource(Res.string.pistachioNuts)),
        Pair("H8", stringResource(Res.string.macadamiaNuts)),
        Pair("I", stringResource(Res.string.celeryAndProducts)),
        Pair("J", stringResource(Res.string.mustardAndProducts)),
        Pair("K", stringResource(Res.string.sesameAndProducts)),
        Pair("L", stringResource(Res.string.sulfurDioxidesAndSulfides)),
        Pair("M", stringResource(Res.string.lupines)),
        Pair("N", stringResource(Res.string.molluscs))
    )

    val additivesMap: Map<String, String> = mapOf(
        Pair("1", stringResource(Res.string.foodColoring)),
        Pair("2", stringResource(Res.string.preservatives)),
        Pair("3", stringResource(Res.string.antioxidants)),
        Pair("4", stringResource(Res.string.flavourEnhancer)),
        Pair("5", stringResource(Res.string.sulfides)),
        Pair("6", stringResource(Res.string.blackened)),
        Pair("7", stringResource(Res.string.waxed)),
        Pair("8", stringResource(Res.string.phosphates)),
        Pair("9", stringResource(Res.string.sweeteners)),
        Pair("10", stringResource(Res.string.phenylalanine))
    )

    val scope = rememberCoroutineScope()

    NoNavTitle {
        Column(
            verticalArrangement = Arrangement.spacedBy((-2).dp),
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            with(sharedTransitionScope) {
                Box(modifier = Modifier.padding(top = 16.dp)) {
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        FoodImage(
                            image = menuItem.dish.image,
                            modifier = Modifier
                                .sharedElement(
                                    sharedContentState = rememberSharedContentState(key = "menuItemImage ${menuItem.id}"),
                                    animatedVisibilityScope = LocalNavAnimatedContentScope.current,
                                    renderInOverlayDuringTransition = false
                                )
                                .clip(RoundedCornerShape(20.dp))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .padding(8.dp)
                            .sharedElement(
                                sharedContentState = rememberSharedContentState(key = "menuItemPrice ${menuItem.id}"),
                                animatedVisibilityScope = LocalNavAnimatedContentScope.current
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        FoodTypeIcon(
                            type = menuItem.dish.type,
                            modifier = Modifier.size(24.dp)
                        )

                        Text(
                            text = " ${menuItem.dish.price}0€",
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Column(modifier = Modifier.matchParentSize()) {
                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            modifier = Modifier
//                                .sharedElement(
//                                    sharedContentState = rememberSharedContentState(key = "menuItemLabels ${menuItem.id}"),
//                                    animatedVisibilityScope = animatedVisibilityScope
//                                )
                                .padding(8.dp)
                                .padding(bottom = 16.dp)
                                .height(64.dp)
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

            Text(
                text = menuItem.dish.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val tooltipState = rememberTooltipState(isPersistent = true)

            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                tooltip = {
                    RichTooltip {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))

                            if (allergics.isEmpty() && menuItem.dish.additionals.isEmpty()) {
                                Text(
                                    text = stringResource(Res.string.noAllergicsOrAdditivesListed),
                                    style = MaterialTheme.typography.titleSmallEmphasized
                                )
                            }

                            if (allergics.isNotEmpty()) {
                                Text(
                                    text = stringResource(Res.string.allergens),
                                    style = MaterialTheme.typography.titleSmallEmphasized,
                                    modifier = Modifier.padding(bottom = 0.dp)
                                )
                            }

                            val textMeasurer = rememberTextMeasurer()
                            val density = LocalDensity.current
                            val style = MaterialTheme.typography.labelLargeEmphasized

                            val maxWidth = remember(allergics, menuItem.dish.additionals) {
                                val pixelAllergyWidth = allergics.maxOfOrNull { allergic ->
                                    textMeasurer.measure(
                                        text = allergic,
                                        style = style,
                                    ).size.width
                                } ?: 0
                                val pixelAdditionalWidth = menuItem.dish.additionals.maxOfOrNull { additional ->
                                    textMeasurer.measure(
                                        text = additional,
                                        style = style
                                    ).size.width
                                } ?: 0

                                return@remember with(density) {
                                    maxOf(pixelAllergyWidth, pixelAdditionalWidth).toDp()
                                }
                            }

                           allergics.forEach { allergic ->
                               Row {
                                   Text(
                                       text = allergic,
                                       style = style,
                                       modifier = Modifier.width(maxWidth)
                                   )
                                   Spacer(modifier = Modifier.padding(end = 4.dp))
                                   Text(
                                       text = allergyMap[allergic] ?: stringResource(Res.string.unknown),
                                       modifier = Modifier.weight(1f)
                                   )
                               }
                           }

                            if (allergics.isNotEmpty() && menuItem.dish.additionals.isNotEmpty()) {
                                Spacer(modifier = Modifier.padding(bottom = 16.dp))
                            }

                            if (menuItem.dish.additionals.isNotEmpty()) {
                                Text(
                                    text = stringResource(Res.string.additives),
                                    style = MaterialTheme.typography.titleSmallEmphasized,
                                    modifier = Modifier.padding(bottom = 0.dp)
                                )
                            }

                            menuItem.dish.additionals.forEach { additive ->
                                Row {
                                    Text(
                                        text = additive,
                                        style = style,
                                        modifier = Modifier.width(maxWidth)
                                    )
                                    Spacer(modifier = Modifier.padding(end = 4.dp))
                                    Text(
                                        text = additivesMap[additive] ?: stringResource(Res.string.unknown),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                       }
                    }
                },
                state = tooltipState
            ) {
                Text(
                    text = allergicsText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .clickable {
                            scope.launch {
                                tooltipState.show()
                            }
                        }
                )
            }

            Text(
                text = menuItem.issuingOffice.name,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                RatingStarsDisplay(
                    rating = menuItem.dish.rating,
                    starModifier = Modifier.size(24.dp)
                )
                Text(
                    text = " Ø ${menuItem.dish.rating} (${menuItem.dish.ratingCount})",
                )
            }

            var tasteRating by rememberSaveable { mutableStateOf(menuItem.dish.feedback?.tasteRating?.toDouble() ?: 0.0) }
            var priceRating by rememberSaveable { mutableStateOf(menuItem.dish.feedback?.priceRating?.toDouble() ?: 0.0) }
            val textFieldState = rememberTextFieldState(initialText = menuItem.dish.feedback?.message ?: "")

            var isFeedbackLoading by remember { mutableStateOf(false) }
            var showIncompleteFeedbackAlert by remember { mutableStateOf(false) }
            var showDismissFeedbackAlert by remember { mutableStateOf(false) }

            Text(
                text = stringResource(Res.string.feedback),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(stringResource(Res.string.tasteFeedbackQuestion))
            RatingStarsInteractive(
                rating = tasteRating,
                onRatingChange = { tasteRating = it },
                starModifier = Modifier.size(56.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(stringResource(Res.string.priceFeedbackQuestion))
            RatingStarsInteractive(
                rating = priceRating,
                onRatingChange = { priceRating = it },
                starModifier = Modifier.size(56.dp),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            TextField(
                state = textFieldState,
                lineLimits = TextFieldLineLimits.MultiLine(5, 5),
                placeholder = { Text(stringResource(Res.string.textFeedbackMsg), modifier = Modifier.fillMaxWidth()) },
                inputTransformation = InputTransformation.maxLength(255),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(Res.string.numberCharactersRemaining, 255 - textFieldState.text.length),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = 24.dp)
            )

            val canteenName: Map<Int, String> = mapOf(
                Pair(1, "Mensa Stadtmitte"),
                Pair(2, "Mensa Lichtwiese"),
                Pair(3, "Mensa Schöfferstraße"),
                Pair(4, "Mensa Dieburg"),
                Pair(5, "Bistro Haardtring"),
                Pair(7, "Schöffers")
            )

            val isFeedbackNotSent = (tasteRating != (menuItem.dish.feedback?.tasteRating?.toDouble() ?: 0.0) ||
                    priceRating != (menuItem.dish.feedback?.priceRating?.toDouble() ?: 0.0) ||
                    textFieldState.text.toString() != (menuItem.dish.feedback?.message ?: ""))

            BigButton(
                title = stringResource(Res.string.sendFeedback),
                enabled = isFeedbackNotSent && !isFeedbackLoading,
                onClick = {
                    if (tasteRating != 0.0 && priceRating != 0.0) {
                        scope.launch {
                            isFeedbackLoading = true
                            canteenName[menuItem.issuingOffice.canteenId]?.let { location ->
                                MensaManager.sendFeedback(
                                    tasteRating = tasteRating.toInt(),
                                    priceRating = priceRating.toInt(),
                                    message = textFieldState.text.toString(),
                                    location = location,
                                    dishId = menuItem.dish.id,
                                )
                            }
                            isFeedbackLoading = false
                        }
                    } else {
                        showIncompleteFeedbackAlert = true
                    }
                },
                trailingContent = {
                    if (isFeedbackLoading) LoadingIndicator(modifier = Modifier.size(36.dp))
                },
                modifier = Modifier.padding(bottom = 32.dp)
            )

            if (showIncompleteFeedbackAlert) {
                AlertDialog(
                    title = { Text(stringResource(Res.string.incompleteFeedback)) },
                    text = { Text(stringResource(Res.string.feedbackNeedsTastePriceMsg)) },
                    onDismissRequest = { showIncompleteFeedbackAlert = false },
                    confirmButton = {
                        TextButton(
                            onClick = { showIncompleteFeedbackAlert = false },
                        ) {
                            Text(stringResource(Res.string.ok))
                        }
                    }
                )
            }

            if (showDismissFeedbackAlert) {
                AlertDialog(
                    title = { Text(stringResource(Res.string.discardFeedbackQ)) },
                    text = { Text(stringResource(Res.string.unsentFeedbackMsg)) },
                    onDismissRequest = { showDismissFeedbackAlert = false },
                    confirmButton = {
                        TextButton(
                            onClick = { showDismissFeedbackAlert = false },
                        ) {
                            Text(stringResource(Res.string.keepEditing))
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDismissFeedbackAlert = false
                                navigateBack()
                            },
                        ) {
                            Text(stringResource(Res.string.discardFeedback))
                        }
                    }
                )
            }

            NavigationBackHandler(
                state = rememberNavigationEventState(
                    currentInfo = NavigationEventInfo.None
                ),
                isBackEnabled = isFeedbackNotSent,
                onBackCompleted = {
                    showDismissFeedbackAlert = true
                }
            )
        }
    }
}