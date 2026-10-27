package tuda.campus.mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import tuda.campus.mobile.managers.tucanManager.Course
import tuda.campus.mobile.managers.tucanManager.EventDate
import tuda.campus.mobile.managers.tucanManager.Exam
import tuda.campus.mobile.managers.tucanManager.ExamRegistration
import tuda.campus.mobile.managers.Location
import tuda.campus.mobile.managers.MensaManager
import tuda.campus.mobile.managers.MenuItem
import tuda.campus.mobile.managers.Teaser
import tuda.campus.mobile.managers.TucanManager
import tuda.campus.mobile.screens.BookScreen
import tuda.campus.mobile.screens.FoodScreen
import tuda.campus.mobile.screens.FoodScreenViewModel
import tuda.campus.mobile.screens.MapScreen
import tuda.campus.mobile.screens.SettingsScreen
import tuda.campus.mobile.screens.StudyScreen
import tuda.campus.mobile.screens.food.CanteenLocationDetail
import tuda.campus.mobile.screens.food.FoodExtras
import tuda.campus.mobile.screens.food.FoodLocationsList
import tuda.campus.mobile.screens.food.HtmlTextView
import tuda.campus.mobile.screens.food.MenuItemDetailScreen
import tuda.campus.mobile.screens.food.TeaserDetailScreen
import tuda.campus.mobile.screens.settings.AllergiesScreen
import tuda.campus.mobile.screens.settings.FoodTypesPicker
import tuda.campus.mobile.screens.study.CourseScreen
import tuda.campus.mobile.screens.study.ExamScreen
import tuda.campus.mobile.screens.study.StudentIDScreen
import tuda.campus.mobile.screens.study.TimetableScreen
import tuda.campus.mobile.screens.study.TotpCodeScreen
import tuda.campus.mobile.screens.study.course.CourseDetailScreen
import tuda.campus.mobile.screens.study.exams.ExamDetailScreen
import tuda.campus.mobile.screens.study.exams.ExamRegDetailScreen
import tuda.campus.mobile.screens.study.exams.ExamRegisterScreen
import tuda.campus.mobile.screens.study.timetable.TimetableDetailScreen
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.booksTab
import tudex.composeapp.generated.resources.canteenTab
import tudex.composeapp.generated.resources.ic_book_2_24px
import tudex.composeapp.generated.resources.ic_book_2_fill_24px
import tudex.composeapp.generated.resources.ic_map_24px
import tudex.composeapp.generated.resources.ic_map_fill_24px
import tudex.composeapp.generated.resources.ic_menu_book_2_24px
import tudex.composeapp.generated.resources.ic_menu_book_2_fill_24px
import tudex.composeapp.generated.resources.ic_school_24px
import tudex.composeapp.generated.resources.ic_school_fill_24px
import tudex.composeapp.generated.resources.ic_settings_24px
import tudex.composeapp.generated.resources.ic_settings_fill_24px
import tudex.composeapp.generated.resources.mapTab
import tudex.composeapp.generated.resources.settingsTab
import tudex.composeapp.generated.resources.studyTab
import kotlin.math.roundToInt

// Study

@Serializable
data object Exams: NavKey

@Serializable
data class ExamDetail(@Contextual val exam: Exam): NavKey

@Serializable
data object ExamRegister: NavKey

@Serializable
data class ExamRegisterDetail(@Contextual val registration: ExamRegistration): NavKey


@Serializable
data object Courses: NavKey

@Serializable
data class CourseDetail(@Contextual val course: Course): NavKey

@Serializable
data object Timetable: NavKey

@Serializable
data class TimetableDetail(@Contextual val date: EventDate): NavKey


@Serializable
data object StudentCards: NavKey

@Serializable
data object TotpCodes: NavKey


// Canteen

@Serializable
data class CanteenTeaserDetail(@Contextual val viewModel: FoodScreenViewModel): NavKey

@Serializable
data class CanteenMenuItemDetail(@Contextual val menuItemID: String): NavKey

@Serializable
data object CanteenExtras: NavKey

@Serializable
data class HtmlText(@Contextual val title: String, @Contextual val html: String): NavKey

@Serializable
data object CanteenLocations: NavKey

@Serializable
data class CanteenLocationDetail(@Contextual val location: Location): NavKey

// Settings

@Serializable
data object FoodTypesPicker: NavKey

@Serializable
data object AllergiesPicker: NavKey

@Serializable
private sealed interface TopLevelRoute: NavKey {
    val iconID: DrawableResource
    val filledIconID: DrawableResource
    val title: StringResource
}


