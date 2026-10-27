package tuda.campus.mobile.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.Courses
import tuda.campus.mobile.Exams
import tuda.campus.mobile.StudentCards
import tuda.campus.mobile.Timetable
import tuda.campus.mobile.TimetableDetail
import tuda.campus.mobile.TotpCodes
import tuda.campus.mobile.components.BigButton
import tuda.campus.mobile.components.CustomList
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.components.TULoginController
import tuda.campus.mobile.components.browserLauncher
import tuda.campus.mobile.managers.tucanManager.EventDate
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.managers.tucanManager.TimeSlot
import tuda.campus.mobile.managers.TucanManager
import tuda.campus.mobile.screens.settings.TULoginScreen
import tuda.campus.mobile.screens.settings.TULoginViewModel
import tuda.campus.mobile.screens.study.getDateFormatter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toLocalTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.*
import kotlin.time.Clock


@Preview
@Composable
private fun Preview() {
    StudyScreen(
        navigateTo = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StudyScreen(
    navigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val totpSecretExists by IdentityManager.tuTotpSecretExists.collectAsStateWithLifecycle()
    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val todayTimeSlots by TucanManager.todayTimeSlots.collectAsStateWithLifecycle()

    var showLoginSheet by remember { mutableStateOf(false) }
    val loginSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val now  = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    val timeSlots = todayTimeSlots.filter { timeSlot ->
        val time = timeSlot.time.split("- ").lastOrNull()?.toLocalTime() ?: return@filter true
        time > now
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            if (tuLoginStatus == TULoginState.SSO || tuLoginStatus == TULoginState.MFA || tuLoginStatus == TULoginState.DEMO) {
                scope.launch { TucanManager.updateTodayTimeSlots() }
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
        MediumNavTitle(stringResource(Res.string.homeTabName)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier
                    .verticalScroll(rememberScrollState())
            ) {
                if (tuLoginStatus == TULoginState.NONE) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        HeaderText(stringResource(Res.string.tucanFeatures))
                        TULoginController(
                            startLoginFlow = {},
                            startLoginFlowAtMfa = {},
                            usesSSO = true
                        ) { loginAction, _ ->
                            LoginLockedCard(
                                onClick = { loginAction() },
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                        }

                        if (totpSecretExists) {
                            HeaderText(stringResource(Res.string._2fa))
                            CommonActionComponent(
                                text = stringResource(Res.string._2faCodes),
                                iconID = Res.drawable.ic_lock_reset_24px,
                                onClick = { navigateTo(TotpCodes) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clip(RoundedCornerShape(20.dp))
                            )
                        }
                    }

                } else {
                    EventsComponent(
                        timeSlots = timeSlots,
                        navigateTo = navigateTo,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        HeaderText(stringResource(Res.string.frequentlyUsed))
                        CommonActionSurface(
                            navigateTo = navigateTo,
                            tuLoginStatus = tuLoginStatus,
                            totpSecretExists = totpSecretExists,
                        )

                        if (tuLoginStatus == TULoginState.MFA ||
                            tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                            tuLoginStatus == TULoginState.MFA_TUCAN_ERROR
                        ) {
                            HeaderText(stringResource(Res.string.identity))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                CommonActionComponent(
                                    text = stringResource(Res.string._2faCodes),
                                    iconID = Res.drawable.ic_lock_reset_24px,
                                    onClick = { navigateTo(TotpCodes) },
                                    modifier = Modifier.weight(1f),
                                    roundTopStart = true,
                                    roundBottomStart = true
                                )
                                CommonActionComponent(
                                    text = stringResource(Res.string.studentID),
                                    iconID = Res.drawable.ic_id_card_24px,
                                    onClick = { navigateTo(StudentCards) },
                                    modifier = Modifier.weight(1f),
                                    roundTopEnd = true,
                                    roundBottomEnd = true
                                )
                            }
                        }

                        HeaderText(stringResource(Res.string.tucan))
                        Box {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .padding(bottom = 16.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceBright,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    //.blur(8.dp)
                                    .padding(top = 16.dp)
                                    .padding(horizontal = 4.dp)
                                    .padding(bottom = 16.dp)
                            ) {
                                Row {
                                    GridItemComponent(
                                        title = stringResource(Res.string.personalData),
                                        iconID = Res.drawable.ic_person_24px,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GridItemComponent(
                                        title = stringResource(Res.string.documents),
                                        iconID = Res.drawable.ic_draft_24px,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GridItemComponent(
                                        title = stringResource(Res.string.electives),
                                        iconID = Res.drawable.ic_arrow_split_24px,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GridItemComponent(
                                        title = stringResource(Res.string.messages),
                                        iconID = Res.drawable.ic_notifications_unread_24px,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row {
                                    GridItemComponent(
                                        title = stringResource(Res.string.forms),
                                        iconID = Res.drawable.ic_chat_24px,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GridItemComponent(
                                        title = stringResource(Res.string.application),
                                        iconID = Res.drawable.ic_article_person_24px,
                                        modifier = Modifier.weight(1f)
                                    )

                                    GridItemComponent(
                                        title = stringResource(Res.string.help),
                                        iconID = Res.drawable.ic_help_24px,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(bottom = 16.dp)
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .rotate(-5f)
                                        .padding()
                                        .background(
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        )
                                )

                                Text(
                                    text = stringResource(Res.string.comingInFutureUpdate),
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.rotate(-5f)
                                )
                            }
                        }
                    }
                }

                val launcher = browserLauncher()

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    HeaderText(stringResource(Res.string.externalServices))
                    CustomList(
                        {
                            ExternalLinkComponent(
                                stringResource(Res.string.uniSport),
                                iconID = Res.drawable.ic_exercise_24px,
                                onClick = { launcher.openUrl("https://online-anmeldung.usz.tu-darmstadt.de/sportarten/aktueller_zeitraum/index.html") }
                            )
                        }, {
                            ExternalLinkComponent(
                                text = stringResource(Res.string.deutschlandticket),
                                iconID = Res.drawable.ic_transit_ticket_24px,
                                onClick = { launcher.openUrl("https://abo.ride-ticketing.de/app/login?partnerId=4343e8d5f524ae7d70fad06e2488d83a") }
                            )
                        }, {
                            ExternalLinkComponent(
                                stringResource(Res.string.identityManagement),
                                iconID = Res.drawable.ic_account_circle_24px,
                                onClick = { launcher.openUrl("https://idm.tu-darmstadt.de") }
                            )
                        }, {
                            ExternalLinkComponent(
                                stringResource(Res.string.tu4you),
                                iconID = Res.drawable.ic_keyboard_double_arrow_up_24px,
                                onClick = { launcher.openUrl("https://tu4you.tu-darmstadt.de/") }
                            )
                        },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        }
    }

    if (showLoginSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLoginSheet = false },
            sheetState = loginSheetState
        ) {
            TULoginScreen(
                viewModel = TULoginViewModel(
                    dismissSheet = {
                        scope.launch { loginSheetState.hide() }
                            .invokeOnCompletion {
                                if (!loginSheetState.isVisible) {
                                    showLoginSheet = false
                                }
                            }
                    }
                ),
                skipBasicLogin = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EventsComponent(
    timeSlots: List<TimeSlot>,
    navigateTo: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minItemWidth = 160.dp
    val screenWidth = LocalWindowInfo.current.containerDpSize.width - 32.dp + 4.dp

    val numberOfItemsThatFit = (screenWidth / (minItemWidth + 8.dp)).toInt()
    val itemWidth = screenWidth / numberOfItemsThatFit - 4.dp

    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val timeSlotStatus by TucanManager.timeSlotStatus.collectAsStateWithLifecycle()

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.horizontalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.width(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderText(stringResource(Res.string.news))
            Card(
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    bottomStart = 20.dp,
                    topEnd = animateDpAsState(if (timeSlots.isEmpty()) 20.dp else 4.dp).value,
                    bottomEnd = animateDpAsState(if (timeSlots.isEmpty()) 20.dp else 4.dp).value,
                ),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                modifier = Modifier
                    .height(100.dp)
                    .width(itemWidth)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = stringResource(Res.string.comingSoon),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.weight(2f))
                }
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(
                visible = timeSlots.isNotEmpty(),
                enter = slideInHorizontally(initialOffsetX = {it/2}) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = {-it/2}) + fadeOut()
            ) {
                HeaderText(text = stringResource(Res.string.upNext))
            }
            Box(
                contentAlignment = Alignment.CenterStart,
            ) {
                if (timeSlotStatus == RequestStatus.LOADING) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            //.height(100.dp)
                            .width(itemWidth)
                            .padding(8.dp)
                            .padding(top = 32.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        LoadingIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = stringResource(Res.string.loadingTodayTimeSlots),
                            textAlign = TextAlign.Center,
                            //style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                    tuLoginStatus == TULoginState.SSO_UNCONFIRMED
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            //.height(100.dp)
                            .width(itemWidth)
                            .padding(8.dp)
                            .padding(top = 32.dp)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        LoadingIndicator(modifier = Modifier.size(32.dp))
                        Text(
                            text = stringResource(Res.string.loggingInTU),
                            textAlign = TextAlign.Center,
                            //style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // This is needed for when a MFA Login fails logging in to tucan after padding SSO
                // Can happen seemingly at random and is fixed by logging into the web service
                if (tuLoginStatus == TULoginState.MFA_TUCAN_ERROR) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            //.height(100.dp)
                            .width(itemWidth)
                            .padding(8.dp)
                            .padding(top = 32.dp)
                            .clickable { showSheet = true }
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(Res.string.tucanFailClickMoreInfo),
                            textAlign = TextAlign.Center,
                            //style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // This is needed for when a SSO Token Refresh fails to give the option of retrying
                if (tuLoginStatus == TULoginState.SSO_INVALID) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            //.height(100.dp)
                            .width(itemWidth)
                            .padding(horizontal = 8.dp)
                            .padding(top = 32.dp)
                            .clickable { showSheet = true }
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = stringResource(Res.string.tucanFail),
                            textAlign = TextAlign.Center,
                            //style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row {
                            val launcher = browserLauncher()
                            TextButton(
                                onClick = {
                                    GlobalScope.launch {
                                        IdentityManager.tuRefreshLogin()
                                    }
                                },
                            ) {
                                Text(stringResource(Res.string.retry))
                            }
                            TextButton(
                                onClick = {
                                    launcher.tucanLogin()
                                },
                            ) {
                                Text(stringResource(Res.string.loginAgain))
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Row {
                    AnimatedVisibility(
                        visible = timeSlots.isNotEmpty(),
                        enter = slideInHorizontally(initialOffsetX = {it/2}) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = {-it/2}) + fadeOut()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clip(
                                RoundedCornerShape(
                                    topEnd = 20.dp,
                                    bottomEnd = 20.dp
                                )
                            )
                        ) {
                            timeSlots.forEach { timeSlot ->
                                val courseDate: EventDate? = run {
                                    val locale = if (timeSlot.day.startsWith("Montag|Dienstag|Mittwoch|Donnerstag|Freitag|Samstag|Sonntag")) "DE" else "EN"
                                    val dateFormatter = getDateFormatter(locale)!!      // Only null if locale != "DE" or "EN" which is impossible here
                                    val date = dateFormatter.format(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)

                                    val timeSplit = timeSlot.time.replace(" ", "").split("-")
                                    val startTime = timeSplit.firstOrNull() ?: return@run null
                                    val endTime = timeSplit.lastOrNull() ?: return@run null

                                    val startTimeSplit = startTime.split(":")
                                    val endTimeSplit = startTime.split(":")

                                    val startTimeHour = startTimeSplit.firstOrNull() ?: return@run null
                                    val startTimeMin = startTimeSplit.lastOrNull() ?: return@run null
                                    val startTimeMins = startTimeHour.toInt() * 60 + startTimeMin.toInt()

                                    val endTimeHour = endTimeSplit.firstOrNull() ?: return@run null
                                    val endTimeMin = endTimeSplit.lastOrNull() ?: return@run null
                                    val endTimeMins = endTimeHour.toInt() * 60 + endTimeMin.toInt()

                                    val eventDate = EventDate.CourseDate(
                                        courseName = timeSlot.courseName,
                                        date = date,
                                        startTime = startTime,
                                        endTime = endTime,
                                        startTimeMins = startTimeMins,
                                        endTimeMins = endTimeMins,
                                        room = timeSlot.room,
                                        teachers = timeSlot.instructor
                                    )

                                    return@run eventDate
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .then(
                                            courseDate?.let { courseDate ->
                                                Modifier.clickable { navigateTo(TimetableDetail(courseDate)) }
                                            } ?: run {
                                                Modifier
                                            }
                                        )
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceBright,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .height(100.dp)
                                        .width(itemWidth)
                                        .padding(8.dp)
                                ) {
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = timeSlot.courseName,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Row {
                                        Text(
                                            text = "${timeSlot.day},\n${
                                                timeSlot.time.replace(
                                                    " ",
                                                    ""
                                                )
                                            }",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(
                                            text = "${
                                                timeSlot.courseID.split("-").lastOrNull() ?: ""
                                            }\n${timeSlot.room}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.End,
                                            maxLines = 2,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))
    }

    val scope = rememberCoroutineScope()

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            TucanLoginFailSheet(dismissSheet = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            showSheet = false
                        }
                    }
            })
        }
    }
}

@Composable
private fun TucanLoginFailSheet(
    dismissSheet: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_error_48px),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )

            Text(
                stringResource(Res.string.tucanFailHeading),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Column {
                Text(stringResource(Res.string.tucanFailLine))

                Row {
                    Text("    1. ")
                    Text(stringResource(Res.string.tucanFailLine1))
                }
                Row {
                    Text("    2. ")
                    Text(stringResource(Res.string.tucanFailLine2))
                }
                Row {
                    Text("    3. ")
                    Text(stringResource(Res.string.tucanFailLine3))
                }

                Text(stringResource(Res.string.tucanFailLine4))
            }

            Spacer(modifier = Modifier.weight(1f))

            BigButton(stringResource(Res.string.retry), onClick = {
                GlobalScope.launch {
                    IdentityManager.tuRefreshLogin()
                }
                dismissSheet()
            })

            Spacer(modifier = Modifier.height(38.dp))
        }
    }
}

@Composable
private fun CommonActionSurface(
    navigateTo: (NavKey) -> Unit,
    tuLoginStatus: TULoginState,
    totpSecretExists: Boolean,
    modifier: Modifier = Modifier
) {
    val showTotp =
        (tuLoginStatus == TULoginState.SSO ||
         tuLoginStatus == TULoginState.SSO_UNCONFIRMED ||
         tuLoginStatus == TULoginState.SSO_INVALID)
         && totpSecretExists

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier.padding(bottom = 16.dp)     // small "m"
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CommonActionComponent(
                text = stringResource(Res.string.courses),
                iconID = Res.drawable.ic_book_24px,
                onClick = { navigateTo(Courses) },
                modifier = Modifier.weight(1f),
                roundTopStart = true
            )
            CommonActionComponent(
                text = stringResource(Res.string.timetable),
                iconID = Res.drawable.ic_calendar_month_24px,
                onClick = { navigateTo(Timetable) },
                modifier = Modifier.weight(1f),
                roundTopEnd = true,
                roundBottomEnd = !showTotp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            CommonActionComponent(
                text = stringResource(Res.string.exams),
                iconID = Res.drawable.ic_verified_24px,
                onClick = { navigateTo(Exams) },
                modifier = Modifier.weight(1f),
                roundBottomStart = true,
                roundBottomEnd = !showTotp
            )

            if (showTotp) {
                CommonActionComponent(
                    text = stringResource(Res.string._2faCodes),
                    iconID = Res.drawable.ic_lock_reset_24px,
                    onClick = { navigateTo(TotpCodes) },
                    modifier = Modifier.weight(1f),
                    roundBottomEnd = true
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CommonActionComponent(
    text: String,
    iconID: DrawableResource,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    roundTopStart: Boolean = false,
    roundTopEnd: Boolean = false,
    roundBottomStart: Boolean = false,
    roundBottomEnd: Boolean = false,
) {
    val shape = RoundedCornerShape(
        topStart = if (roundTopStart) 20.dp else 4.dp,
        topEnd = if (roundTopEnd) 20.dp else 4.dp,
        bottomStart = if (roundBottomStart) 20.dp else 4.dp,
        bottomEnd = if (roundBottomEnd) 20.dp else 4.dp
    )

    ListItem(
        headlineContent = { Text(text = text) },
        leadingContent = {
            Icon(
                painter = painterResource(iconID), // Example icon
                contentDescription = null
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = modifier
            .clip(shape)
            .clickable { onClick() }
    )

    /*
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceBright,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Icon(
                painter = painterResource(iconID), // Example icon
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp) // Space between icon and text
            )
            Text(text = text)
        }
    }
     */
}

@Composable
private fun GridItemComponent(
    title: String,
    iconID: DrawableResource,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Icon(
            painterResource(iconID),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                )
                //.padding(19.dp)
                .padding(16.dp)
                .size(28.dp)
        )
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ExternalLinkComponent(
    text: String,
    iconID: DrawableResource,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = {
            Icon(
                painter = painterResource(iconID),
                contentDescription = null
            )
        },
        trailingContent = {
            Icon(
                painter = painterResource(Res.drawable.ic_open_in_new_24px),
                contentDescription = null
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = Modifier.clickable { onClick() }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoginLockedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier      // small m
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
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
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.loginWithTU),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
