package tuda.campus.mobile.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.ic_keyboard_arrow_right_24px


@Composable
fun ExpandableListSection(
    modifier: Modifier = Modifier,
    title: String,
    content: @Composable () -> Unit
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        val rotationDegrees by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f)

        ListItem(
            headlineContent = { Text(text = title) },
            trailingContent = {
                Icon(
                    painter = painterResource(Res.drawable.ic_keyboard_arrow_right_24px),
                    contentDescription = null,
                    modifier = modifier.rotate(rotationDegrees)
                )
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .clickable { isExpanded = !isExpanded }
        )

        AnimatedVisibility(
            modifier = Modifier.fillMaxWidth(),
            visible = isExpanded
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                content()
            }
        }
    }
}