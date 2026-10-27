package tuda.campus.mobile.screens.food

import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import coil3.compose.AsyncImage
import tuda.campus.mobile.managers.FoodType
import tuda.campus.mobile.managers.mensaManager.FoodIcons

@Composable
fun FoodTypeIcon(
    type: FoodType,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current
) {
    val icons by FoodIcons.icons.collectAsState()
    val svg = icons[type]

    if (svg != null) {
        AsyncImage(
            model = svg.encodeToByteArray(),
            contentDescription = null,
            modifier = modifier,
            colorFilter = ColorFilter.tint(tint)
        )
    } else {
        LoadingIndicator(modifier = modifier)
    }
}