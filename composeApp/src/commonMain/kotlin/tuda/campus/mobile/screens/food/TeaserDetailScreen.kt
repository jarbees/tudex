package tuda.campus.mobile.screens.food

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import tuda.campus.mobile.components.NoNavTitle
import tuda.campus.mobile.managers.Teaser
import tuda.campus.mobile.screens.FoodScreenViewModel
import org.jetbrains.compose.resources.stringResource
import tudex.composeapp.generated.resources.Res
import tudex.composeapp.generated.resources.noImageFound

@Composable
fun TeaserDetailScreen(
    //pagerState: PagerState,
    teasers: List<Teaser>,
    hideTeaserDetail: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    viewModel: FoodScreenViewModel
) {
    val pagerState = viewModel.pagerState ?: rememberPagerState(pageCount = { teasers.size }).also {
        viewModel.pagerState = it
    }

    NoNavTitle {
        HorizontalPager(pagerState) { pageOffset ->
            val teaser = teasers[pageOffset]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    //.clickable { screenContent = ScreenContent.MAIN }
                    .clickable { hideTeaserDetail() }
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    teaser.image.platformImage?.bytes?.decodeToImageBitmap()
                        ?.let { bitmap ->
                            with(sharedTransitionScope) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = null,
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier
                                        .sharedElement(
                                            sharedContentState = rememberSharedContentState(key = "teaserImage"),
                                            animatedVisibilityScope = LocalNavAnimatedContentScope.current
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .fillMaxWidth()
                                )
                            }
                        } ?: run {
                        Text(stringResource(Res.string.noImageFound))
                    }
                }

                teaser.detailTitle?.let { detailTitle ->
                    Text(
                        text = detailTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                if (teaser.infoTitle != null || teaser.infoText != null) {
                    Column(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(8.dp)
                    ) {
                        teaser.infoTitle?.let { detailText ->
                            Text(
                                text = detailText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        teaser.infoText?.let { infoText ->
                            Text(
                                text = infoText,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}