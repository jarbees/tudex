package tuda.campus.mobile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.GlobalScope
import tuda.campus.mobile.screens.settings.TULoginScreen
import tuda.campus.mobile.screens.settings.TULoginViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.cancel
import tudex.composeapp.generated.resources.ic_draft_48px
import tudex.composeapp.generated.resources.ic_lock_fill_48px
import tudex.composeapp.generated.resources.ic_sync_problem_24px
import tudex.composeapp.generated.resources.ic_sync_problem_48px
import tudex.composeapp.generated.resources.itemNotLoaded
import tudex.composeapp.generated.resources.loadingItemFailed
import tudex.composeapp.generated.resources.loadingItemName
import tudex.composeapp.generated.resources.loggingInTU
import tudex.composeapp.generated.resources.loginWithTU
import tudex.composeapp.generated.resources.noItemToDisplay
import tudex.composeapp.generated.resources.refreshFailed
import tudex.composeapp.generated.resources.retry
import tudex.composeapp.generated.resources.sectionRequiresTU

@Composable
fun ContentStatusScreen(
    loginStatus: TULoginState,
    itemStatus: RequestStatus,
    itemName: String,
    refresh: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    if (loginStatus == TULoginState.MFA_UNCONFIRMED ||
        loginStatus == TULoginState.SSO_UNCONFIRMED
    ) {
        LoginLoadingScreen()
    } else if (itemStatus == RequestStatus.LOADING) {
        Column {
            RequestLoadingScreen(
                itemName = itemName,
                modifier = modifier.weight(1f)
            )
        }
    } else if (itemStatus == RequestStatus.FAILED) {
        Column {
            FailedLoadingScreen(
                itemName = itemName,
                refresh = refresh,
                modifier = modifier.weight(1f)
            )
        }
    } else {
        Column {
            EmptyResultScreen(
                itemName = itemName,
                modifier = modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RequestLoadingScreen(
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
private fun LoginLoadingScreen(
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

@Composable
private fun FailedLoadingScreen(
    itemName: String,
    refresh: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()      // small m
    ) {

        Spacer(Modifier.weight(1f))

        Icon(
            painter = painterResource(Res.drawable.ic_sync_problem_48px),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 12.dp)
        )

        Text(
            stringResource(Res.string.loadingItemFailed, itemName),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        TextButton(
            onClick = {
                GlobalScope.launch {
                    refresh()
                }
            }
        ) {
            Text(stringResource(Res.string.retry))
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun EmptyResultScreen(
    itemName: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth()      // small m
    ) {

        Spacer(Modifier.weight(1f))

        Icon(
            painter = painterResource(Res.drawable.ic_draft_48px),
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .padding(bottom = 12.dp)
        )

        Text(
            stringResource(Res.string.noItemToDisplay, itemName),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavLoadingComponent(
    title: String,
    loginStatus: TULoginState,
    itemStatus: RequestStatus,
    itemName: String,
    isItemEmpty: Boolean,
    refresh: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAlert by rememberSaveable { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(title)
        if (!isItemEmpty) {
            if (loginStatus == TULoginState.MFA_UNCONFIRMED ||
                loginStatus == TULoginState.SSO_UNCONFIRMED ||
                itemStatus == RequestStatus.LOADING
            ) {
                LoadingIndicator(modifier = Modifier.size(32.dp))
            } else if (itemStatus == RequestStatus.FAILED) {
                IconButton(
                    onClick = { showAlert = true },
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_sync_problem_24px),
                        contentDescription = null
                    )
                }
            }
        }
    }

    if (showAlert) {
        AlertDialog(
            title = {
                Text(text = stringResource(Res.string.refreshFailed))
            },
            text = {
                Text(text = stringResource(Res.string.itemNotLoaded, itemName))
            },
            onDismissRequest = { showAlert = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlert = false
                        GlobalScope.launch {
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(),
                    content = { Text(stringResource(Res.string.retry)) }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showAlert = false },
                    colors = ButtonDefaults.textButtonColors(),
                    content = { Text(stringResource(Res.string.cancel)) }
                )
            }
        )
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

@Preview
@Composable
private fun FailedLoadingScreenPreview() {
    FailedLoadingScreen(
        itemName = "Item",
        refresh = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    )
}

@Preview
@Composable
private fun EmptyResultScreenPreview() {
    EmptyResultScreen(
        itemName = "Item",
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    )
}
