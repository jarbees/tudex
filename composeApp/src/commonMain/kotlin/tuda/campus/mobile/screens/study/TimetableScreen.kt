package tuda.campus.mobile.screens.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.TimetableDetail
import tuda.campus.mobile.components.LoginLoadingScreen
import tuda.campus.mobile.components.RequestLoadingScreen
import tuda.campus.mobile.components.SmallNavTitle
import tuda.campus.mobile.managers.tucanManager.EventDate
import tuda.campus.mobile.managers.tucanManager.EventDate.CourseDate
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.SettingsManager
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.managers.TimeViewType
import tuda.campus.mobile.managers.TucanManager
import kotlinx.coroutines.delay
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.dates
import tudex.composeapp.generated.resources.ic_calendar_view_day_24px
import tudex.composeapp.generated.resources.ic_check_24px
import tudex.composeapp.generated.resources.ic_clock_24px
import tudex.composeapp.generated.resources.ic_distance_24px
import tudex.composeapp.generated.resources.ic_list_24px
import tudex.composeapp.generated.resources.list
import tudex.composeapp.generated.resources.noEventsToday
import tudex.composeapp.generated.resources.timetable
import tudex.composeapp.generated.resources.unknown
import tudex.composeapp.generated.resources.week
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TimetableScreen(
    navigateTo: (NavKey) -> Unit,
    backAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val dateStatus by TucanManager.dateStatus.collectAsStateWithLifecycle()
    val datesList by TucanManager.datesList.collectAsStateWithLifecycle()

    val courses by TucanManager.courses.collectAsStateWithLifecycle()
    val courseStatus by TucanManager.courseStatus.collectAsStateWithLifecycle()

    var dropDownExpanded by remember { mutableStateOf(false) }
    val viewType by SettingsManager.timeViewType.collectAsStateWithLifecycle()

    val timeTableStr = stringResource(Res.string.timetable)
    var navTitle by remember { mutableStateOf(timeTableStr) }

    val inlineContent = mapOf(
        "clock" to InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_clock_24px),
                contentDescription = null,
                tint = LocalContentColor.current
            )
        },
        "room" to InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_distance_24px),
                contentDescription = null,
                tint = LocalContentColor.current
            )
        }
    )

    SmallNavTitle(
        titleComp = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(navTitle)
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
        backAction = backAction,
        actions = {
            IconButton(
                onClick = { dropDownExpanded = !dropDownExpanded }
            ) {
                Icon(
                    painter = painterResource(resource =
                        if (viewType == TimeViewType.LIST)
                            Res.drawable.ic_list_24px
                        else
                            Res.drawable.ic_calendar_view_day_24px
                    ),
                    contentDescription = null
                )
            }

            DropdownMenuPopup(
                expanded = dropDownExpanded,
                onDismissRequest = { dropDownExpanded = false }
            ) {
                DropdownMenuGroup(
                    shapes = MenuDefaults.groupShapes()
                ) {
                    DropdownMenuItem(
                        selected = viewType == TimeViewType.LIST,
                        onClick = {
                            SettingsManager.updateTimeViewType(TimeViewType.LIST)
                            dropDownExpanded = false
                        },
                        text = { Text(stringResource(Res.string.list)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_list_24px),
                                contentDescription = null
                            )
                        },
                        trailingIcon =
                            if (viewType == TimeViewType.LIST) {
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

                    DropdownMenuItem(
                        selected = viewType == TimeViewType.WEEK,
                        onClick = {
                            SettingsManager.updateTimeViewType(TimeViewType.WEEK)
                            dropDownExpanded = false
                        },
                        text = { Text(stringResource(Res.string.week)) },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(Res.drawable.ic_calendar_view_day_24px),
                                contentDescription = null
                            )
                        },
                        trailingIcon =
                            if (viewType == TimeViewType.WEEK) {
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
    ) {
        if (datesList == null) {
            if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED ||
                tuLoginStatus == TULoginState.SSO_UNCONFIRMED
            ) {
                LoginLoadingScreen()
            } else if (dateStatus == RequestStatus.LOADING) {
                Column {
                    RequestLoadingScreen(
                        itemName = stringResource(Res.string.dates),
                        modifier = modifier.weight(1f)
                    )
                }
            }
        } else if (viewType == TimeViewType.WEEK) {
            datesList?.let { datesList ->
                TimetableWeek(
                    datesList = datesList,
                    updateNavTitle = { navTitle = it },
                    navigateTo = navigateTo
                )
            }
        } else if (viewType == TimeViewType.LIST) {
            datesList?.let { datesList ->
                TimetableList(
                    datesList = datesList,
                    inlineContent = inlineContent,
                    updateNavTitle = { navTitle = it },
                    navigateTo = navigateTo
                )
            }
        }
    }
}

fun getDateFormatter(locale: String): DateTimeFormat<LocalDate>? {
    return when (locale) {
        "EN" -> LocalDate.Format {
            val englishDays = DayOfWeekNames(
                listOf("Mon", "Tue", "Wed", "Th", "Fri", "Sat", "Sun")
            )
            val englishMonths = MonthNames(
                listOf("Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.",
                    "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec.")
            )

            // Supposed to be an alternative to byUnicodePattern("EEE, d. MMM. yyyy")
            dayOfWeek(englishDays)
            chars(", ")
            day(Padding.NONE)
            chars(". ")
            monthName(englishMonths)
            chars(" ")
            year()
        }
        "DE" -> LocalDate.Format {
            val germanDays = DayOfWeekNames(
                listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So")
            )
            val germanMonths = MonthNames(
                listOf("Jan.", "Feb.", "Mär.", "Apr.", "Mai", "Jun.",
                    "Jul.", "Aug.", "Sep.", "Okt.", "Nov.", "Dez.")
            )

            // Supposed to be an alternative to byUnicodePattern("EE, d. MMM yyyy")
            dayOfWeek(germanDays)
            chars(", ")
            day(Padding.NONE)
            chars(". ")
            monthName(germanMonths)
            chars(" ")
            year()
        }
        else -> null
    }
}

/**
 * Inserts an emptyList() entry at the correct position representing today
 */
private fun buildDatesListWithToday(
    lists: List<List<EventDate>?>,
    todayDateString: String,
    dateFormatter: DateTimeFormat<LocalDate>
): List<List<EventDate>?> {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    val firstIndexAfterToday = lists.indexOfFirst { dateList ->
        dateList?.firstOrNull()?.let { courseDate ->
            val date = dateFormatter.parse(courseDate.date)
            return@indexOfFirst date > today
        } ?: false
    }

    return if (firstIndexAfterToday >= 0) {
        if (firstIndexAfterToday >= 1) {
            val previousSection = lists[firstIndexAfterToday - 1]
            if (previousSection?.firstOrNull()?.date == todayDateString) {
                lists
            } else {
                lists.toMutableList().apply {
                    add(firstIndexAfterToday, null)
                }
            }
        } else {
            lists.toMutableList().apply {
                add(0, null)
            }
        }
    } else {
        lists + null
    }
}

data class TimeGrouping(
    val date: EventDate,
    val spacesBefore: Int,
    val spacesAfter: Int
)

fun determineTimeGroupings(dates: List<EventDate>): List<TimeGrouping> {
    var currentEndTimeMins: MutableList<Int> = mutableListOf(-1)
    val overlappingDateExtIndices: MutableList<Int> = mutableListOf()

    val groupedDates: MutableList<TimeGrouping> = dates.map { TimeGrouping(it, 0, 0) }.toMutableList()

    for (dateIndex in groupedDates.indices) {
        // Check if overlaps existing columns
        val potentialOverlaps: List<Int> = currentEndTimeMins.filter { endTimeMins ->
            endTimeMins > groupedDates[dateIndex].date.startTimeMins
        }

        // No overlaps - reset to single column
        if (potentialOverlaps.isEmpty()) {
            overlappingDateExtIndices.clear()
            overlappingDateExtIndices.add(dateIndex)
            currentEndTimeMins = mutableListOf(groupedDates[dateIndex].date.endTimeMins)
            continue
        }

        var overlapFound = false

        // Try to fit in existing column
        for (timeIndex in currentEndTimeMins.indices) {
            if (currentEndTimeMins[timeIndex] <= groupedDates[dateIndex].date.startTimeMins) {
                val spacesBefore = timeIndex
                val spacesAfter = currentEndTimeMins.size - timeIndex - 1

                overlappingDateExtIndices.add(dateIndex)
                currentEndTimeMins[timeIndex] = groupedDates[dateIndex].date.endTimeMins

                groupedDates[dateIndex] = TimeGrouping(
                    groupedDates[dateIndex].date,
                    spacesBefore,
                    spacesAfter
                )

                overlapFound = true
                break
            }
        }

        if (overlapFound) continue

        // Create new column - increment spacesAfter for all overlapping items
        for (overlapIndex in overlappingDateExtIndices) {
            val current = groupedDates[overlapIndex]
            groupedDates[overlapIndex] = TimeGrouping(
                current.date,
                current.spacesBefore,
                current.spacesAfter + 1
            )
        }

        // Add new item to new column
        val spacesBefore = currentEndTimeMins.size
        groupedDates[dateIndex] = TimeGrouping(
            groupedDates[dateIndex].date,
            spacesBefore,
            0
        )

        currentEndTimeMins.add(groupedDates[dateIndex].date.endTimeMins)
        overlappingDateExtIndices.add(dateIndex)
    }

    return groupedDates
}

@Composable
fun TimetableDay(
    dates: List<EventDate>?,
    onSlotClick: (EventDate) -> Unit,
    showTimeBobble: Boolean,
    modifier: Modifier = Modifier
) {
    val inlineContent = mapOf(
        "clock" to InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_clock_24px),
                contentDescription = null,
                tint = LocalContentColor.current
            )
        },
        "room" to InlineTextContent(
            Placeholder(
                width = 16.sp,
                height = 16.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.Center
            )
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_distance_24px),
                contentDescription = null,
                tint = LocalContentColor.current
            )
        }
    )

    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column {
                for (time in 0..24) {
                    Text(
                        text = "${time.toString().padStart(2, '0')}:00",
                        modifier = Modifier.height(64.dp)
                    )
                }
            }

            if (dates != null) {
                val sortedDates =
                    dates.sortedWith(compareBy<EventDate> { it.startTimeMins }.thenBy { it.endTimeMins })

                val groupedDateExts: List<TimeGrouping> =
                    determineTimeGroupings(sortedDates)

                Box {
                    groupedDateExts.forEach { timeGrouping ->
                        val date = timeGrouping.date

                        Column {
                            Spacer(modifier = Modifier.height(8.dp + 64.dp * (date.startTimeMins / 60f)))     // 8.dp provide general offset
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                if (timeGrouping.spacesBefore != 0) {
                                    Spacer(modifier = Modifier.weight(1f * timeGrouping.spacesBefore))
                                }

                                Column(
                                    modifier = Modifier
                                        .padding(vertical = 2.dp)       // together with -4.dp in height makes for better look
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            //color = MaterialTheme.colorScheme.primary,
                                            color =
                                                if (date is CourseDate)
                                                    MaterialTheme.colorScheme.primaryContainer
                                                else //if (date is Date.ExamDate)
                                                    MaterialTheme.colorScheme.tertiaryContainer
                                        )
                                        .clickable { onSlotClick(date) }
                                        .height(64.dp * ((date.endTimeMins - date.startTimeMins) / 60f) - 4.dp)   // -2.dp is less accurate but looks better
                                        .weight(1f)
                                        .padding(4.dp)
                                        .padding(horizontal = 4.dp)
                                ) {
                                    //CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                                    CompositionLocalProvider(LocalContentColor provides
                                            if (date is CourseDate)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else //if (date is Date.ExamDate)
                                                MaterialTheme.colorScheme.onTertiaryContainer
                                    ) {
                                        if (timeGrouping.date is EventDate.ExamDate) {
                                            Text(
                                                text = timeGrouping.date.examType,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }

                                        Text(
                                            text = timeGrouping.date.courseName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        if ((date.endTimeMins - date.startTimeMins) >= 60) {
                                            val timeText = buildAnnotatedString {
                                                appendInlineContent("clock")
                                                append(" ${timeGrouping.date.startTime}-${timeGrouping.date.endTime}")
                                            }

                                            Text(
                                                text = timeText,
                                                inlineContent = inlineContent,
                                                style = MaterialTheme.typography.labelMedium
                                            )

                                            if (timeGrouping.date is CourseDate) {
                                                val roomText = buildAnnotatedString {
                                                    appendInlineContent("room")
                                                    append(" ${timeGrouping.date.room}")
                                                }

                                                Text(
                                                    text = roomText,
                                                    inlineContent = inlineContent,
                                                    style = MaterialTheme.typography.labelMedium
                                                )
                                            }
                                        } else {
                                            val text = buildAnnotatedString {
                                                appendInlineContent("clock")
                                                append(" ${date.startTime}-${date.endTime}")
                                                if (timeGrouping.date is CourseDate) {
                                                    append("    ")
                                                    appendInlineContent("room")
                                                    append(" ${date.room}")
                                                }
                                            }

                                            Text(
                                                text = text,
                                                inlineContent = inlineContent,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }

                                if (timeGrouping.spacesAfter != 0) {
                                    Spacer(modifier = Modifier.weight(1f * timeGrouping.spacesAfter))
                                }
                            }
                        }
                    }
                }
            } else {
                Box {
                    for (i in 0..2) {
                        Column {
                            Spacer(modifier = Modifier.height(64.dp * (4 + 8*i) - 16.dp))
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(Res.string.noEventsToday),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        val currentTime by produceState(
            Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).time
        ) {
            while (true) {
                value = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault()).time
                delay(5000)
            }
        }

        Column {
            val minutes = currentTime.hour * 60 + currentTime.minute

            val minuteText = currentTime.minute.toString().padStart(2, '0')
            val hourText = currentTime.hour.toString().padStart(2, '0')

            Spacer(modifier = Modifier.height((64.dp * minutes/60f) - 8.dp))

            if (showTimeBobble) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$hourText:$minuteText",
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(start = 4.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 4.dp)
                    )
                    Box {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            thickness = 3.dp
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.tertiary,
                            thickness = 2.dp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableWeek(
    datesList: Pair<String, List<List<EventDate>>>,
    updateNavTitle: (String) -> Unit,
    navigateTo: (NavKey) -> Unit
) {
    val dateFormatter = remember(datesList.first) { getDateFormatter(locale = datesList.first) }
    dateFormatter?.let { dateFormatter ->
        val baseDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val middleIndex = Int.MAX_VALUE / 2

        val listState = rememberLazyListState(initialFirstVisibleItemIndex = middleIndex)
        val snapFlingBehavior = rememberSnapFlingBehavior(listState)
        var selectedItem by rememberSaveable { mutableStateOf(middleIndex) }

        var isPagerAnimated by remember { mutableStateOf(false) }

        val pagerState = rememberPagerState(
            pageCount = { Int.MAX_VALUE },
            initialPage = middleIndex
        )

        Column {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth()
            ) {
                val itemWidth = maxWidth / 7
                val density = LocalDensity.current

                var lastOffset by remember { mutableStateOf(0.dp) }
                val selectedItemOffset by remember {
                    derivedStateOf {
                        with(density) {
                            val offset = listState.layoutInfo.visibleItemsInfo
                                .find { it.index == selectedItem }
                                ?.offset ?: return@derivedStateOf lastOffset

                            val minimized = max(0, offset)
                            val bounded = min(minimized.toFloat(), maxWidth.toPx() / 7 * 6)


                            lastOffset = bounded.toDp()
                            return@derivedStateOf lastOffset
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .width(itemWidth + (0.1).dp)
                        .padding(top = 16.dp)
                        .padding(horizontal = 8.dp)
                        .offset(x = selectedItemOffset)
                ) {
                    Spacer(
                        modifier = Modifier
                            .background(
                                color =
                                    if (selectedItem == middleIndex)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .size(36.dp)
                    )
                }

                val onPrimaryColor = MaterialTheme.colorScheme.onPrimary

                LazyRow(
                    state = listState,
                    flingBehavior = snapFlingBehavior,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer(
                            compositingStrategy = CompositingStrategy.Offscreen
                        )
                        .drawWithContent {
                            drawContent()
                            drawCircle(
                                color = onPrimaryColor,
                                blendMode = BlendMode.SrcAtop,
                                center = Offset(
                                    x = (selectedItemOffset + itemWidth / 2 + 0.1.dp).toPx(),
                                    y = (16 + 18).dp.toPx()
                                ),
                                radius = 18.dp.toPx(),
                            )
                        }
                ) {
                    items(
                        count = Int.MAX_VALUE,
                        key = { it }
                    ) { index ->
                        val dayOffset = index - middleIndex
                        val date = baseDate.plus(dayOffset, DateTimeUnit.DAY)

                        val dateString = dateFormatter.format(date)
                        val day = dateString.split(",").first()
                        val dateNum = date.day.toString().padStart(2, '0')

                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier
                                .width(itemWidth + (0.1).dp)
                                .padding(horizontal = 8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    selectedItem = index
                                }
                        ) {
                            DateHeader(
                                day = day,
                                dateNum = dateNum,
                                isToday = baseDate == date,
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            val datesMap: Map<String, List<EventDate>> = datesList.second
                .filter { it.isNotEmpty() }
                .associateBy { datesList -> datesList.first().date }

            val currentHour = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
            val initial = with(LocalDensity.current) { (64.dp * (currentHour-2)).toPx() }.roundToInt()

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState(initial = initial))
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.padding(top = 8.dp)
                ) { pageOffset ->
                    val dayOffset = pageOffset - Int.MAX_VALUE / 2
                    val date = baseDate.plus(dayOffset, DateTimeUnit.DAY)
                    val dateString = dateFormatter.format(date)

                    val courseDates = datesMap[dateString]

                    TimetableDay(
                        dates = courseDates,
                        onSlotClick = { navigateTo(TimetableDetail(it)) },
                        showTimeBobble = pageOffset == middleIndex,
                    )
                }
            }


            // This is done because if the selectedItem were set to +7 during scroll,
            // The animation would look horrible. Still it should be accurate-ish so that
            // The Month updates during scroll, which is why it is set to +7
            LaunchedEffect(listState.isScrollInProgress) {
                try {
                    if (!listState.isScrollInProgress) {
                        if (selectedItem == listState.firstVisibleItemIndex + 7) {
                            selectedItem = listState.firstVisibleItemIndex + 6
                        }

                        if (pagerState.currentPage != selectedItem) {
                            isPagerAnimated = true
                            pagerState.animateScrollToPage(selectedItem)
                            isPagerAnimated = false
                        }
                    }
                } finally {
                    isPagerAnimated = false
                }
            }

            LaunchedEffect(listState.firstVisibleItemIndex) {
                if (selectedItem < listState.firstVisibleItemIndex) {
                    selectedItem = listState.firstVisibleItemIndex
                } else if (selectedItem > listState.firstVisibleItemIndex + 7) {
                    selectedItem = listState.firstVisibleItemIndex + 7
                }
            }

            val unknownStr = stringResource(Res.string.unknown)

            LaunchedEffect(selectedItem) {
                try {
                    if (!listState.isScrollInProgress) {
                        isPagerAnimated = true
                        pagerState.animateScrollToPage(selectedItem)
                        isPagerAnimated = false
                    }

                    val dayOffset = selectedItem - middleIndex
                    val date = baseDate.plus(dayOffset, DateTimeUnit.DAY)

                    val year = date.year.toString()

                    val month =
                        if (datesList.first == "EN") {
                            date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                        } else if (datesList.first == "DE") {
                            when (date.month) {
                                Month.JANUARY -> "Januar"
                                Month.FEBRUARY -> "Februar"
                                Month.MARCH -> "März"
                                Month.APRIL -> "April"
                                Month.MAY -> "Mai"
                                Month.JUNE -> "Juni"
                                Month.JULY -> "Juli"
                                Month.AUGUST -> "August"
                                Month.SEPTEMBER -> "September"
                                Month.OCTOBER -> "Oktober"
                                Month.NOVEMBER -> "November"
                                Month.DECEMBER -> "Dezember"
                            }
                        } else {
                            unknownStr
                        }

                    updateNavTitle("$month $year")
                } finally {
                    isPagerAnimated = false
                    //selectedItem = pagerState.currentPage
                }
            }

            LaunchedEffect(pagerState.currentPage) {
                if (!isPagerAnimated) {
                    selectedItem = pagerState.currentPage

                    if (selectedItem < listState.firstVisibleItemIndex) {
                        listState.animateScrollToItem(selectedItem - 6)
                    } else if (selectedItem > listState.firstVisibleItemIndex + 6) {
                        listState.animateScrollToItem(selectedItem)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TimetableList(
    datesList: Pair<String, List<List<EventDate>>>,
    inlineContent: Map<String, InlineTextContent>,
    updateNavTitle: (String) -> Unit,
    navigateTo: (NavKey) -> Unit
) {
    val dateFormatter = remember(datesList.first) { getDateFormatter(locale = datesList.first) }
    dateFormatter?.let { dateFormatter ->

        val todayDateString = remember {
            dateFormatter.format(Clock.System.todayIn(TimeZone.currentSystemDefault()))
        }
        val datesListWithToday = remember(datesList) {
            buildDatesListWithToday(
                lists = datesList.second,
                todayDateString = todayDateString,
                dateFormatter = dateFormatter
            )
        }

        val todayHeaderIndex = remember(datesListWithToday, todayDateString) {
            val index = datesListWithToday.indexOfFirst { dates ->
                dates?.firstOrNull()?.date == todayDateString || /*dates.isEmpty()*/ dates == null
            }

            if (index == -1) return@remember 0
            else return@remember index
        }

        // End indexes in the names list for each char
        val endIndexes = remember(datesListWithToday) {
            getEndIndexes(datesListWithToday)
        }

        // Start indexes in the names list for each char
        val startIndexes = remember(datesListWithToday) {
            getStartIndexes(datesListWithToday)
        }

        val ranges = remember(datesListWithToday) {
            getRanges(datesListWithToday)
        }

        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = startIndexes[todayHeaderIndex]
        )

        val moveStickyHeader by remember {
            derivedStateOf {
                endIndexes.contains(listState.firstVisibleItemIndex + 1)
            }
        }

        Box(
            contentAlignment = Alignment.TopStart,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy((-16).dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                //val flattenedList = datesListWithToday.flatten()
                val flattenedList = datesListWithToday.flatMap { it ?: listOf(null) }

                itemsIndexed(flattenedList) { index, date ->
                    if (date == null) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy((-6).dp),
                            modifier = Modifier.padding(bottom = 56.dp)
                        ) {
                            val showHeader = listState.firstVisibleItemIndex != index

                            val localDate = dateFormatter.parse(todayDateString)
                            val day = todayDateString.split(",").first()
                            val dateNum = localDate.day.toString().padStart(2, '0')

                            DateHeader(
                                day = day,
                                dateNum = dateNum,
                                isToday = true,
                                isSelected = true,
                                modifier = Modifier
                                    .alpha(if (showHeader) 1f else 0f)
                                    .alpha(if (startIndexes.contains(index)) 1f else 0f)
                            )

                            ListItem(
                                headlineContent = {
                                    Text(stringResource(Res.string.noEventsToday))
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy((-6).dp)
                        ) {
                            val showHeader = listState.firstVisibleItemIndex != index

                            val localDate = dateFormatter.parse(date.date)
                            val day = date.date.split(",").first()
                            val dateNum = localDate.day.toString().padStart(2, '0')

                            DateHeader(
                                day = day,
                                dateNum = dateNum,
                                isToday = date.date == todayDateString,
                                isSelected = date.date == todayDateString,
                                isCompressed = true,
                                modifier = Modifier
                                    .alpha(if (showHeader) 1f else 0f)
                                    .alpha(if (startIndexes.contains(index)) 1f else 0f)
                            )

                            val text = buildAnnotatedString {
                                appendInlineContent("clock")
                                append(" ${date.startTime}-${date.endTime}")
                                if (date is CourseDate) {
                                    append("    ")
                                    appendInlineContent("room")
                                    append(" ${date.room}")
                                }
                            }

                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = date.courseName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color =
                                            if (date is EventDate.ExamDate)
                                                MaterialTheme.colorScheme.error
                                            else
                                                Color.Unspecified
                                    )
                                },
                                supportingContent = {
                                    if (date is EventDate.ExamDate) {
                                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                                            Text(
                                                text = text,
                                                inlineContent = inlineContent,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = text,
                                            inlineContent = inlineContent
                                        )
                                    }
                                },
                                overlineContent =
                                    if (date is EventDate.ExamDate) { {
                                         Text(
                                            text = date.examType,
                                            color = MaterialTheme.colorScheme.error
                                         )
                                    } } else null
                                ,
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .offset(y = (-16).dp)
                                    .padding(
                                        bottom =
                                            if (endIndexes.contains(index + 1) && index < flattenedList.size - 1) 24.dp
                                            else 0.dp
                                    )
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { navigateTo(TimetableDetail(date)) }
                            )
                        }
                    }
                }
            }

            val visibleIndex = listState.firstVisibleItemIndex
            val sectionIndex = ranges.indexOfFirst { visibleIndex in it }

            val dateString = datesListWithToday.getOrNull(sectionIndex)?.firstOrNull()?.date ?: todayDateString
            val date = dateFormatter.parse(dateString)
            val day = dateString.split(",").first()
            val dateNum = date.day.toString().padStart(2, '0')

            val unknownStr = stringResource(Res.string.unknown)

            LaunchedEffect(date.month, date.year) {
                val year = date.year.toString()

                val month =
                    if (datesList.first == "EN") {
                        date.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    } else if (datesList.first == "DE") {
                        when(date.month) {
                            Month.JANUARY -> "Januar"
                            Month.FEBRUARY -> "Februar"
                            Month.MARCH -> "März"
                            Month.APRIL -> "April"
                            Month.MAY -> "Mai"
                            Month.JUNE -> "Juni"
                            Month.JULY -> "Juli"
                            Month.AUGUST -> "August"
                            Month.SEPTEMBER -> "September"
                            Month.OCTOBER -> "Oktober"
                            Month.NOVEMBER -> "November"
                            Month.DECEMBER -> "Dezember"
                        }
                    } else {
                        unknownStr
                    }

                updateNavTitle("$month $year")
            }

            DateHeader(
                day = day,
                dateNum = dateNum,
                isToday = dateString == todayDateString,
                isSelected = dateString == todayDateString,
                isCompressed = true,
                modifier =
                    if (moveStickyHeader) {
                        Modifier
                            .clip(RectangleShape)
                            .offset {
                                IntOffset(0, -listState.firstVisibleItemScrollOffset)
                            }
                    } else {
                        Modifier
                    }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DateHeader(
    day: String,
    dateNum: String,
    isToday: Boolean = false,
    isSelected: Boolean = false,
    isCompressed: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textColor =
        if (isToday)
            MaterialTheme.colorScheme.primary
        else
            if (day == "Sa" || day == "So" || day == "Sat" || day == "Sun")
                if (isSystemInDarkTheme())
                    MaterialTheme.colorScheme.outline
                else
                    MaterialTheme.colorScheme.outlineVariant
            else
                MaterialTheme.colorScheme.onSurface

    val onCircleColor =
        if (isToday)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.inverseOnSurface


    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy( if (isCompressed && !isSelected) (-4).dp else 0.dp),
        modifier = modifier
    ) {
        Text(
            text = day,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
        )

        Box(contentAlignment = Alignment.Center) {
            if (isSelected) {
                Spacer(
                    modifier = Modifier
                        .background(
                            color = textColor,
                            shape = CircleShape
                        )
                        .size(36.dp)
                )
            } else {
                Spacer(modifier = Modifier.size(36.dp))
            }

            Text(
                text = dateNum,
                color = if (isSelected) onCircleColor else textColor,
                style = MaterialTheme.typography.headlineSmallEmphasized,
                fontWeight = if (isToday) FontWeight.Bold else null
            )
        }
    }
}

fun <T> getStartIndexes(lists: List<List<T>?>): List<Int> {
    var acc = 0
    return lists.map { list ->
        val start = acc
        acc += list?.size ?: 1
        start
    }
}

fun <T> getEndIndexes(lists: List<List<T>?>): List<Int> {
    var acc = 0
    return lists.map { list ->
        acc += list?.size ?: 1
        acc
    }
}

fun <T> getRanges(lists: List<List<T>?>): List<IntRange> {
    var acc = 0
    return lists.map { list ->
        val start = acc
        acc += list?.size ?: 1
        start until acc   // end is exclusive
    }
}
