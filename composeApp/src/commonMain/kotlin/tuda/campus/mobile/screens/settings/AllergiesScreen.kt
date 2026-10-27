package tuda.campus.mobile.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
import tuda.campus.mobile.managers.MensaManager
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.allergies
import tudex.composeapp.generated.resources.almonds
import tudex.composeapp.generated.resources.barley
import tudex.composeapp.generated.resources.brazilNuts
import tudex.composeapp.generated.resources.cashewNuts
import tudex.composeapp.generated.resources.celeryAndProducts
import tudex.composeapp.generated.resources.crustaceansAndProducts
import tudex.composeapp.generated.resources.eggsAndProducts
import tudex.composeapp.generated.resources.fishAndProducts
import tudex.composeapp.generated.resources.hazelnuts
import tudex.composeapp.generated.resources.lupines
import tudex.composeapp.generated.resources.macadamiaNuts
import tudex.composeapp.generated.resources.milkAndProducts
import tudex.composeapp.generated.resources.molluscs
import tudex.composeapp.generated.resources.mustardAndProducts
import tudex.composeapp.generated.resources.oats
import tudex.composeapp.generated.resources.peanutsAndProducts
import tudex.composeapp.generated.resources.pecanNuts
import tudex.composeapp.generated.resources.pistachioNuts
import tudex.composeapp.generated.resources.rye
import tudex.composeapp.generated.resources.sesameAndProducts
import tudex.composeapp.generated.resources.soybeans
import tudex.composeapp.generated.resources.spelt
import tudex.composeapp.generated.resources.sulfurDioxidesAndSulfides
import tudex.composeapp.generated.resources.walnuts
import tudex.composeapp.generated.resources.whatAreYouAllergicToQ
import tudex.composeapp.generated.resources.wheat

@Composable
fun AllergiesScreen(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabledAllergies by MensaManager.enabledAllergies.collectAsStateWithLifecycle()

    val allergyList = listOf(
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

    MediumNavTitle(
        title = stringResource(Res.string.allergies),
        backAction = navigateBack
    ) {
        Column(
            modifier = modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            HeaderText(
                text = stringResource(Res.string.whatAreYouAllergicToQ),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                allergyList.forEach { allergy ->
                    ListItem(
                        headlineContent = {
                            Row {
                                Text(allergy.first)
                                Text(
                                    text = " - ${allergy.second}",
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = enabledAllergies.contains(allergy.first),
                                onCheckedChange = { isChecked ->
                                    if (isChecked && !enabledAllergies.contains(allergy.first)) {
                                        MensaManager.updateEnabledAllergies(enabledAllergies + allergy.first)
                                    } else if (!isChecked) {
                                        MensaManager.updateEnabledAllergies(enabledAllergies.filter { it != allergy.first })
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