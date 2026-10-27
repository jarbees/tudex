package tuda.campus.mobile.screens.study.exams

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import tuda.campus.mobile.ExamRegisterDetail
import tuda.campus.mobile.components.BigButton
import tuda.campus.mobile.components.ExpandableListSection
import tuda.campus.mobile.components.HeaderText
import tuda.campus.mobile.components.LoginLoadingScreen
import tuda.campus.mobile.components.RequestLoadingScreen
import tuda.campus.mobile.components.SmallNavTitle
import tuda.campus.mobile.managers.tucanManager.ExamRegistration
import tuda.campus.mobile.managers.IdentityManager
import tuda.campus.mobile.managers.tucanManager.RegistrationOption
import tuda.campus.mobile.managers.tucanManager.RequestStatus
import tuda.campus.mobile.managers.TULoginState
import tuda.campus.mobile.managers.TucanManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.cancel
import tudex.composeapp.generated.resources.confirmRegistration
import tudex.composeapp.generated.resources.confirmUnregistration
import tudex.composeapp.generated.resources.deselectAll
import tudex.composeapp.generated.resources.examModifyMsgMultiple
import tudex.composeapp.generated.resources.examModifyMsgSingle
import tudex.composeapp.generated.resources.examRegistration
import tudex.composeapp.generated.resources.examRegistrations
import tudex.composeapp.generated.resources.ic_add_24px
import tudex.composeapp.generated.resources.ic_arrow_back_24px
import tudex.composeapp.generated.resources.ic_remove_24px
import tudex.composeapp.generated.resources.register
import tudex.composeapp.generated.resources.register_actionWord
import tudex.composeapp.generated.resources.select
import tudex.composeapp.generated.resources.unregister
import tudex.composeapp.generated.resources.unregister_actionWord


@Preview
@Composable
private fun Preview() {
    val examRegistration = ExamRegistration(
        semester = "WiSe 25/26",
        courseID = "21-00-1234",
        courseName = "Introduction in Courses",
        examType = "Written and oral exam",
        examinationDate = "12.12.1234",
        registerOption = RegistrationOption.REGISTER,
        registerArgs = "",
        additionalInfoArgs = "",
        registerPeriod = "15.11.24 00:00 - 15.12.24 23:59",
        cancelPeriod = "15.11.24 00:00 - 04.03.26 23:59",
        examiners = "Real Person",
        room = listOf("S101/A1"),
        roomArgs = emptyList()
    )

    ExamRegisterScreen(
        navigateTo = {},
        navigateBack = {},
        updateNavigationBar = {},
        examRegistrations = listOf(listOf(listOf(
            examRegistration
        ),listOf(
            examRegistration
        ),listOf(
            examRegistration
        ),
            listOf(
                examRegistration,
                examRegistration
            )
        )),
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
        //val examRegistrations by TucanManager.examRegistrations.collectAsStateWithLifecycle()
    )
}

