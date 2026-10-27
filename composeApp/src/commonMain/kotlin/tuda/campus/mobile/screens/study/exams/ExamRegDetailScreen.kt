package tuda.campus.mobile.screens.study.exams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tuda.campus.mobile.components.SmallNavTitle
import tuda.campus.mobile.managers.tucanManager.ExamRegistration
import tuda.campus.mobile.managers.TucanManager
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.cancellationPeriod
import tudex.composeapp.generated.resources.course
import tudex.composeapp.generated.resources.details
import tudex.composeapp.generated.resources.examType
import tudex.composeapp.generated.resources.examinationDate
import tudex.composeapp.generated.resources.examiners
import tudex.composeapp.generated.resources.registrationPeriod
import tudex.composeapp.generated.resources.rooms
import tudex.composeapp.generated.resources.semester

@Preview
@Composable
fun ExamRegDetailScreen(
    examRegistration: ExamRegistration,
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // This is a hacky solution, the real one would be to remove the mutability of ExamRegistration
    // Currently, in LaunchedEffect examRegistration is updated in-place meaning, compose doesn't know about the change.
    // To cause recomposition, "refresh" is updated (must be used somewhere, see the Text).
    // This also means, the screen doesn't react to external changes to examRegistration (which currently don't exist)
    // In SwiftUI, an equivalent (hacky) thing is done. For more details, see https://developer.android.com/develop/ui/compose/performance/stability#mutable-objects
    var refresh by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (examRegistration.registerPeriod.isEmpty() ||
            examRegistration.cancelPeriod.isEmpty() ||
            examRegistration.examiners.isEmpty())
        {
            TucanManager.updateExamRegistrationDetail(examRegistration)
            refresh = true
        }
    }

    Text("$refresh", modifier = Modifier.alpha(0f))

    SmallNavTitle(stringResource(Res.string.details), backAction = navigateBack) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
                optListItem(stringResource(Res.string.semester), examRegistration.semester)
                optListItem(stringResource(Res.string.course), examRegistration.courseID + " - " + examRegistration.courseName)
                optListItem(stringResource(Res.string.examType), examRegistration.examType)
            }

            AnimatedVisibility(
                visible = examRegistration.registerPeriod.isNotEmpty() || examRegistration.cancelPeriod.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp))
                ) {
                    optListItem(stringResource(Res.string.registrationPeriod), examRegistration.registerPeriod)
                    optListItem(stringResource(Res.string.cancellationPeriod), examRegistration.cancelPeriod)
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.clip(RoundedCornerShape(20.dp))
            ) {
                optListItem(stringResource(Res.string.examinationDate), examRegistration.examinationDate)
                optListItem(stringResource(Res.string.examiners), examRegistration.examiners)
                optListItem(stringResource(Res.string.rooms), examRegistration.room.joinToString(separator = ", "))
            }

            // There exists an attribute examReg.roomArgs which provides a link to Tucans Room Overview
        }
    }
}

@Composable
private fun optListItem(title: String, text: String) {
    AnimatedVisibility(
        visible = text.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        ListItem(
            headlineContent = { Text(text) },
            overlineContent = { Text(title) },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            modifier = Modifier.clip(RoundedCornerShape(4.dp))
        )
    }
}