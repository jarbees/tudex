package tuda.campus.mobile.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.spring
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.TwoRowsTopAppBar
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.ic_arrow_back_24px
import kotlin.math.abs

@Composable
fun NoNavTitle(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        content()
    }
}

@Preview
@Composable
private fun PreviewMedium() {
    MediumNavTitle(
        "Testing",
        backAction = {}
    ) {
        Text("Test")
    }
}


@Composable
fun MediumNavTitle(
    title: String,
    backAction: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit
) {
    _MediumNavTitle(
        title = title,
        titleComp = null,
        titleCompVariable = null,
        backAction = backAction,
        navigationIcon = {},
        actions = actions,
        content = content
    )
}

@Composable
fun MediumNavTitle(
    titleComp: @Composable (Boolean) -> Unit,
    backAction: () -> Unit,
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit
) {
    _MediumNavTitle(
        titleCompVariable = titleComp,
        backAction = backAction,
        navigationIcon = {},
        actions = actions,
        content = content
    )
}

@Composable
fun MediumNavTitle(
    title: String,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit
) {
    _MediumNavTitle(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

@Composable
fun MediumNavTitle(
    titleComp: @Composable (Boolean) -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit
) {
    _MediumNavTitle(
        titleCompVariable = titleComp,
        navigationIcon = navigationIcon,
        actions = actions,
        content = content
    )
}

// Will only show navigationIcon if no backAction is provided
// Will only show titleComp if no title is provided
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun _MediumNavTitle(
    title: String? = null,
    titleComp: (@Composable () -> Unit)? = null,
    titleCompVariable: (@Composable (Boolean) -> Unit)? = null,
    backAction: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable (RowScope.() -> Unit) = {},
    content: @Composable () -> Unit
) {
    val scrollBehavior = pinnedExitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Column(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        TwoRowsTopAppBar(
            title = { expanded ->
                CompositionLocalProvider(LocalTextStyle provides
                        if (expanded)
                            MaterialTheme.typography.headlineSmall
                        else
                            MaterialTheme.typography.titleLarge
                ) {
                    Column(modifier = Modifier.padding(bottom = if (expanded) 16.dp else 0.dp)) {
                        if (title != null) {
                            Text(text = title)
                        } else if (titleComp != null) {
                            titleComp()
                        } else if (titleCompVariable != null) {
                            titleCompVariable(expanded)
                        }
                    }
                }
            },
            navigationIcon = {
                if (backAction != null) {
                    IconButton(onClick = backAction) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back_24px),
                            contentDescription = null
                        )
                    }
                } else {
                    navigationIcon()
                }
            },
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
        )
        /*
        MediumTopAppBar(
            title = {
                if (title != null) {
                    Text(text = title)
                } else if (titleComp != null) {
                    titleComp()
                } else if (titleCompVariable != null) {
                    titleCompVariable(scrollBehavior.state.collapsedFraction < 1f)
                }
            },
            navigationIcon = {
                if (backAction != null) {
                    IconButton(onClick = backAction) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back_24px),
                            contentDescription = null
                        )
                    }
                } else {
                    navigationIcon()
                }
            },
            actions = actions,
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            //collapsedHeight = TopAppBarDefaults.MediumAppBarCollapsedHeight

        )
         */

        content()
    }
}

@Preview
@Composable
private fun PreviewSmall() {
    SmallNavTitle(
        "Testing",
        backAction = {}
    ) {
        Text("Test")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmallNavTitle(
    title: String? = null,
    titleComp: (@Composable () -> Unit)? = null,
    backAction: (() -> Unit)? = null,
    navigationIcon: @Composable () -> Unit = { },
    actions: @Composable (RowScope.() -> Unit) = { },
    content: @Composable () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                if (title != null) {
                    Text(title)
                } else if (titleComp != null) {
                    titleComp()
                }
            },
            navigationIcon = {
                if (backAction != null) {
                    IconButton(onClick = backAction) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_arrow_back_24px),
                            contentDescription = null
                        )
                    }
                } else {
                    navigationIcon()
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )

        content()
    }
}

// The reasoning for using this is that the default LargeTopAppBar can be swiped to collapse/expand
// even if the content is not scrollable, which is an unusual default behavior

// Source - https://stackoverflow.com/a/78538564
// Posted by BenjyTec, modified by community. See post 'Timeline' for change history
// Retrieved 2025-12-23, License - CC BY-SA 4.0

