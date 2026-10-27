package tuda.campus.mobile.screens.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.CourseDetail
import tuda.campus.mobile.components.ExpandableListSection
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.LoginLoadingScreen
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.components.RequestLoadingScreen
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.managers.TucanManager
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.courses

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun CourseScreen(
    backAction: () -> Unit,
    navigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val courses by TucanManager.courses.collectAsStateWithLifecycle()
    val courseStatus by TucanManager.courseStatus.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()
    val isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (tuLoginStatus == TULoginState.SSO || tuLoginStatus == TULoginState.MFA || tuLoginStatus == TULoginState.DEMO) {
                scope.launch { TucanManager.updateCourses() }
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
                    Text(stringResource(Res.string.courses))
                    if (courses.isNotEmpty()) {
                        if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                            tuLoginStatus == TULoginState.SSO_UNCONFIRMED ||
                            courseStatus == RequestStatus.LOADING
                        ) {
                            LoadingIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                }
            },
            backAction = backAction
        ) {
            if (courses.isEmpty()) {
                if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                    tuLoginStatus == TULoginState.SSO_UNCONFIRMED
                ) {
                    LoginLoadingScreen()
                } else if (courseStatus == RequestStatus.LOADING) {
                    Column {
                        RequestLoadingScreen(
                            itemName = stringResource(Res.string.courses),
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
                    courses.forEach { modules ->
                        HeaderText(
                            text = modules.firstOrNull()?.firstOrNull()?.semester ?: ""
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            modules.forEach { courses ->
                                if (courses.isNotEmpty()) {
                                    ExpandableListSection(title = courses.first().moduleName) {
                                        courses.forEach { course ->
                                            ListItem(
                                                headlineContent = { Text(course.courseName) },
                                                supportingContent = { Text(course.courseID) },
                                                leadingContent = {
                                                    Spacer(
                                                        modifier = Modifier.width(
                                                            36.dp
                                                        )
                                                    )
                                                },
                                                colors = ListItemDefaults.colors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceBright
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable { navigateTo(CourseDetail(course)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}