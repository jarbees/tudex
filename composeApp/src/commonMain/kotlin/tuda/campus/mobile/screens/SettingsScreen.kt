package tuda.campus.mobile.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.savedstate.serialization.SavedStateConfiguration
import tuda.campus.mobile.AllergiesPicker
import tuda.campus.mobile.Books
import tuda.campus.mobile.Canteen
import tuda.campus.mobile.ExamDetail
import tuda.campus.mobile.Exams
import tuda.campus.mobile.FoodTypesPicker
import tuda.campus.mobile.Map
import tuda.campus.mobile.Settings
import tuda.campus.mobile.Study
import tuda.campus.mobile.components.CustomList
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.components.TULoginController
import tuda.campus.mobile.components.getAppVersion
import tuda.campus.mobile.components.getBuildNumber
import tuda.campus.mobile.getPlatform
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.MensaManager
import tuda.campus.mobile.managers.SettingsManager
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.screens.settings.TULoginScreen
import tuda.campus.mobile.screens.settings.TULoginViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources._2faRemovalWarning
import tudex.composeapp.generated.resources.allergies
import tudex.composeapp.generated.resources.cancel
import tudex.composeapp.generated.resources.confirmLogout
import tudex.composeapp.generated.resources.defaultLocation
import tudex.composeapp.generated.resources.english
import tudex.composeapp.generated.resources.error
import tudex.composeapp.generated.resources.foodSettings
import tudex.composeapp.generated.resources.foodTypes
import tudex.composeapp.generated.resources.german
import tudex.composeapp.generated.resources.guest
import tudex.composeapp.generated.resources.ic_check_24px
import tudex.composeapp.generated.resources.keep
import tudex.composeapp.generated.resources.loginAs2fa
import tudex.composeapp.generated.resources.loginAs2faBroken
import tudex.composeapp.generated.resources.loginAsDemoAccount
import tudex.composeapp.generated.resources.loginAsSso
import tudex.composeapp.generated.resources.loginWithDemo
import tudex.composeapp.generated.resources.loginWithSso
import tudex.composeapp.generated.resources.loginWithUsernamePassword
import tudex.composeapp.generated.resources.logout
import tudex.composeapp.generated.resources.menuLanguage
import tudex.composeapp.generated.resources.remove
import tudex.composeapp.generated.resources.remove2faSecretQ
import tudex.composeapp.generated.resources.settings
import tudex.composeapp.generated.resources.status
import tudex.composeapp.generated.resources.student
import tudex.composeapp.generated.resources.switchBackNativeUI
import tudex.composeapp.generated.resources.uiType
import tudex.composeapp.generated.resources.unknown

