package tuda.campus.mobile.screens.study.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.TimetableDetail
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.managers.tucanManager.EventDate
import tuda.campus.mobile.managers.tucanManager.EventDate.CourseDate
import tuda.campus.mobile.managers.TucanManager
import tuda.campus.mobile.mapAnnotations
import tuda.campus.mobile.screens.AnnotatedMap
import tuda.campus.mobile.screens.mapLauncher
import tuda.campus.mobile.screens.study.TimeGrouping
import tuda.campus.mobile.screens.study.determineTimeGroupings
import tuda.campus.mobile.screens.study.getDateFormatter
import kotlinx.coroutines.delay
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.ic_clock_24px
import tudex.composeapp.generated.resources.ic_distance_24px
import kotlin.time.Clock

@Composable
fun TimetableDetailScreen(
    navigateBack: () -> Unit,
    navigateTo: (NavKey) -> Unit,
    date: EventDate
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

    MediumNavTitle(
        backAction = navigateBack,
        titleComp = { expanded ->
            if (expanded) {
                Column {
                    Text(
                        date.courseName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (date is CourseDate) {
                        Text(
                            date.teachers,
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            } else {
                Text(
                    text = date.courseName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            val timeText = buildAnnotatedString {
                appendInlineContent("clock")
                append(" ${date.date}, ${date.startTime}-${date.endTime}")
            }

            Text(text = timeText, inlineContent = inlineContent)

            if (date is CourseDate) {
                val roomText = buildAnnotatedString {
                    appendInlineContent("room")
                    append(" ${date.room}")
                }

                Text(text = roomText, inlineContent = inlineContent)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val datesList by TucanManager.datesList.collectAsStateWithLifecycle()


            datesList?.let { datesList ->
                val dateFormatter = remember(datesList.first) { getDateFormatter(locale = datesList.first) }
                dateFormatter?.let { dateFormatter ->
                    datesList.second.firstOrNull { it.firstOrNull()?.date == date.date }?.let { dates ->
                        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val isToday = date.date == dateFormatter.format(today)

                        TimetableCutout(
                            dates = dates,
                            selectedDate = date,
                            isToday = isToday,
                            navigateTo = navigateTo,
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceBright)
                        )
                    }
                }
            }

            if (date is CourseDate) {
                val mapLauncher = mapLauncher()

                mapAnnotations.find { annotation ->
                    date.room
                        .replace("|", "")
                        .contains(annotation.annotationTitle.replace("|", ""))
                }?.let { mapAnnotation ->
                    Box(
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .weight(1f)
                    ) {
                        AnnotatedMap(
                            selectedMarker = mapAnnotation,
                            modifier = Modifier
                                .fillMaxSize()
                        )

                        Spacer(
                            modifier = Modifier
                                .clickable { mapLauncher.launchMap(mapAnnotation) }
                                .fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimetableCutout(
    dates: List<EventDate>,
    selectedDate: EventDate,
    isToday: Boolean,
    navigateTo: (NavKey) -> Unit,
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

    val startHour = selectedDate.startTimeMins / 60

    Box(
        modifier = modifier.clipToBounds()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Column {
                for (time in startHour..24) {
                    Text(
                        text = "${time.toString().padStart(2, '0')}:00",
                        modifier = Modifier.height(64.dp)
                    )
                }
            }

            val sortedDates =
                dates.sortedWith(compareBy<EventDate> { it.startTimeMins }.thenBy { it.endTimeMins })

            val groupedDateExts: List<TimeGrouping> =
                determineTimeGroupings(sortedDates)

            Box {
                groupedDateExts.forEach { timeGrouping ->
                    val date = timeGrouping.date

                    val offset = (8.dp + 64.dp * ((date.startTimeMins / 60f) - startHour ))

                    Column(
                        modifier = Modifier.offset(y = offset)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (timeGrouping.spacesBefore != 0) {
                                Spacer(modifier = Modifier.weight(1f * timeGrouping.spacesBefore))
                            }

                            Column(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)       // together with -4.dp in height makes for better look
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        color =
                                            if (date is CourseDate)
                                                if (date == selectedDate)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.primaryContainer
                                            else //if (date is ExamDate)
                                                if (date == selectedDate)
                                                    MaterialTheme.colorScheme.tertiary
                                                else
                                                    MaterialTheme.colorScheme.tertiaryContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .then(
                                        if (date == selectedDate)
                                            Modifier
                                        else
                                            Modifier.clickable { navigateTo(TimetableDetail(date)) }
                                    )
                                    .height(64.dp * ((date.endTimeMins - date.startTimeMins) / 60f) - 4.dp)   // -2.dp is less accurate but looks better
                                    .weight(1f)
                                    .padding(4.dp)
                                    .padding(horizontal = 4.dp)
                            ) {
                                //CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onPrimary) {
                                CompositionLocalProvider(LocalContentColor provides
                                        if (date is CourseDate)
                                            if (date == selectedDate)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        else //if (date is ExamDate)
                                            if (date == selectedDate)
                                                MaterialTheme.colorScheme.onTertiary
                                            else
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

                                        if (date is CourseDate) {
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
                                            if (date is CourseDate) {
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


        if (isToday) {
            val minutes = currentTime.hour * 60 + currentTime.minute

            val minuteText = currentTime.minute.toString().padStart(2, '0')
            val hourText = currentTime.hour.toString().padStart(2, '0')

            val offset = (64.dp * (minutes/60f - startHour)) - 8.dp

            Column(modifier = Modifier.offset(y = offset)) {
                //Spacer(modifier = Modifier.height((64.dp * (minutes/60f - startHour)) - 8.dp))
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
                            color = MaterialTheme.colorScheme.surfaceBright,
                            thickness = (2.5).dp
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