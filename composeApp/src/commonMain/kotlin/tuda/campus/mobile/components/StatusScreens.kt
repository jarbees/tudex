package tuda.campus.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tuda.campus.mobile.screens.settings.TULoginScreen
import tuda.campus.mobile.screens.settings.TULoginViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.ic_lock_fill_48px
import tudex.composeapp.generated.resources.loadingItemName
import tudex.composeapp.generated.resources.loggingInTU
import tudex.composeapp.generated.resources.loginWithTU
import tudex.composeapp.generated.resources.sectionRequiresTU


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun RequestLoadingScreen(
    itemName: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()      // small m
    ) {

        Spacer(Modifier.weight(1f))

        LoadingIndicator(modifier = Modifier
            .size(80.dp)
            .padding(bottom = 12.dp)
        )

        Text(
            stringResource(Res.string.loadingItemName, itemName),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginLoadingScreen(
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()      // small m
    ) {

        Spacer(Modifier.weight(1f))

        LoadingIndicator(modifier = Modifier
            .size(80.dp)
            .padding(bottom = 12.dp)
        )

        Text(
            stringResource(Res.string.loggingInTU),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginLockedScreen(modifier: Modifier = Modifier) {
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()      // small m
    ) {
        Spacer(Modifier.weight(1f))

        Icon(
            painter = painterResource(Res.drawable.ic_lock_fill_48px),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(64.dp)
        )

        Text(
            stringResource(Res.string.sectionRequiresTU),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLargeEmphasized,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = { showSheet = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.loginWithTU),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            TULoginScreen(
                viewModel = TULoginViewModel(
                    dismissSheet = {
                        scope.launch { sheetState.hide() }
                            .invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showSheet = false
                                }
                            }
                    }
                ),
                skipBasicLogin = true
            )
        }
    }
}


@Preview
@Composable
private fun RequestLoadingScreenPreview() {
    RequestLoadingScreen(
        itemName = "TestThing",
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    )
}

@Preview
@Composable
private fun LoginLoadingScreenPreview() {
    LoginLoadingScreen(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    )
}

@Preview
@Composable
private fun LoginLockedScreenPreview() {
    LoginLockedScreen(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    )
}