@Composable
fun ExamRegisterScreen(
    navigateTo: (NavKey) -> Unit,
    navigateBack: () -> Unit,
    updateNavigationBar: (isShown: Boolean) -> Unit,
    examRegistrations: List<List<List<ExamRegistration>>>,
    viewModel: ExamRegisterViewModel = ExamRegisterViewModel(),
    modifier: Modifier = Modifier
) {
    val tuLoginStatus by IdentityManager.tuLoginStatus.collectAsStateWithLifecycle()
    val examRegStatus by TucanManager.examRegStatus.collectAsStateWithLifecycle()

    val selection by viewModel.selection.collectAsStateWithLifecycle()
    val selectionMode by viewModel.selectionMode.collectAsStateWithLifecycle()
    val isSomethingLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            updateNavigationBar(true)
        }
    }

    SmallNavTitle(
        title = stringResource(Res.string.examRegistration),
        navigationIcon = {
            if (selectionMode != null) {
                TextButton(
                    enabled = selection.isNotEmpty(),
                    onClick = { viewModel.deselectAllExams() },
                    content = { Text(stringResource(Res.string.deselectAll)) }
                )
            } else {
                IconButton(onClick = navigateBack) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back_24px),
                        contentDescription = null
                    )
                }
            }
        },
        actions = {
            if (selectionMode == null) {
                TextButton(
                    onClick = {
                        viewModel.updateSelectionMode(true)
                        updateNavigationBar(false)
                    },
                    content = { Text(stringResource(Res.string.select)) }
                )
            } else {
                TextButton(
                    onClick = {
                        viewModel.updateSelectionMode(false)
                        updateNavigationBar(true)
                    },
                    content = { Text(stringResource(Res.string.cancel)) }
                )
            }
        }
    ) {
        if (tuLoginStatus == TULoginState.MFA_UNCONFIRMED) {
            LoginLoadingScreen()
        } else if (examRegStatus == RequestStatus.LOADING && examRegistrations.isEmpty()) {
            Column {
                RequestLoadingScreen(
                    itemName = stringResource(Res.string.examRegistrations),
                    modifier = modifier.weight(1f)
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = modifier
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    examRegistrations.forEach { moduleList ->
                        HeaderText(
                            text = moduleList.firstOrNull()?.firstOrNull()?.semester ?: "",
                            modifier = Modifier.padding(top = 16.dp)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                        ) {
                            moduleList.forEach { registrations ->
                                if (registrations.count() == 1) {
                                    val registration = registrations.first()
                                    ExamListItem(
                                        registration = registration,
                                        navigateTo = navigateTo,
                                        isSelected = selection.contains(registration),
                                        // SwiftUI: .contains(where: { $0.id == examReg.id })
                                        selectionMode = selectionMode,
                                        selectExam = {
                                            viewModel.selectExam(
                                                registration = registration,
                                                isSelected = selection.contains(registration)
                                            )
                                        },
                                        updateIsLoading = { viewModel.updateIsLoading(it) },
                                        isSomethingLoading = isSomethingLoading
                                    )
                                } else if (registrations.count() != 0) {
                                    ExpandableListSection(title = registrations.first().courseName) {
                                        registrations.forEach { registration ->
                                            ExamListItem(
                                                registration = registration,
                                                navigateTo = navigateTo,
                                                isSelected = selection.contains(registration),
                                                selectionMode = selectionMode,
                                                selectExam = {
                                                    viewModel.selectExam(
                                                        registration = registration,
                                                        isSelected = selection.contains(registration)
                                                    )
                                                },
                                                updateIsLoading = { viewModel.updateIsLoading(it) },
                                                isSomethingLoading = isSomethingLoading
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // When in Edit Mode, Navigation Bar disappears and with it bottom padding.
                    // This is supposed to make the List be scrollable with padding at its bottom
                    if (selectionMode != null) {
                        Spacer(modifier = Modifier.size(96.dp))
                    }
                }

                Column {
                    Spacer(modifier = Modifier.weight(1f))
                    ModifyButton(
                        selectionMode = selectionMode,
                        selection = selection,
                        updateIsLoading = { viewModel.updateIsLoading(it) },
                        isSomethingLoading = isSomethingLoading,
                        completeModify = { viewModel.completeModify() },
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.size(36.dp))
                }
            }
        }
    }
}

@Composable
private fun ExamListItem(
    registration: ExamRegistration,
    navigateTo: (NavKey) -> Unit,
    isSelected: Boolean,
    selectionMode: RegistrationOption?,
    selectExam: () -> Unit,
    updateIsLoading: (Boolean) -> Unit,
    isSomethingLoading: Boolean
) {
    ListItem(
        headlineContent = { Text(registration.courseName) },
        supportingContent = { Text(registration.examType) },
        leadingContent = {
            ItemButton(
                registration = registration,
                isSelected = isSelected,
                selectionMode = selectionMode,
                selectExam = selectExam,
                updateIsLoading = updateIsLoading,
                isSomethingLoading = isSomethingLoading
            )
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceBright
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                if (selectionMode != null) {
                    selectExam()
                } else {
                    navigateTo(ExamRegisterDetail(registration))
                }
            }
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ItemButton(
    registration: ExamRegistration,
    isSelected: Boolean,
    selectionMode: RegistrationOption?,
    selectExam: () -> Unit,
    updateIsLoading: (isLoading: Boolean) -> Unit,
    isSomethingLoading: Boolean
) {
    var showAlert by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var registerID by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val isRegister = (registration.registerOption == RegistrationOption.REGISTER)
    val isUnavailable = (registration.registerOption == RegistrationOption.UNAVAILABLE)

    fun getExamModifyPrompt(isRegister: Boolean) {
        scope.launch {
            isLoading = true
            //examRegHelper.isLoading = true
            updateIsLoading(true)

            registerID = TucanManager.getExamModifyPrompt(isRegister, registration.registerArgs)
                ?: return@launch

            if (registerID != "") {
                showAlert = true
            }

            //examRegHelper.isLoading = false
            isLoading = false
            updateIsLoading(false)
        }
    }

    fun confirmExamModify() {
        scope.launch {
            isLoading = true
            updateIsLoading(true)
            //viewModel.isLoading = true
            val success = TucanManager.confirmExamModify(registerID)

            if (success) {
                async { TucanManager.updateExams() }
                async { TucanManager.updateExamRegistrations() }
            }

            // Nothing happens for failure (?)

            isLoading = false
            updateIsLoading(false)
        }
    }

    if (isUnavailable) {
        Spacer(modifier = Modifier.size(36.dp))

    } else if (isLoading || (isSelected && isSomethingLoading)) {
        LoadingIndicator(modifier = Modifier.size(36.dp))

    } else if (selectionMode != null) {
        val enabled = selectionMode == registration.registerOption || selectionMode == RegistrationOption.UNAVAILABLE

        IconButton(
            onClick = { selectExam() },
            enabled = enabled,
            modifier = Modifier
                .alpha(if (enabled) 1f else 0.25f)
                .then(
                    if (isSelected) {
                        Modifier.background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                    } else {
                        Modifier.border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                    }
                )
                .size(36.dp)
        ) {
            Icon(
                painterResource(resource =
                    if (isRegister)
                        Res.drawable.ic_add_24px     // Plus
                    else
                        Res.drawable.ic_remove_24px      // minus
                ),
                contentDescription = null,
                tint =
                    if (isSelected)
                        MaterialTheme.colorScheme.surfaceBright
                    else
                        MaterialTheme.colorScheme.primary,
                modifier = Modifier
            )
        }
    } else {
        IconButton(
            onClick = { getExamModifyPrompt(isRegister) },
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                )
                .size(36.dp)
        ) {
            Icon(
                painterResource(
                    if (isRegister)
                        Res.drawable.ic_add_24px     // Plus
                    else
                        Res.drawable.ic_remove_24px      // minus
                ),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
            )
        }
    }

    if (showAlert) {
        AlertDialog(
            title = {
                Text(text =
                    if (isRegister)
                        stringResource(Res.string.confirmRegistration)
                    else
                        stringResource(Res.string.confirmUnregistration)
                )
            },
            text = {
                val actionWord = if (isRegister) stringResource(Res.string.register_actionWord) else stringResource(Res.string.unregister_actionWord)
                Text(
                    stringResource(
                        Res.string.examModifyMsgSingle,
                        actionWord,
                        registration.courseID,
                        registration.courseName,
                        registration.examType
                    )
                )
            },
            onDismissRequest = { showAlert = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAlert = false
                        confirmExamModify()
                    },
                    colors = ButtonDefaults.textButtonColors(),
                    content = { Text(if (isRegister) stringResource(Res.string.register) else stringResource(Res.string.unregister)) }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = { showAlert = false },
                    colors = ButtonDefaults.textButtonColors(),
                    content = { Text(stringResource(Res.string.cancel)) }
                )
            }
        )
    }
}

@Composable
private fun ModifyButton(
    selectionMode: RegistrationOption?,
    selection: List<ExamRegistration>,
    updateIsLoading: (isLoading: Boolean) -> Unit,
    isSomethingLoading: Boolean,
    completeModify: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAlert by rememberSaveable { mutableStateOf(false) }
    var alertExamsText by rememberSaveable { mutableStateOf("") }
    var modifyIDs by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()


    fun getExamModifyPrompts(isRegister: Boolean) {
        scope.launch {
            updateIsLoading(true)

            val newModifyIDs: MutableList<String> = mutableListOf()
            var newAlertExamsText = ""

            // This cannot be done concurrently as TUCaN would throw an ExecutionError
            // The error would be displayed as a webpage with the text "ExecutionError"
            for (registration in selection) {
                val modifyID = TucanManager.getExamModifyPrompt(
                    isRegister = isRegister,
                    arguments = registration.registerArgs
                ) ?: ""
                if (modifyID != "") {
                    newAlertExamsText = newAlertExamsText.plus("\n\n${registration.courseID} ${registration.courseName}\n${registration.examType}")
                    newModifyIDs.add(modifyID)
                } else {
                    print("Failure")
                }
            }

            modifyIDs = newModifyIDs
            alertExamsText = newAlertExamsText
            showAlert = true
            updateIsLoading(false)
        }
    }

    fun confirmExamModify() {
        scope.launch {
            updateIsLoading(true)

            // This might be possible concurrently
            for (modifyID in modifyIDs) {
                TucanManager.confirmExamModify(registerID = modifyID)
            }

            async { TucanManager.updateExams() }
            async {
                TucanManager.updateExamRegistrations()
                completeModify()
            }
        }
    }

    AnimatedVisibility(
        visible = selectionMode != null && selectionMode != RegistrationOption.UNAVAILABLE,
        enter = slideInVertically(initialOffsetY = { it/2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it/2 }) + fadeOut()
    ) {
        val isRegister = (selectionMode == RegistrationOption.REGISTER)

        Box(
            modifier = modifier
        ) {
            BigButton(
                title = if (isRegister) stringResource(Res.string.register) else stringResource(Res.string.unregister),
                enabled = !isSomethingLoading,
                onClick = { getExamModifyPrompts(isRegister) }
            )
        }

        if (showAlert) {
            AlertDialog(
                title = {
                    Text(text = if (isRegister) stringResource(Res.string.confirmRegistration) else stringResource(Res.string.unregister))
                },
                text = {
                    val actionWord = if (isRegister) stringResource(Res.string.register_actionWord) else stringResource(Res.string.unregister_actionWord)
                    Text(stringResource(Res.string.examModifyMsgMultiple, actionWord, alertExamsText))
                },
                onDismissRequest = { showAlert = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showAlert = false
                            confirmExamModify()
                        },
                        content = { Text(if (isRegister) stringResource(Res.string.register) else stringResource(Res.string.unregister)) }
                    )
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAlert = false },
                        content = { Text(stringResource(Res.string.cancel)) }
                    )
                }
            )
        }
    }
}