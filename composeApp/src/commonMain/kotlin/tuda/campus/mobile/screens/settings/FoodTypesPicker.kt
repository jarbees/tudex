package tuda.campus.mobile.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.managers.FoodType
import tuda.campus.mobile.managers.MensaManager
import tuda.campus.mobile.screens.food.FoodTypeIcon
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.beef
import tudex.composeapp.generated.resources.fishAndProducts
import tudex.composeapp.generated.resources.foodTypes
import tudex.composeapp.generated.resources.pork
import tudex.composeapp.generated.resources.poultry
import tudex.composeapp.generated.resources.vegan
import tudex.composeapp.generated.resources.vegetarian
import tudex.composeapp.generated.resources.venison
import tudex.composeapp.generated.resources.whatDoYouLikeToEatQ

@Composable
fun FoodTypesPicker(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val disabledFoodTypes by MensaManager.disabledFoodTypes.collectAsStateWithLifecycle()

    MediumNavTitle(
        title = stringResource(Res.string.foodTypes),
        backAction = navigateBack
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            HeaderText(
                text = stringResource(Res.string.whatDoYouLikeToEatQ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val foodTypes: List<Pair<FoodType, String>> = listOf(
                Pair(FoodType.VEGAN, stringResource(Res.string.vegan)),
                Pair(FoodType.MEATLESS, stringResource(Res.string.vegetarian)),
                Pair(FoodType.FISH, stringResource(Res.string.fishAndProducts)),
                Pair(FoodType.PORK, stringResource(Res.string.pork)),
                Pair(FoodType.BEEF, stringResource(Res.string.beef)),
                Pair(FoodType.POULTRY, stringResource(Res.string.poultry)),
                Pair(FoodType.VENISON,stringResource(Res.string.venison))
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                foodTypes.forEach { foodType ->
                    ListItem(
                        headlineContent = { Text(foodType.second) },
                        leadingContent = {
                            FoodTypeIcon(
                                type = foodType.first,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = !disabledFoodTypes.contains(foodType.first),
                                onCheckedChange = { isChecked ->
                                    if (!isChecked && !disabledFoodTypes.contains(foodType.first)) {
                                        MensaManager.updateDisabledFoodTypes(disabledFoodTypes + foodType.first)
                                    } else if (isChecked) {
                                        MensaManager.updateDisabledFoodTypes(disabledFoodTypes.filter { it != foodType.first })
                                    }
                                }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }
    }
}