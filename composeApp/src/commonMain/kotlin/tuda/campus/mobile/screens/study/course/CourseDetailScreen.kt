package tuda.campus.mobile.screens.study.course

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.managers.tucanManager.Course
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.blockWithStr
import tudex.composeapp.generated.resources.courseDetailsNotFound
import tudex.composeapp.generated.resources.dates
import tudex.composeapp.generated.resources.deadlineForAuditWithStr
import tudex.composeapp.generated.resources.deadlines
import tudex.composeapp.generated.resources.endCancellationWithStr
import tudex.composeapp.generated.resources.endRegistrationWithStr
import tudex.composeapp.generated.resources.group
import tudex.composeapp.generated.resources.groups
import tudex.composeapp.generated.resources.info
import tudex.composeapp.generated.resources.phaseWithStr
import tudex.composeapp.generated.resources.plenum
import tudex.composeapp.generated.resources.startWithStr

private enum class CourseTab {
    Info, Groups, Deadlines, Dates;

    val displayName: String
        @Composable
        get() = when(this) {
            Info -> stringResource(Res.string.info)
            Groups -> stringResource(Res.string.groups)
            Deadlines -> stringResource(Res.string.deadlines)
            Dates -> stringResource(Res.string.dates)
        }
}

@Composable
fun CourseDetailScreen(
    course: Course,
    backAction: () -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    MediumNavTitle(
        backAction = backAction,
        titleComp = { expanded ->
            if (expanded) {
                Column {
                    Text(
                        text = "${course.courseID} - ${course.semester}",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = course.courseName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            } else {
                Text(
                    text = course.courseName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) {
        course.courseDetails?.let { courseDetails ->

            val tabs = buildList {
                add(CourseTab.Info)
                if (courseDetails.groups.isNotEmpty()) add(CourseTab.Groups)
                if (courseDetails.regDeadlines.isNotEmpty()) add(CourseTab.Deadlines)
                if (courseDetails.dates != null || courseDetails.plenaryDates != null) add(CourseTab.Dates)
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                SecondaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index }
                        ) {
                            Text(
                                text = tab.displayName,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .padding(bottom = 2.dp)
                            )
                        }
                    }
                }

                when (tabs[selectedTabIndex]) {
                    CourseTab.Info -> {
                        Text(courseDetails.details, modifier = Modifier.padding(bottom = 16.dp))
                    }
                    CourseTab.Groups -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            courseDetails.groups.forEach { group ->
                                ListItem(
                                    headlineContent = { Text(group.title) },
                                    supportingContent = { Text(group.teachers) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    )
                                )
                            }
                        }
                    }
                    CourseTab.Deadlines -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(20.dp))
                        ) {
                            courseDetails.regDeadlines.forEach { regDeadline ->
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceBright)
                                        .clip(RoundedCornerShape(2.dp))
                                        .padding(16.dp)
                                        .fillMaxWidth()
                                ) {
                                    Text(stringResource(Res.string.phaseWithStr, regDeadline.phase))
                                    Text(stringResource(Res.string.blockWithStr, regDeadline.block))
                                    Text(stringResource(Res.string.startWithStr, regDeadline.startDate))
                                    Text(stringResource(Res.string.endRegistrationWithStr, regDeadline.endRegisterDate))
                                    Text(stringResource(Res.string.endCancellationWithStr, regDeadline.endDeregisterDate))
                                    Text(stringResource(Res.string.deadlineForAuditWithStr, regDeadline.endListenerDate))
                                }
                            }
                        }
                    }
                    CourseTab.Dates -> {
                        courseDetails.dates?.let { dates ->
                            HeaderText(
                                text = stringResource(Res.string.group),
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            ) {
                                dates.forEach { date ->
                                    ListItem(
                                        headlineContent = { Text("${date.date}, ${date.startTime}-${date.endTime}") },
                                        trailingContent = { Text(date.room) },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceBright
                                        )
                                    )
                                }
                            }
                        }

                        courseDetails.plenaryDates?.let { plenaryDates ->
                            HeaderText(
                                text = stringResource(Res.string.plenum),
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            ) {
                                plenaryDates.forEach { date ->
                                    ListItem(
                                        headlineContent = { Text("${date.date}, ${date.startTime}-${date.endTime}") },
                                        trailingContent = { Text(date.room) },
                                        colors = ListItemDefaults.colors(
                                            containerColor = MaterialTheme.colorScheme.surfaceBright
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (course.courseDetails == null) {
            Text(stringResource(Res.string.courseDetailsNotFound))
        }
    }
}