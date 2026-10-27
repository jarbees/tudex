package tuda.campus.mobile.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun CustomList(content: List<@Composable () -> Unit>, modifier: Modifier = Modifier) {
    CustomList(*content.toTypedArray(), modifier = modifier)
}

@Composable
fun CustomList(vararg content: @Composable () -> Unit, modifier: Modifier = Modifier) {
    // Check if the views list is empty
    if (content.isEmpty()) return

    if (content.size == 1) {
        content.first().let {
            Card(
                modifier = modifier.fillMaxWidth(),     // small m, not Modifier
                shape = RoundedCornerShape(20.dp)
            ) {
                it()
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = modifier
        ) {
            // Handle the first view with rounded corners at the top
            content.first().let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    )
                ) {
                    it()
                }
            }

            // Handle the middle views with standard rounded corners
            if (content.size > 2) {
                content.drop(1).dropLast(1).forEach { view ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        view()
                    }
                }
            }

            // Handle the last view with rounded corners at the bottom
            if (content.size > 1) {
                content.last().let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(
                            topStart = 4.dp,
                            topEnd = 4.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 20.dp
                        )
                    ) {
                        it()
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCustomCardList() {
    CustomList({
        ListItem(
            headlineContent = { Text("Item 1") },
            supportingContent = { Text("This is a description.") },
            leadingContent = { Text("Test 1") },
            trailingContent = { Text("Test2 1") }
        )}, {
        ListItem(
            headlineContent = { Text("Item 2") },
            supportingContent = { Text("This is a description.") },
            leadingContent = { Text("Test 2") },
            trailingContent = { Text("Test2 2") }
        )}, {
        ListItem(
            headlineContent = { Text("Item 3") },
            supportingContent = { Text("This is a description.") },
            leadingContent = { Text("Test 3") },
            trailingContent = { Text("Test2 3") }
        )}
    )
}