@ExperimentalMaterial3Api
@Composable
fun pinnedExitUntilCollapsedScrollBehavior(
    state: TopAppBarState = rememberTopAppBarState(),
    canScroll: () -> Boolean = { true },
    snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    flingAnimationSpec: DecayAnimationSpec<Float>? = rememberSplineBasedDecay()
): TopAppBarScrollBehavior =
    PinnedExitUntilCollapsedScrollBehavior(
        state = state,
        snapAnimationSpec = snapAnimationSpec,
        flingAnimationSpec = flingAnimationSpec,
        canScroll = canScroll
    )

@OptIn(ExperimentalMaterial3Api::class)
private class PinnedExitUntilCollapsedScrollBehavior(
    override val state: TopAppBarState,
    override val snapAnimationSpec: AnimationSpec<Float>?,
    override val flingAnimationSpec: DecayAnimationSpec<Float>?,
    val canScroll: () -> Boolean = { true }
) : TopAppBarScrollBehavior {
    override val isPinned: Boolean = true
    override var nestedScrollConnection =
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Don't intercept if scrolling down.
                if (!canScroll() || available.y > 0f) return Offset.Zero

                val prevHeightOffset = state.heightOffset
                state.heightOffset += available.y
                return if (prevHeightOffset != state.heightOffset) {
                    // We're in the middle of top app bar collapse or expand.
                    // Consume only the scroll on the Y axis.
                    available.copy(x = 0f)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!canScroll()) return Offset.Zero
                state.contentOffset += consumed.y

                if (available.y < 0f || consumed.y < 0f) {
                    // When scrolling up, just update the state's height offset.
                    val oldHeightOffset = state.heightOffset
                    state.heightOffset += consumed.y
                    return Offset(0f, state.heightOffset - oldHeightOffset)
                }

                if (consumed.y == 0f && available.y > 0) {
                    // Reset the total content offset to zero when scrolling all the way down. This
                    // will eliminate some float precision inaccuracies.
                    state.contentOffset = 0f
                }

                if (available.y > 0f) {
                    // Adjust the height offset in case the consumed delta Y is less than what was
                    // recorded as available delta Y in the pre-scroll.
                    val oldHeightOffset = state.heightOffset
                    state.heightOffset += available.y
                    return Offset(0f, state.heightOffset - oldHeightOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val superConsumed = super.onPostFling(consumed, available)
                return superConsumed + settleAppBar(
                    state,
                    available.y,
                    flingAnimationSpec,
                    snapAnimationSpec
                )
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
private suspend fun settleAppBar(
    state: TopAppBarState,
    velocity: Float,
    flingAnimationSpec: DecayAnimationSpec<Float>?,
    snapAnimationSpec: AnimationSpec<Float>?
): Velocity {
    // Check if the app bar is completely collapsed/expanded. If so, no need to settle the app bar,
    // and just return Zero Velocity.
    // Note that we don't check for 0f due to float precision with the collapsedFraction
    // calculation.
    if (state.collapsedFraction < 0.01f || state.collapsedFraction == 1f) {
        return Velocity.Zero
    }
    var remainingVelocity = velocity
    // In case there is an initial velocity that was left after a previous user fling, animate to
    // continue the motion to expand or collapse the app bar.
    if (flingAnimationSpec != null && abs(velocity) > 1f) {
        var lastValue = 0f
        AnimationState(
            initialValue = 0f,
            initialVelocity = velocity,
        )
            .animateDecay(flingAnimationSpec) {
                val delta = value - lastValue
                val initialHeightOffset = state.heightOffset
                state.heightOffset = initialHeightOffset + delta
                val consumed = abs(initialHeightOffset - state.heightOffset)
                lastValue = value
                remainingVelocity = this.velocity
                // avoid rounding errors and stop if anything is unconsumed
                if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
            }
    }
    // Snap if animation specs were provided.
    if (snapAnimationSpec != null) {
        if (state.heightOffset < 0 &&
            state.heightOffset > state.heightOffsetLimit
        ) {
            AnimationState(initialValue = state.heightOffset).animateTo(
                if (state.collapsedFraction < 0.5f) {
                    0f
                } else {
                    state.heightOffsetLimit
                },
                animationSpec = snapAnimationSpec
            ) { state.heightOffset = value }
        }
    }

    return Velocity(0f, remainingVelocity)
}
