package tuda.campus.mobile.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.TextObfuscationMode.Companion.RevealLastTyped
import androidx.compose.foundation.text.input.TextObfuscationMode.Companion.Visible
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import tuda.campus.mobile.components.BigButton
import tuda.campus.mobile.generateTotpPairFromBase32
import tuda.campus.mobile.nativeTransition
import tuda.campus.mobile.screens.study.TotpCodeCard
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources._2faLoginAndBenefits
import tudex.composeapp.generated.resources._continue
import tudex.composeapp.generated.resources.accountDetailsUsageInfo
import tudex.composeapp.generated.resources.basicLoginAndBenefits
import tudex.composeapp.generated.resources.continueWith2faQ
import tudex.composeapp.generated.resources.enrollTokenMsg1
import tudex.composeapp.generated.resources.enrollTokenMsg2
import tudex.composeapp.generated.resources.enrollTokenMsg3
import tudex.composeapp.generated.resources.error
import tudex.composeapp.generated.resources.ic_info_48px
import tudex.composeapp.generated.resources.ic_key_48px
import tudex.composeapp.generated.resources.ic_lock_open_48px
import tudex.composeapp.generated.resources.ic_shield_40px
import tudex.composeapp.generated.resources.ic_shield_halffill_40px
import tudex.composeapp.generated.resources.ic_verified_user_fill_40px
import tudex.composeapp.generated.resources.ic_visibility_24px
import tudex.composeapp.generated.resources.ic_visibility_off_24px
import tudex.composeapp.generated.resources.login
import tudex.composeapp.generated.resources.loginFailed
import tudex.composeapp.generated.resources.loginWithTU
import tudex.composeapp.generated.resources.noLoginAndBenefits
import tudex.composeapp.generated.resources.ok
import tudex.composeapp.generated.resources.onlyPressThisOncePageSays
import tudex.composeapp.generated.resources.open2faManagementAndLogin
import tudex.composeapp.generated.resources.orEnterCodeManually
import tudex.composeapp.generated.resources.password
import tudex.composeapp.generated.resources.pleaseEnterThisCodeOnTheWebpage
import tudex.composeapp.generated.resources.scanQRCode
import tudex.composeapp.generated.resources.setup2faForTUPlan
import tudex.composeapp.generated.resources.totpCode
import tudex.composeapp.generated.resources.useSeparateDeviceMsg
import tudex.composeapp.generated.resources.username
import tudex.composeapp.generated.resources.youCanNowPressContinueBelow
import tudex.composeapp.generated.resources.youShouldNowSeeQRCode


@Serializable
private data object BasicCredentials: NavKey

@Serializable
private data object LoginInformation: NavKey

@Serializable
private data object MfaInstructions: NavKey

@Serializable
private data object MfaCredentials: NavKey

@Serializable
private data object MfaConfirmation: NavKey


@Composable
fun TULoginScreen(
    viewModel: TULoginViewModel,
    modifier: Modifier = Modifier,
    skipBasicLogin: Boolean = false,
) {
    val config = SavedStateConfiguration {
        serializersModule = SerializersModule {
            polymorphic(NavKey::class) {
                subclass(BasicCredentials::class, BasicCredentials.serializer())
                subclass(LoginInformation::class, LoginInformation.serializer())
                subclass(MfaInstructions::class, MfaInstructions.serializer())
                subclass(MfaCredentials::class, MfaCredentials.serializer())
                subclass(MfaConfirmation::class, MfaConfirmation.serializer())
            }
        }
    }
    
    val backStack = rememberNavBackStack(config, if (skipBasicLogin) LoginInformation else BasicCredentials)
    val coroutineScope = rememberCoroutineScope()

    val showAlert by viewModel.showAlert.collectAsStateWithLifecycle()
    val alertText by viewModel.alertText.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<BasicCredentials> {
                BasicCredentialsScreen(
                    viewModel.usernameTextFieldState,
                    viewModel.passwordTextFieldState,
                    isLoading = isLoading,
                    basicLogin = {
                        coroutineScope.launch {
                            //val status = viewModel.basicLogin(nextPage = { backStack.add(LoginInformation) })
                            viewModel.basicLogin(
                                //nextPage = { backStack.add(LoginInformation) }
                                nextPage = { backStack.add(MfaInstructions) }
                            )
                            /*
                            if (status == 200) {
                                backStack.add(LoginInformation)
                            }
                             */
                        }
                    }
                )
            }
            entry<LoginInformation> {
                LoginInformationScreen(
                    nextPage = { backStack.add(MfaInstructions) },
                    dismissAction = { viewModel.dismissSheet() }
                )
            }
            entry<MfaInstructions> {
                MfaInstructions(nextPage = { backStack.add(MfaCredentials) })
            }
            entry<MfaCredentials> {
                MfaCredentials(
                    totpTextFieldState = viewModel.totpTextFieldState,
                    nextPage = { backStack.add(MfaConfirmation) }
                )
            }
            entry<MfaConfirmation> {
                val errorStr = stringResource(Res.string.error)

                MfaConfirmation(
                    getNextTotpCode = {
                        generateTotpPairFromBase32(viewModel.totpTextFieldState.text.toString())
                            ?: Pair(errorStr, errorStr)
                    },
                    mfaLogin = {
                        coroutineScope.launch {
                            viewModel.mfaLogin()
                        }
                    },
                    isLoading = isLoading
                )
            }
        },
        transitionSpec = { nativeTransition() },
        popTransitionSpec = { nativeTransition(true) },
        predictivePopTransitionSpec = { nativeTransition(true) },
        modifier = modifier
            .fillMaxSize()
            .padding(bottom = 38.dp)
    )

    if (showAlert) {
        AlertDialog(
            title = {
                Text(text = stringResource(Res.string.loginFailed))
            },
            text = {
                Text(text = alertText)
            },
            onDismissRequest = { viewModel.updateShowAlert(false) },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.updateShowAlert(false) },
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Text(stringResource(Res.string.ok))
                }
            }
        )
    }
}

