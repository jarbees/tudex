package tuda.campus.mobile.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.comingInFutureUpdate
import tudex.composeapp.generated.resources.ic_book_2_fill_48px

@Preview
@Composable
fun BookScreen(modifier: Modifier = Modifier) {
    val foregroundColor = MaterialTheme.colorScheme.outline

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()      // small m
    ) {

        Spacer(Modifier.weight(1f))

        Icon(
            painter = painterResource(Res.drawable.ic_book_2_fill_48px),
            contentDescription = null,
            tint = foregroundColor,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(80.dp)
        )

        Text(stringResource(Res.string.comingInFutureUpdate),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.displaySmall,
            color = foregroundColor
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}