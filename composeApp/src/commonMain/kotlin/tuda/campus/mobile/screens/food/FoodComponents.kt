package tuda.campus.mobile.screens.food

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import tuda.campus.mobile.components.PlatformImageDisplay
import tuda.campus.mobile.managers.Image
import org.jetbrains.compose.resources.painterResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.ic_star_24px
import tudex.composeapp.generated.resources.ic_star_48px
import tudex.composeapp.generated.resources.ic_star_fill_24px
import tudex.composeapp.generated.resources.ic_star_fill_48px
import tudex.composeapp.generated.resources.ic_star_half_24px
import tudex.composeapp.generated.resources.ic_star_half_48px
import tudex.composeapp.generated.resources.ic_unknown_food
import kotlin.math.roundToInt

@Composable
internal fun RatingStarsDisplay(
    rating: Double,
    modifier: Modifier = Modifier,
    starModifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        val trueRating = (rating * 2).roundToInt()

        for (i in 0..4) {
            Icon(
                painter = painterResource(
                    resource =
                        if ((trueRating - (i * 2)) >= 2)
                            Res.drawable.ic_star_fill_24px
                        else if ((trueRating - (i * 2)) == 1)
                            Res.drawable.ic_star_half_24px
                        else
                            Res.drawable.ic_star_24px
                ),
                contentDescription = null,
                modifier = starModifier
            )
        }
    }
}

@Composable
internal fun RatingStarsInteractive(
    rating: Double,
    onRatingChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    starModifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        val trueRating = (rating * 2).roundToInt()

        for (i in 0..4) {
            Icon(
                painter = painterResource(
                    resource =
                        if ((trueRating - (i * 2)) >= 2)
                            Res.drawable.ic_star_fill_48px
                        else if ((trueRating - (i * 2)) == 1)
                            Res.drawable.ic_star_half_48px
                        else
                            Res.drawable.ic_star_48px
                ),
                contentDescription = null,
                modifier = starModifier.clickable(
                    indication = null,
                    interactionSource = MutableInteractionSource()
                ) { onRatingChange((i+1).toDouble()) }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun FoodImage(
    image: Image?,
    modifier: Modifier
) {
    image?.platformImage?.let { platformImage ->
        PlatformImageDisplay(
            platformImage = platformImage,
            modifier = modifier
        )
    } ?: run {
        if (image?.__typename == "NoImageExists") {
            Image(
                painter = painterResource(Res.drawable.ic_unknown_food),
                contentDescription = null,
                modifier = modifier
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
            ) {
                LoadingIndicator()
            }
        }
    }
}