@Preview
@Composable
private fun Preview() {
    val config = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(Study::class, Study.serializer())
                subclass(Exams::class, Exams.serializer())
                subclass(ExamDetail::class, ExamDetail.serializer())

                subclass(Canteen::class, Canteen.serializer())

                subclass(Map::class, Map.serializer())

                subclass(Books::class, Books.serializer())

                subclass(Settings::class, Settings.serializer())
            }
        }
    }

    val backStack = rememberNavBackStack(config, Study)
    SettingsScreen(
        navigateTo = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Composable
private fun ListText(
    text: String
) {
    ListItem(
        headlineContent = {
            Text(text)
        },
//        modifier = Modifier
//            .padding(bottom = 16.dp)
//            .clip(RoundedCornerShape(20.dp)),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    )
}

@Composable
private fun ListButton(
    title: String,
    action: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(title, color = MaterialTheme.colorScheme.primary)
        },
        modifier = Modifier
            //.padding(bottom = 16.dp)
            //.clip(RoundedCornerShape(20.dp))
            .clickable { action() },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    navigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val tuLoginState by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val tuUsername by IdentityManager.tuUsername.collectAsStateWithLifecycle()
    val isMfaLoginAvailable by SettingsManager.isMfaLoginAvailable.collectAsStateWithLifecycle()
    val isDemoLoginAvailable by SettingsManager.isDemoLoginAvailable.collectAsStateWithLifecycle()

    val selectedCanteen by MensaManager.selectedCanteen.collectAsStateWithLifecycle()
    val canteenStatus by MensaManager.status.collectAsStateWithLifecycle()
    val canteenLanguage by MensaManager.lang.collectAsStateWithLifecycle()
    val canteenLocation by MensaManager.defaultCanteen.collectAsStateWithLifecycle()

    var showLogoutAlert by remember { mutableStateOf(false) }
    var showTotpResetAlert by remember { mutableStateOf(false) }

    var skipBasicLogin by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val scope = rememberCoroutineScope()

    MediumNavTitle(stringResource(Res.string.settings)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            HeaderText(stringResource(Res.string.foodSettings))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
            ) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.foodTypes)) },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { navigateTo(FoodTypesPicker) },
                )

                ListItem(
                    headlineContent = { Text(stringResource(Res.string.allergies)) },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceBright
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { navigateTo(AllergiesPicker) },
                )

                val canteenSelectorList: List<Triple<Int, String, String>> = listOf(
                    Triple(0, "Last Viewed Canteen", "Last Viewed"),
                    Triple(1, "Mensa Stadtmitte", "Stadtmitte"),
                    Triple(2, "Mensa Lichtwiese", "Lichtwiese"),
                    Triple(3, "Mensa Schöfferstraße", "Schöfferstraße"),
                    Triple(4, "Mensa Dieburg", "Dieburg"),
                    Triple(5, "Bistro Haardtring", "Haardtring"),
                    Triple(7, "Campusrestaurant Schöffers", "Schöffers")
                )

                val statusSelectorList: List<Triple<String, String, String?>> = listOf(
                    Triple("STUDENT", stringResource(Res.string.student), null),
                    Triple("GUEST", stringResource(Res.string.guest), null)
                )

                val menuLangSelectorList: List<Triple<String, String, String?>> = listOf(
                    Triple("DE", stringResource(Res.string.german), null),
                    Triple("EN", stringResource(Res.string.english), null)
                )

                ListSelectorMenu(
                    title = stringResource(Res.string.status),
                    list = statusSelectorList,
                    selection = canteenStatus,
                    onSelectionChange = {
                        scope.launch {
                            MensaManager.updateStatus(it)
                        }
                    },
                )

                ListSelectorMenu(
                    title = stringResource(Res.string.menuLanguage),
                    list = menuLangSelectorList,
                    selection = canteenLanguage,
                    onSelectionChange = {
                        scope.launch {
                            MensaManager.updateLang(it)
                        }
                    },
                )

                ListSelectorMenu(
                    title = stringResource(Res.string.defaultLocation),
                    list = canteenSelectorList,
                    selection = canteenLocation,
                    onSelectionChange = {
                        scope.launch {
                            MensaManager.updateDefaultCanteen(it)
                        }
                    },
                )
            }


            HeaderText(
                text = "TU ID",
                modifier = Modifier.pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitFirstDown()

                            val job = scope.launch {
                                delay(12000L)
                                SettingsManager.updateMfaLoginAvailability(!isMfaLoginAvailable)
                            }

                            waitForUpOrCancellation()
                            job.cancel()
                        }
                    }
                }
            )

            if (tuLoginState == TULoginState.SSO) {
                CustomList({
                    ListText(stringResource(Res.string.loginAsSso, tuUsername))
                }, {
                    ListButton(stringResource(Res.string.logout)) {
                        showLogoutAlert = true
                    }
                })
            }
            else if (tuLoginState == TULoginState.MFA) {
                CustomList({
                    ListText(stringResource(Res.string.loginAs2fa, tuUsername))
                }, {
                    ListButton(stringResource(Res.string.logout)) {
                        showLogoutAlert = true
                    }
                })
            }
            else if (tuLoginState == TULoginState.DEMO) {
                CustomList({
                    ListText(stringResource(Res.string.loginAsDemoAccount))
                }, {
                    ListButton(stringResource(Res.string.logout)) {
                        showLogoutAlert = true
                    }
                })
            }
            else if (tuLoginState == TULoginState.MFA_TUCAN_ERROR) {
                CustomList({
                    ListText(stringResource(Res.string.loginAs2faBroken, tuUsername))
                }, {
                    ListButton(stringResource(Res.string.logout)) {
                        showLogoutAlert = true
                    }
                })
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    TULoginController(
                        startLoginFlow = {
                            skipBasicLogin = false
                            showSheet = true
                        },
                        startLoginFlowAtMfa = {
                            skipBasicLogin = true
                            showSheet = true
                        }
                    ) { loginAction, isLoading ->
                        ListItem(
                            headlineContent = {
                                Text(stringResource(Res.string.loginWithSso), color = MaterialTheme.colorScheme.primary)
                            },
                            trailingContent = {
                                if (isLoading) {
                                    LoadingIndicator()
                                }
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { loginAction() },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            )
                        )
                    }

                    if (isMfaLoginAvailable) {
                        TULoginController(
                            startLoginFlow = {
                                skipBasicLogin = false
                                showSheet = true
                            },
                            startLoginFlowAtMfa = {
                                skipBasicLogin = true
                                showSheet = true
                            },
                            usesSSO = false
                        ) { loginAction, isLoading ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        stringResource(Res.string.loginWithUsernamePassword),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingContent = {
                                    if (isLoading) {
                                        LoadingIndicator()
                                    }
                                },
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { loginAction() },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surfaceBright
                                )
                            )
                        }
                    }

                    if (isDemoLoginAvailable) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(Res.string.loginWithDemo),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { IdentityManager.tuDemoLogin() },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceBright
                            )
                        )
                    }
                }
            }

            // Compose can be used on iOS for debugging purposes,
            // which is why an option to switch back to SwiftUI is needed
            if (getPlatform().name.startsWith("iOS")) {
                HeaderText(stringResource(Res.string.uiType), modifier = Modifier
                    .padding(top = 16.dp))
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.switchBackNativeUI)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = { SettingsManager.switchUiType() } )
                )
            }

            var demoClickNumber by remember { mutableStateOf(0) }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "TUDex v${getAppVersion() ?: stringResource(Res.string.error)} build ${getBuildNumber() ?: stringResource(Res.string.error)}",
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(
                        interactionSource = null,
                        indication = null
                    ) {
                        if (tuLoginState == TULoginState.NONE) {
                            if (demoClickNumber < 9) {
                                demoClickNumber++
                            } else {
                                demoClickNumber = 0
                                SettingsManager.updateDemoLoginAvailability(!isDemoLoginAvailable)
                            }
                        }
                    }
                )
            }
        }
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
                skipBasicLogin = skipBasicLogin
            )
        }
    }

    if (showLogoutAlert) {
        AlertDialog(
            title = { Text(stringResource(Res.string.confirmLogout)) },
            onDismissRequest = { showLogoutAlert = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutAlert = false
                        if (IdentityManager.tuLoginStatus.value == TULoginState.MFA ||
                            IdentityManager.tuLoginStatus.value == TULoginState.MFA_UNCONFIRMED)
                        {
                            showTotpResetAlert = true
                        }
                        scope.launch { IdentityManager.tuLogout() }
                    },
                ) {
                    Text(stringResource(Res.string.logout))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showLogoutAlert = false },
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            }
        )
    }

    if (showTotpResetAlert) {
        AlertDialog(
            title = { Text(stringResource(Res.string.remove2faSecretQ)) },
            text = { Text(stringResource(Res.string._2faRemovalWarning)) },
            onDismissRequest = { showTotpResetAlert = false },
            confirmButton = {
                TextButton(
                    onClick = { showTotpResetAlert = false },
                ) {
                    Text(stringResource(Res.string.keep))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        IdentityManager.resetTotpSecret()
                        showTotpResetAlert = false
                    },
                ) {
                    Text(stringResource(Res.string.remove))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> ListSelectorMenu(
    title: String,
    list: List<Triple<T, String, String?>>,
    selection: T,
    onSelectionChange: (T) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        trailingContent = {
            var expanded by remember { mutableStateOf(false) }
            val selectedItem = list.firstOrNull { it.first == selection }

            TextButton(
                onClick = { expanded = true },
            ) {
                Text(
                    text = selectedItem?.third ?: selectedItem?.second ?: stringResource(Res.string.unknown)
                )
            }

            DropdownMenuPopup(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes()
                ) {
                    list.forEach { (code, string) ->
                        DropdownMenuItem(
                            selected = code == selection,
                            onClick = {
                                onSelectionChange(code)
                                expanded = false
                            },
                            text = { Text(string) },
                            leadingIcon =
                                if (code == selection) {
                                    {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_check_24px),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                } else null,
                            shapes = MenuDefaults.itemShapes(),
                        )
                    }
                }
            }
        },
        modifier = Modifier.clip(RoundedCornerShape(4.dp)),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        )
    )
}