@Composable
private fun HeaderTextAndIcon(
    title: String,
    iconID: DrawableResource
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        Icon(
            painterResource(iconID),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(80.dp)
        )

        Text(
            title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadableHeaderTextAndIcon(
    title: String,
    iconID: DrawableResource,
    isLoading: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        val rotation by animateFloatAsState(
            targetValue = if (isLoading) 130f else 0f,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()
        )

        AnimatedContent(
            targetState = isLoading,
            transitionSpec = {
                scaleIn() + fadeIn() togetherWith scaleOut() + fadeOut()
            },
        ) { loading ->
            if (loading) {
                LoadingIndicator(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(
                            rotationZ = rotation
                        )
                )
            } else {
                Icon(
                    painterResource(iconID),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer(
                            rotationZ = rotation
                        ),
                )
            }
        }

        Text(
            title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineLarge
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BasicCredentialsScreen(
    usernameTextFieldState: TextFieldState,
    passwordTextFieldState: TextFieldState,
    isLoading: Boolean,
    basicLogin: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        LoadableHeaderTextAndIcon(
            title = stringResource(Res.string.loginWithTU),
            iconID = Res.drawable.ic_key_48px,
            isLoading = isLoading
        )

        var passwordVisible by remember { mutableStateOf(false) }

        TextField(
            state = usernameTextFieldState,
            enabled = !isLoading,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.username), modifier = Modifier.fillMaxWidth()) },
            modifier = Modifier.semantics { contentType = ContentType.Username }
        )

        SecureTextField(
            state = passwordTextFieldState,
            enabled = !isLoading,
            label = { Text(stringResource(Res.string.password), modifier = Modifier.fillMaxWidth()) },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible }
                ) {
                    Icon(
                        painter = painterResource(
                            if (passwordVisible) Res.drawable.ic_visibility_off_24px
                            else Res.drawable.ic_visibility_24px
                        ),
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.semantics { contentType = ContentType.Password },
            supportingText = {
                Text(
                    stringResource(Res.string.accountDetailsUsageInfo),
                    modifier = Modifier.padding(top = 12.dp)
                )
            },
            textObfuscationMode = if (passwordVisible) Visible else RevealLastTyped
        )

        Spacer(modifier = Modifier.weight(1f))

        BigButton(title = stringResource(Res.string.login), enabled =!isLoading, onClick = { basicLogin() })

        /*
        Button(
            onClick = { basicLogin() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.login),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
         */

       // Spacer(modifier = Modifier.height(38.dp))
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoginInformationScreen(
    nextPage: () -> Unit,
    dismissAction: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        HeaderTextAndIcon(stringResource(Res.string.continueWith2faQ), Res.drawable.ic_lock_open_48px)
        /*
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                painterResource(Res.drawable.ic_lock_open_48px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Text(
                stringResource(Res.string.continueWith2faQ),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displaySmall
            )
        }
         */

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(Res.drawable.ic_shield_40px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(12.dp)
                    .size(48.dp)
            )

            Text(
                stringResource(Res.string.noLoginAndBenefits),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLargeEmphasized
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(Res.drawable.ic_shield_halffill_40px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialShapes.Circle.toShape()
                    )
                    .padding(12.dp)
                    .size(48.dp)
            )

            Text(
                stringResource(Res.string.basicLoginAndBenefits),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painterResource(Res.drawable.ic_verified_user_fill_40px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = MaterialShapes.SoftBurst.toShape()
                    )
                    .padding(18.dp)
                    .size(44.dp)
            )

            Text(
                stringResource(Res.string._2faLoginAndBenefits),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        BigButton(title = stringResource(Res.string.login), onClick = { nextPage() })

        /*
        Button(
            onClick = { nextPage() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.login),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
         */

        /*
        TextButton(
            onClick = { dismissAction() },
        ) {
            Text(
                text = stringResource(Res.string.useBasicLogin),
                style = MaterialTheme.typography.titleMediumEmphasized,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
         */
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BigListItemWithNumber(
    number: Int,
    text: AnnotatedString,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "$number",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(start = 4.dp, end = 12.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialShapes.Circle.toShape()
                )
                .padding(12.dp)
                .size(36.dp)
        )

        Text(
            text = text,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLargeEmphasized
        )
    }
}

@Preview
@Composable
private fun MfaInstructionsPreview() {
    MfaInstructions(
        nextPage = {},
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun MfaInstructions(
    nextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        HeaderTextAndIcon(stringResource(Res.string.setup2faForTUPlan), Res.drawable.ic_info_48px)

        Text(
            stringResource(Res.string.useSeparateDeviceMsg),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        BigListItemWithNumber(
            number = 1,
            text = buildAnnotatedString {
                append(stringResource(Res.string.open2faManagementAndLogin))
                withLink(
                    LinkAnnotation.Url(
                        "https://login.tu-darmstadt.de/2fa",
                        //TextLinkStyles(style = SpanStyle(color = Color.Blue))
                    )
                ) {
                    append("login.tu-darmstadt.de/2fa")
                }
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )
        BigListItemWithNumber(
            number = 2,
            text = buildAnnotatedString {
                append(stringResource(Res.string.enrollTokenMsg1))
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                    append("TOTP")
                }
                append(stringResource(Res.string.enrollTokenMsg2))
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) {
                    append("TUPlan")
                }
                append(stringResource(Res.string.enrollTokenMsg3))
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )
        BigListItemWithNumber(
            number = 3,
            text = buildAnnotatedString { append(stringResource(Res.string.youShouldNowSeeQRCode)) },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Text(
            stringResource(Res.string.youCanNowPressContinueBelow),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        BigButton(title = stringResource(Res.string._continue), onClick = { nextPage() })
        /*
        Button(
            onClick = { nextPage() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string._continue),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
         */
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MfaCredentials(
    totpTextFieldState: TextFieldState,
    nextPage: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(stringResource(Res.string.scanQRCode))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            modifier = Modifier.weight(1f)
        ) { }

        Text(
            stringResource(Res.string.orEnterCodeManually),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        TextField(
            state = totpTextFieldState,
            //enabled = !isLoading,
            lineLimits = TextFieldLineLimits.SingleLine,
            label = { Text(stringResource(Res.string.totpCode), modifier = Modifier.fillMaxWidth()) },
            modifier = Modifier.padding(bottom = 16.dp)
        )

        BigButton(
            title = stringResource(Res.string._continue),
            onClick = { nextPage() },
            //onClick = { mfaLogin() },
//            trailingContent = {
//                if (isLoading) {
//                    LoadingIndicator(
//                        modifier = Modifier.size(32.dp)
//                    )
//                }
//            }
        )

        /*
        Button(
            onClick = { mfaLogin() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.login),
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

         */
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MfaConfirmation (
    getNextTotpCode: () -> Pair<String, String>,
    isLoading: Boolean,
    mfaLogin: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        TotpCodeCard(
            getTotpCodePair = { getNextTotpCode() },
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            stringResource(Res.string.pleaseEnterThisCodeOnTheWebpage),
            Modifier.padding(bottom = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            stringResource(Res.string.onlyPressThisOncePageSays),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        BigButton(
            title = stringResource(Res.string._continue),
            enabled = !isLoading,
            onClick = { mfaLogin() },
            trailingContent = {
                if (isLoading) {
                    LoadingIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        )
    }
}