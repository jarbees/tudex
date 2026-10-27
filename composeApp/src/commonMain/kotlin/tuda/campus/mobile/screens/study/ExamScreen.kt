package tuda.campus.mobile.screens.study

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.ExamDetail
import tuda.campus.mobile.ExamRegister
import tuda.campus.mobile.components.CustomList
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.LoginLoadingScreen
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.components.RequestLoadingScreen
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.managers.TucanManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.exams
import tudex.composeapp.generated.resources.ic_fail_badge_fill_24px
import tudex.composeapp.generated.resources.ic_unknown_badge_24px
import tudex.composeapp.generated.resources.ic_verified_badge_fill_24px
import tudex.composeapp.generated.resources.register
import tudex.composeapp.generated.resources.unknown

@Preview
@Composable
private fun Preview() {
    ExamScreen(
        navigateTo = {},
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExamScreen(
    navigateTo: (NavKey) -> Unit,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val examStatus by TucanManager.examStatus.collectAsStateWithLifecycle()
    val exams by TucanManager.exams.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    val isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (tuLoginStatus == TULoginState.SSO || tuLoginStatus == TULoginState.MFA || tuLoginStatus == TULoginState.DEMO) {
                scope.launch { TucanManager.updateExams() }
            }
        },
        state = pullToRefreshState,
        indicator = {
            if (tuLoginStatus == TULoginState.SSO || tuLoginStatus == TULoginState.MFA || tuLoginStatus == TULoginState.DEMO) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    ) {
        MediumNavTitle(
            titleComp = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.exams))
                    if (exams.isNotEmpty()) {
                        if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                            tuLoginStatus == TULoginState.SSO_UNCONFIRMED ||
                            examStatus == RequestStatus.LOADING
                        ) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            },
            backAction = navigateBack,
            actions = {
                TextButton(
                    onClick = {
                        scope.launch {
                            TucanManager.checkToken()
                        }
                        navigateTo(ExamRegister)
                    },
                    content = { Text(stringResource(Res.string.register)) }
                )
            }
        ) {
            if (exams.isEmpty()) {
                if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                    tuLoginStatus == TULoginState.SSO_UNCONFIRMED
                ) {
                    LoginLoadingScreen()
                } else if (examStatus == RequestStatus.LOADING) {
                    Column {
                        RequestLoadingScreen(
                            itemName = stringResource(Res.string.exams),
                            modifier = modifier.weight(1f)
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    exams.forEach { examList ->
                        HeaderText(examList.firstOrNull()?.semester ?: stringResource(Res.string.unknown))

                        CustomList(
                            examList.map { exam ->
                                {
                                    var isLoading by rememberSaveable { mutableStateOf(false) }

                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                exam.courseName,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        supportingContent = {
                                            Text(
                                                exam.examType,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        trailingContent = {
                                            if (isLoading) {
                                                LoadingIndicator(modifier = Modifier.size(32.dp))
                                            } else {
                                                Icon(
                                                    painter = painterResource(
                                                        if (exam.grade == "5,0" || exam.grade == "nb")
                                                            Res.drawable.ic_fail_badge_fill_24px
                                                        else if (exam.grade == "")
                                                            Res.drawable.ic_unknown_badge_24px
                                                        else
                                                            Res.drawable.ic_verified_badge_fill_24px
                                                    ),
                                                    tint = if (exam.grade == "5,0" || exam.grade == "nb")
                                                    // Material400 red
                                                        Color(239, 83, 80, 255)
                                                    else if (exam.grade == "")
                                                        MaterialTheme.colorScheme.onSurface
                                                    else
                                                    // Material400 green
                                                        Color(102, 187, 106, 255),
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(end = 4.dp)
                                                )
                                            }
                                        },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceBright
                                        ),
                                        modifier = Modifier.clickable {
                                            if (exam.gradeStats == null && exam.grade != "") {
                                                isLoading = true
                                                scope.launch {
                                                    if (exam.gradeSheetArgs != null) {
                                                        TucanManager.updateExamGradeStats(exam)
                                                    }
                                                    isLoading = false
                                                    navigateTo(ExamDetail(exam))
                                                }
                                            } else {
                                                navigateTo(ExamDetail(exam))
                                            }
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.padding(bottom = 20.dp)
                        )
                    }
                }
            }
        }
    }
}