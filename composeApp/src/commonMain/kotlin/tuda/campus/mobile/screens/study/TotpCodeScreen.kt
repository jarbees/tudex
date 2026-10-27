package tuda.campus.mobile.screens.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tuda.campus.mobile.components.LoginLockedScreen
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.managers.IdentityManager
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources._2faCode
import tudex.composeapp.generated.resources.codeFor2faOption
import tudex.composeapp.generated.resources.error
import tudex.composeapp.generated.resources.ic_content_copy_24px
import tudex.composeapp.generated.resources.nextCode
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Composable
fun TotpCodeScreen(
    navigateBack: () -> Unit
) {
    val tuTotpSecretExists by IdentityManager.tuTotpSecretExists.collectAsStateWithLifecycle()

    MediumNavTitle(
        stringResource(Res.string._2faCode),
        backAction = navigateBack
    ) {
        if (!tuTotpSecretExists) {
            LoginLockedScreen(modifier = Modifier.padding(16.dp))
        } else {
            val errorStr = stringResource(Res.string.error)

            Column {
                TotpCodeCard(
                    getTotpCodePair = {
                        IdentityManager.getTotpCodePair() ?: Pair(errorStr,errorStr)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Preview
@Composable
private fun TotpCardPreview() {
    TotpCodeCard(
        getTotpCodePair = { Pair("Preview", "123456") },
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .fillMaxWidth()
    )
}

@OptIn(ExperimentalTime::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TotpCodeCard(
    getTotpCodePair: () -> Pair<String, String>,
    modifier: Modifier = Modifier
) {
    fun getRemainingTotpTime(): Int {
        val timeSecond = Clock.System.now().epochSeconds % 30
        return 29 - timeSecond.toInt()
    }

    var totpCodePair by rememberSaveable { mutableStateOf(getTotpCodePair()) }
    var timeRemaining by rememberSaveable { mutableStateOf(getRemainingTotpTime()) }
    val animatedCountdown by animateFloatAsState(timeRemaining.toFloat() / 29)

    LaunchedEffect(Unit) {
        while(true) {
            timeRemaining = getRemainingTotpTime()

            if (timeRemaining == 29) {
                totpCodePair = getTotpCodePair()
            }

            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(20.dp)
            )
            .aspectRatio(16f/10f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                stringResource(Res.string.codeFor2faOption),
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text(
                    text = totpCodePair.first,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.SemiBold
                )

                val clipboardManager = LocalClipboardManager.current

                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            annotatedString = AnnotatedString(totpCodePair.first)
                        )
                    }
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_content_copy_24px),
                        contentDescription = null
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.nextCode, totpCodePair.second),
                    color = MaterialTheme.colorScheme.outline
                )

                val clipboardManager = LocalClipboardManager.current

                IconButton(
                    onClick = {
                        clipboardManager.setText(
                            annotatedString = AnnotatedString(totpCodePair.second)
                        )
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_content_copy_24px),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
        }

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Column(
                modifier = Modifier
                    .background(
                        color = if (isSystemInDarkTheme())
                                    MaterialTheme.colorScheme.inverseSurface
                                else
                                    MaterialTheme.colorScheme.surfaceBright,
                        shape = RoundedCornerShape(bottomStart = 20.dp, topEnd = 20.dp)
                    )
                    .padding(top = if (isSystemInDarkTheme()) 12.dp else 16.dp)
                    .padding(end = if (isSystemInDarkTheme()) 14.dp else 18.dp)
                    .padding(bottom = 0.dp, start = 10.dp)
                    .height(36.dp)
            ) {
                Text(
                    stringResource(Res.string._2faCode),
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSystemInDarkTheme())
                                MaterialTheme.colorScheme.inverseOnSurface
                            else
                                MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Row {
            Spacer(modifier = Modifier.weight(1f))
            Column {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularWavyProgressIndicator(
                        progress = { animatedCountdown },
                        stroke = Stroke(width = 20f),
                        trackStroke = Stroke(width = 20f),
                        modifier = Modifier.size(64.dp)
                    )

                    Text(
                        "$timeRemaining",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

