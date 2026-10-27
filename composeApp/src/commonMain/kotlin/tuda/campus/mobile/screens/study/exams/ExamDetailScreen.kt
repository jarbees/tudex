package tuda.campus.mobile.screens.study.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tuda.campus.mobile.components.CustomList
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.MediumNavTitle
import tuda.campus.mobile.managers.tucanManager.Exam
import tuda.campus.mobile.managers.tucanManager.GradeStats
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.assessment
import tudex.composeapp.generated.resources.averageSpace
import tudex.composeapp.generated.resources.examsSpace
import tudex.composeapp.generated.resources.failWithPercent
import tudex.composeapp.generated.resources.gradeReport
import tudex.composeapp.generated.resources.passWithPercent
import tudex.composeapp.generated.resources.unknown
import kotlin.let
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExamDetailScreen(
    exam: Exam,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    //SmallNavTitle(backAction = navigateBack) {
    MediumNavTitle(
        backAction = navigateBack,
        titleComp = { expanded ->
            if (expanded) {
                Column {
                    Text(
                        exam.courseID,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        exam.courseName,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        exam.examinationDateTime,
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text(
                    text = exam.courseName,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    ) {
        var surfaceWidth by remember { mutableFloatStateOf(0f) }
        var selectedIndex by remember { mutableStateOf<Int?>(null) }

        var tooltipWidth by remember { mutableFloatStateOf(0f) }
        var tooltipPosition by remember { mutableFloatStateOf(0f) }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { _ ->      // onTap only fires, if no dragGesture starts
                            selectedIndex = null
                        }
                    )
                }
        ) {
            /*
            Column(
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    exam.courseID,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    exam.courseName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    exam.examinationDateTime,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
             */

            HeaderText(stringResource(Res.string.assessment))
            ListItem(
                headlineContent = { Text(if (exam.grade == "") stringResource(Res.string.unknown) else "${exam.gradeName} (${exam.grade})") },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(20.dp))
            )

            exam.gradeStats?.let { gradeStats ->
                HeaderText(stringResource(Res.string.gradeReport))
                if (!(gradeStats.grades.isEmpty() && gradeStats.gradeAvg == null && gradeStats.resultCount == null)) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .fillMaxWidth()
                    ) {

                        Column(
                            modifier = Modifier
                                .onSizeChanged { surfaceWidth = it.width.toFloat() }
                                .padding(16.dp)
                        ) {
                            // ChartHeader
                            Box {
                                DefaultChartHeader(
                                    courseName = exam.courseName,
                                    resultCount = gradeStats.resultCount,
                                    gradeAvg = gradeStats.gradeAvg,
                                    modifier = Modifier
                                        .alpha(if (selectedIndex == null) 1f else 0f)
                                        .padding(bottom = if (gradeStats.grades.isNotEmpty()) 16.dp else 0.dp)
                                )

                                GradeChartHeader(
                                    selectedIndex = selectedIndex,
                                    tooltipWidth = tooltipWidth,
                                    updateTooltipWidth = { tooltipWidth = it },
                                    tooltipPosition = tooltipPosition,
                                    surfaceWidth = surfaceWidth,
                                    gradeStats = gradeStats,
                                    modifier = Modifier.alpha(if (selectedIndex != null) 1f else 0f)
                                )
                            }

                            if (gradeStats.grades.isNotEmpty()) {
                                // ChartView
                                ChartView(
                                    grades = gradeStats.grades,
                                    selectedIndex = selectedIndex,
                                    updateSelectedIndex = { selectedIndex = it },
                                    updateTooltipPosition = { tooltipPosition = it }
                                )


                                // FailPassBar
                                if (gradeStats.resultCount != null && gradeStats.resultCount != "0") {
                                    gradeStats.grades.firstOrNull { it.first == "nb" || it.first == "5,0" }
                                        ?.let { failedGrade ->
                                            val totalGrades = gradeStats.resultCount!!.toInt()
                                            val failedGrades = failedGrade.second.toFloat()
                                            val failPercentage: Float = failedGrades / totalGrades

                                            FailPassBar(failPercentage)
                                        }
                                }
                            }
                        }
                    }
                }

                CustomList(
                    gradeStats.otherCounts.map { otherCount ->
                        {
                            if (otherCount.second != null) {
                                ListItem(
                                    headlineContent = { Text("${otherCount.first}: ${otherCount.second}") },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    )
                                )
                            } else {
                                ListItem(
                                    headlineContent = { Text(otherCount.first) },
                                    colors = ListItemDefaults.colors(
                                        containerColor = MaterialTheme.colorScheme.surfaceBright
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun DefaultChartHeader(
    courseName: String,
    resultCount: String?,
    gradeAvg: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Text(
            courseName,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (!(resultCount == null && gradeAvg == null)) {
            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    ) {
                        append(stringResource(Res.string.examsSpace))
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(resultCount ?: "???")
                    }
                }
            )

            Text(
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize
                        )
                    ) {
                        append(stringResource(Res.string.averageSpace))
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(gradeAvg ?: "???")
                    }
                }
            )
        } else {
            Spacer(modifier = Modifier.fillMaxHeight())
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GradeChartHeader(
    selectedIndex: Int?,
    tooltipWidth: Float,
    updateTooltipWidth: (Float) -> Unit,
    tooltipPosition: Float,
    surfaceWidth: Float,
    gradeStats: GradeStats,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .offset(x = with(LocalDensity.current) {
                ((tooltipPosition - tooltipWidth / 2).toDp() - 32.dp)   // 32.dp is the padding to the left
                    .coerceIn(
                        0.dp,       // Lower offset bound
                        maxOf(      // before surfaceWidth is set, use default of 0.dp to avoid crash
                            (surfaceWidth - tooltipWidth).toDp() - 32.dp,   // 32.dp is the padding to the left
                            0.dp
                        )
                    )
            })
            .onSizeChanged { //tooltipWidth = it.width.toFloat()
                updateTooltipWidth(it.width.toFloat())
            }
            .padding(bottom = 12.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        selectedIndex?.let { index ->
            val gradeName = gradeStats.grades[index].first

            val gradeCount = gradeStats.grades[index].second
            val totalCount = gradeStats.resultCount?.toInt()

            Text(
                gradeName,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (totalCount != null && totalCount != 0) {
                val percentage = ((gradeCount.toDouble() / totalCount) * 100).roundToInt()
                Text(
                    "$gradeCount ($percentage%)",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            } else {
                Text(
                    "$gradeCount",
                    style = MaterialTheme.typography.titleMediumEmphasized
                )
            }
        }
    }
}

@Composable
private fun ChartView(
    grades: List<Pair<String, Int>>,
    selectedIndex: Int?,
    updateSelectedIndex: (Int) -> Unit,
    updateTooltipPosition: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val SCALING_SIZES = listOf(3, 6, 10, 15, 20, 30, 40, 50, 75, 100, 150, 200, 300, 400, 500, 600, 800, 1000, 1200, 1500, 2000)

    val largestGrade: Pair<String, Int> = grades.maxBy { it.second }     // grades always has an entry if gradeStats exists

    val scale = SCALING_SIZES.firstOrNull { it >= largestGrade.second }
        ?: (((largestGrade.second + 999) / 1000) * 1000)    // A fallback that probably will never be triggered

    val divisions =
        if (scale % 3 == 0) 3
        else if (scale % 4 == 0) 4
        else 2

    val amountPerDivision = scale / divisions

    var totalWidth by remember { mutableFloatStateOf(0f) }
    var paddingWidth by remember { mutableFloatStateOf(0f) }
    val itemWidth = (totalWidth - paddingWidth) / (grades.size)

    Column(
        modifier = modifier
            .padding(bottom = 12.dp)
            .height(300.dp)
    ) {
        Box(
            modifier = Modifier.weight(1f)
        ) {
            // Side Labels
            Column {
                for (divisionIdx in divisions downTo 0) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f))
                        Text(
                            text = "${divisionIdx * amountPerDivision}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            // This modifier reports a height of 0 to column for layout
                            modifier = Modifier.layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, 0) {  // Report 0 height to Row/Column
                                    placeable.placeRelative(0, -placeable.height / 2)  // Center vertically
                                }
                            }
                        )
                    }

                    if (divisionIdx != 0) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Main Content
            Row {
                Row(
                    modifier = Modifier
                        .onSizeChanged { totalWidth = it.width.toFloat() }
                        .weight(1f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { offset ->     // onTap only fires, if no dragGesture starts
                                    val index =
                                        ((offset.x - (paddingWidth / 2)) / itemWidth).toInt()
                                            .coerceIn(0, grades.size - 1)

                                    updateSelectedIndex(index)
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ ->
                                change.consume()

                                val newIndex = ((change.position.x - (paddingWidth / 2)) / itemWidth).toInt()
                                    .coerceIn(0, grades.size - 1)

                                if (newIndex != selectedIndex) {
                                    updateSelectedIndex(newIndex)
                                }
                            }
                        }
                ) {

                    Spacer(
                        modifier = Modifier
                            .weight(0.25f)
                    )

                    grades.forEachIndexed { index, grade ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            val percentage = grade.second.toFloat() / scale

                            // Bars and Highlight Line
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                //if (pressedHoverIndex == index) {
                                if (selectedIndex == index) {
                                    VerticalDivider(
                                        modifier = Modifier
                                            .layout { measurable, constraints ->
                                                val overridenHeight =
                                                    constraints.maxHeight + 12.dp.roundToPx()
                                                val placeable =
                                                    measurable.measure(constraints.copy(maxHeight = overridenHeight))
                                                layout(placeable.width, placeable.height) {
                                                    placeable.place(0, 0)
                                                }
                                            }
                                            .offset(y = (-6).dp)
                                            .onGloballyPositioned { coordinates ->
                                                updateTooltipPosition(coordinates.positionInRoot().x)
                                            }
                                    )
                                }

                                Column {
                                    if (percentage != 1.toFloat()) {
                                        Spacer(modifier = Modifier.weight(1 - percentage))
                                    }

                                    if (percentage != 0.toFloat()) {
                                        Spacer(
                                            modifier = Modifier
                                                .weight(percentage)
                                                .fillMaxWidth()
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = RoundedCornerShape(
                                                        topStart = 2.dp,
                                                        topEnd = 2.dp,
                                                        bottomStart = 0.dp,
                                                        bottomEnd = 0.dp
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        if (index != grades.count() - 1) {
                            Spacer(modifier = Modifier.weight(0.4f))
                        }
                    }

                    Spacer(
                        modifier = Modifier
                            .onSizeChanged { paddingWidth = it.width.toFloat() * 2 }
                            .weight(0.2f)
                    )
                }
                // This text reserves space for the division labels
                Text(
                    "$scale",
                    modifier = Modifier.alpha(0f)
                )
            }
        }

        // Bottom Labels
        Row {
            Spacer(modifier = Modifier.weight(0.25f))
            grades.forEach { grade ->
                Text(
                    grade.first,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .weight(1f)
                )

                if (grade != grades.last()) {
                    Spacer(modifier = Modifier.weight(0.4f))
                }
            }

            Spacer(modifier = Modifier.weight(0.2f))

            // This text reserves space for the division labels
            Text(
                "$scale",
                modifier = Modifier.alpha(0f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FailPassBar(
    failPercentage: Float
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(Res.string.passWithPercent, ((1 - failPercentage) * 100).roundToInt()),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleMediumEmphasized,
            lineHeight = 18.sp
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
        ) {
            if (failPercentage != 1.toFloat()) {
                Spacer(
                    modifier = Modifier
                        .weight(1 - failPercentage)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }


            if (failPercentage != 0.toFloat()) {
                Spacer(
                    modifier = Modifier
                        .weight(failPercentage)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }

        Text(
            stringResource(Res.string.failWithPercent, ((failPercentage) * 100).roundToInt()),
            style = MaterialTheme.typography.titleMediumEmphasized,
            lineHeight = 18.sp
        )
    }
}



@Preview("D1")
@Composable
private fun Preview1() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID = "20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime = "",
        grade = "1,0",
        gradeName = "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = listOf(
                Pair(first = "1,0", second = 1),
                Pair(first = "1,3", second = 3),
                Pair(first = "1,7", second = 5),
                Pair(first = "2,0", second = 7),
                Pair(first = "2,3", second = 10),
                Pair(first = "2,7", second = 20),
                Pair(first = "3,0", second = 30),
                Pair(first = "3,3", second = 40),
                Pair(first = "3,7", second = 70),
                Pair(first = "4,0", second = 100),
                Pair(first = "5,0", second = 150)
            ),
            gradeAvg = "3,98",
            resultCount = "436",
            otherCounts = listOf(
                Pair(first = "fehlend (Krankschreibung)", second = 6)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Preview("D2")
@Composable
private fun Preview2() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID ="20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime =  "",
        grade =  "1,0",
        gradeName =  "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = listOf(
                Pair(first = "b", second = 20),
                Pair(first = "nb", second = 50)
            ),
            gradeAvg = "1,0",
            resultCount = "70",
            otherCounts = listOf(
                Pair(first = "fehlend (Krankschreibung)", second = 6)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Preview("A1")
@Composable
private fun Preview3() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID ="20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime =  "",
        grade =  "1,0",
        gradeName =  "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = emptyList(),
            gradeAvg = null,
            resultCount = null,
            otherCounts = listOf(
                Pair(first = "nicht gesetzt", second = null)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Preview("A2")
@Composable
private fun Preview4() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID ="20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime =  "",
        grade =  "1,0",
        gradeName =  "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = listOf(
                Pair(first = "1,0", second = 1),
                Pair(first = "1,3", second = 3),
                Pair(first = "1,7", second = 5),
                Pair(first = "2,0", second = 7),
                Pair(first = "2,3", second = 10),
                Pair(first = "2,7", second = 20),
                Pair(first = "3,0", second = 30),
                Pair(first = "3,3", second = 40),
                Pair(first = "3,7", second = 70),
                Pair(first = "4,0", second = 100),
                Pair(first = "5,0", second = 150)
            ),
            gradeAvg = null,
            resultCount = null,
            otherCounts = listOf(
                Pair(first = "nicht gesetzt", second = null)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Preview("A3")
@Composable
private fun Preview5() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID ="20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime =  "",
        grade =  "1,0",
        gradeName =  "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = emptyList(),
            gradeAvg = "1,0",
            resultCount = null,
            otherCounts = listOf(
                Pair(first = "nicht gesetzt", second = null)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Preview("A4")
@Composable
private fun Preview6() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID ="20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime =  "",
        grade =  "1,0",
        gradeName =  "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = emptyList(),
            gradeAvg = "1,0",
            resultCount = "0",
            otherCounts = listOf(
                Pair(first = "nicht gesetzt", second = null)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

@Preview("A5")
@Composable
private fun Preview7() {
    val exam = Exam(
        semester = "SoSe 2029",
        courseID ="20-0000-01",
        courseName = "Introduction to Computer",
        examType = "Oral/Written Exam",
        examinationDateTime =  "",
        grade =  "1,0",
        gradeName =  "sehr gut",
        gradeSheetArgs = null,
        gradeStats = GradeStats(
            grades = listOf(
                Pair(first = "1,0", second = 1),
                Pair(first = "1,3", second = 3),
                Pair(first = "1,7", second = 5),
                Pair(first = "2,0", second = 7),
                Pair(first = "2,3", second = 10),
                Pair(first = "2,7", second = 20),
                Pair(first = "3,0", second = 30),
                Pair(first = "3,3", second = 40),
                Pair(first = "3,7", second = 70),
                Pair(first = "4,0", second = 100),
                Pair(first = "5,0", second = 150)
            ),
            gradeAvg = null,
            resultCount = "0",
            otherCounts = listOf(
                Pair(first = "nicht gesetzt", second = null)
            )
        )
    )

    ExamDetailScreen(
        exam = exam,
        navigateBack = {},
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    )
}