@Serializable
data object Study : TopLevelRoute {
    override val iconID = Res.drawable.ic_school_24px
    override val filledIconID = Res.drawable.ic_school_fill_24px
    override val title = Res.string.studyTab
}
@Serializable
data object Canteen : TopLevelRoute {
    override val iconID = Res.drawable.ic_menu_book_2_24px
    override val filledIconID = Res.drawable.ic_menu_book_2_fill_24px
    override val title = Res.string.canteenTab
}
@Serializable
data object Map : TopLevelRoute {
    override val iconID = Res.drawable.ic_map_24px
    override val filledIconID = Res.drawable.ic_map_fill_24px
    override val title = Res.string.mapTab
}
@Serializable
data object Books : TopLevelRoute {
    override val iconID = Res.drawable.ic_book_2_24px
    override val filledIconID = Res.drawable.ic_book_2_fill_24px
    override val title = Res.string.booksTab
}
@Serializable
data object Settings : TopLevelRoute {
    override val iconID = Res.drawable.ic_settings_24px
    override val filledIconID = Res.drawable.ic_settings_fill_24px
    override val title = Res.string.settingsTab
}

private val TABS : List<TopLevelRoute> = listOf(Study, Canteen, Map, Books, Settings)

private val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Study::class, Study.serializer())
            subclass(TotpCodes::class, TotpCodes.serializer())

            subclass(Exams::class, Exams.serializer())
            subclass(ExamDetail::class, ExamDetail.serializer())
            subclass(ExamRegister::class, ExamRegister.serializer())
            subclass(ExamRegisterDetail::class, ExamRegisterDetail.serializer())

            subclass(Courses::class, Courses.serializer())
            subclass(CourseDetail::class, CourseDetail.serializer())

            subclass(Timetable::class, Timetable.serializer())
            subclass(TimetableDetail::class, TimetableDetail.serializer())

            subclass(StudentCards::class, StudentCards.serializer())

            subclass(Canteen::class, Canteen.serializer())
            subclass(CanteenTeaserDetail::class, CanteenTeaserDetail.serializer())
            subclass(CanteenMenuItemDetail::class, CanteenMenuItemDetail.serializer())
            subclass(CanteenExtras::class, CanteenExtras.serializer())
            subclass(HtmlText::class, HtmlText.serializer())
            subclass(CanteenLocations::class, CanteenLocations.serializer())
            subclass(CanteenLocationDetail::class, CanteenLocationDetail.serializer())

            subclass(Map::class, Map.serializer())

            subclass(Books::class, Books.serializer())

            subclass(Settings::class, Settings.serializer())
            subclass(FoodTypesPicker::class, FoodTypesPicker.serializer())
            subclass(AllergiesPicker::class, AllergiesPicker.serializer())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalCoroutinesApi::class)
@Composable
fun App() {
    var selectedTab: TopLevelRoute by remember { mutableStateOf(Study) }

    val backStacks = TABS.associateWith { tab ->
        rememberNavBackStack(config, tab)
    }

    var isNavigationBarShown by rememberSaveable { mutableStateOf(true) }

    // This is used for syncing FoodTeaser ScrollPosition
    val foodScreenViewModel = remember { FoodScreenViewModel() }

    //val topLevelBackStack = remember { TopLevelBackStack<Any>(Study) }
    //val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = isNavigationBarShown,
                enter = slideInVertically(initialOffsetY = { it/2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it/2 }) + fadeOut()
            ) {
                NavigationBar {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = {
                                if (tab == selectedTab) {
                                    val backStack = backStacks[selectedTab] ?: return@NavigationBarItem
                                    backStack.clear()
                                    backStack.add(selectedTab)
                                } else {
                                    selectedTab = tab
                                }
                            },
                            label = { Text(stringResource(tab.title)) },
                            icon = {
                                Icon(
                                    painter = painterResource(
                                        if (tab == selectedTab) tab.filledIconID
                                        else tab.iconID
                                    ),
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { paddingValues ->
        val backStack = backStacks[selectedTab] ?: return@Scaffold
        val bottomPadding by animateDpAsState(if (isNavigationBarShown) paddingValues.calculateBottomPadding() else 0.dp)

        LaunchedEffect(selectedTab) {
            if (selectedTab == Canteen) {
                foodScreenViewModel.resetState()
            }
        }

        SharedTransitionLayout {
            NavDisplay(
                backStack = backStack,
                //onBack = { topLevelBackStack.removeLast() },
                entryProvider = entryProvider {
                    entry<Study> {
                        StudyScreen(navigateTo = { backStack.add(it) })
                    }
                    entry<TotpCodes> {
                        TotpCodeScreen(navigateBack = { backStack.removeLastOrNull() })
                    }

                    entry<Exams> {
                        ExamScreen(
                            navigateTo = { backStack.add(it) },
                            navigateBack = { backStack.removeLastOrNull() },
                        )
                    }
                    entry<ExamDetail> { key ->
                        ExamDetailScreen(
                            exam = key.exam,
                            navigateBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<ExamRegister> {
                        val examRegs by TucanManager.examRegistrations.collectAsStateWithLifecycle()
                        ExamRegisterScreen(
                            navigateTo = { backStack.add(it) },
                            navigateBack = { backStack.removeLastOrNull() },
                            updateNavigationBar = { isNavigationBarShown = it },
                            examRegistrations = examRegs
                        )
                    }
                    entry<ExamRegisterDetail> { key ->
                        ExamRegDetailScreen(
                            examRegistration = key.registration,
                            navigateBack = { backStack.removeLastOrNull() }
                        )
                    }

                    entry<Courses> {
                        CourseScreen(
                            backAction = { backStack.removeLastOrNull() },
                            navigateTo = { backStack.add(it) }
                        )
                    }
                    entry<CourseDetail> { key ->
                        CourseDetailScreen(
                            course = key.course,
                            backAction = { backStack.removeLastOrNull() }
                        )
                    }

                    entry<Timetable> {
                        TimetableScreen(
                            backAction = { backStack.removeLastOrNull() },
                            navigateTo = { backStack.add(it) }
                        )
                    }
                    entry<TimetableDetail> { key ->
                        TimetableDetailScreen(
                            date = key.date,
                            navigateBack = { backStack.removeLastOrNull() },
                            navigateTo = { backStack.add(it) }
                        )
                    }

                    entry<StudentCards> {
                        StudentIDScreen(
                            backAction = { backStack.removeLastOrNull() }
                        )
                    }

                    entry<Canteen> {
                        FoodScreen(
                            navigateTo = { backStack.add(it) },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            viewModel = foodScreenViewModel
                        )
                    }
                    entry<CanteenTeaserDetail> { entry ->
                        val teasers: List<Teaser> by MensaManager.teasers.collectAsStateWithLifecycle()
                        //val pagerState = rememberPagerState(pageCount = { teasers.size })

                        TeaserDetailScreen(
                            //pagerState = entry.pagerState,
                            teasers = teasers,
                            hideTeaserDetail = { backStack.removeLastOrNull() },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            viewModel = entry.viewModel,
                        )
                    }
                    entry<CanteenMenuItemDetail> { entry ->
                        val menuItems: List<MenuItem> by MensaManager.menuItems.collectAsStateWithLifecycle()
                        val menuItem = menuItems.find { entry.menuItemID == it.id }

                        if (menuItem == null) {
                            backStack.removeLastOrNull()
                        } else {
                            MenuItemDetailScreen(
                                menuItem = menuItem,
                                navigateBack = { backStack.removeLastOrNull() },
                                sharedTransitionScope = this@SharedTransitionLayout,
                            )
                        }
                    }
                    entry<CanteenExtras> {
                        FoodExtras(
                            backAction = { backStack.removeLastOrNull() },
                            navigateTo = { backStack.add(it) }
                        )
                    }
                    entry<HtmlText> { entry ->
                        HtmlTextView(
                            navigateBack = { backStack.removeLastOrNull() },
                            title = entry.title,
                            html = entry.html
                        )
                    }
                    entry<CanteenLocations> {
                        FoodLocationsList(
                            navigateBack = { backStack.removeLastOrNull() },
                            navigateTo = { backStack.add(it) }
                        )
                    }
                    entry<CanteenLocationDetail> { entry ->
                        CanteenLocationDetail(
                            location = entry.location,
                            navigateBack = { backStack.removeLastOrNull() }
                        )
                    }


                    entry<Map> {
                        MapScreen()
                    }
                    entry<Books> {
                        BookScreen()
                    }
                    entry<Settings> {
                        SettingsScreen(
                            navigateTo = { backStack.add(it) }
                        )
                    }
                    entry<FoodTypesPicker> {
                        FoodTypesPicker(
                            navigateBack = { backStack.removeLastOrNull() }
                        )
                    }
                    entry<AllergiesPicker> {
                        AllergiesScreen(
                            navigateBack = { backStack.removeLastOrNull() }
                        )
                    }
                },
                transitionSpec = { nativeTransition() },
                popTransitionSpec = { nativeTransition(true) },
                predictivePopTransitionSpec = { nativeTransition(true) },
                modifier = Modifier.padding(bottom = bottomPadding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun nativeTransition(reverse: Boolean = false): ContentTransform {
    val r = if (reverse) -1 else 1

    return ContentTransform(
        targetContentEnter =
        slideInHorizontally(
            initialOffsetX = { fullWidth -> (r * 0.2f * fullWidth).roundToInt() },
            //animationSpec = tween(durationMillis = 200)
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(
            //animationSpec = tween(durationMillis = 200, delayMillis = 50),
            animationSpec = tween(durationMillis = 200, delayMillis = 75),
            initialAlpha = 0f
        ),
        initialContentExit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> (r * -0.2f * fullWidth).roundToInt() },
            //animationSpec = tween(durationMillis = 200)
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(
            //animationSpec = tween(durationMillis = 200, delayMillis = 50)
            animationSpec = tween(durationMillis = 200, delayMillis = 75)
        )
    )
}