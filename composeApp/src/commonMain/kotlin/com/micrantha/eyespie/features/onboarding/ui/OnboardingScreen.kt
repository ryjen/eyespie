package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.EyesPie
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.Screen
import com.micrantha.eyespie.features.onboarding.ui.components.ClickableAnimatedPagerIndicator
import eyespie.composeapp.generated.resources.download_failed
import eyespie.composeapp.generated.resources.downloading
import eyespie.composeapp.generated.resources.no
import eyespie.composeapp.generated.resources.onboarding_genai_text
import eyespie.composeapp.generated.resources.onboarding_genai_title
import eyespie.composeapp.generated.resources.yes
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

// TODO: wizard like setup for first run
// A) How to Play
//   1) Scanning
//   2) Sharing
//   3) Guessing
// B) Permissions Info
//   1) Camera for scanning
//   2) Notifications for game events and downloads
//   3) Storage for GenAI
//   4) Contacts for sharing
// C) GenAI
//   1) prompt user to download genAI models
//   2) start background downloads
//   3) notify user progress and completion
// D) Social
//   1) Invitations / contacts
//   2) Link social media share
//   3) More info in settings
class OnboardingScreen : Screen, StateRenderer<OnboardingUiState> {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel<OnboardingScreenModel>()

        val state by screenModel.state.collectAsState()

        Render(state, screenModel)
    }

    @Composable
    override fun Render(
        state: OnboardingUiState,
        dispatch: Dispatch
    ) {
        val pagerState = rememberPagerState(
            initialPage = state.page.ordinal,
            pageCount = { OnboardingPage.entries.size }
        )

        LaunchedEffect(Unit) {
            dispatch(OnboardingAction.Init)
        }

        LaunchedEffect(pagerState.currentPage) {
            delay(300)
            dispatch(OnboardingAction.PageChanged(pagerState.currentPage))
        }

        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            HorizontalPager(
                modifier = Modifier.weight(1f).fillMaxSize(),
                state = pagerState,
            ) { pageNum ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (pageNum) {
                        OnboardingPage.GenAI.ordinal -> RenderGenAI(state, dispatch)
                        OnboardingPage.Welcome.ordinal -> RenderWelcome(state, dispatch)
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(Dimensions.screen).fillMaxWidth()
            ) {
                ClickableAnimatedPagerIndicator(
                    pagerState = pagerState
                )
            }
        }
    }

    @Composable
    private fun OnboardingPage.title() = when (this) {
        OnboardingPage.Welcome -> null
        OnboardingPage.GenAI -> stringResource(S.onboarding_genai_title)
    }

    @Composable
    private fun BoxScope.RenderWelcome(state: OnboardingUiState, dispatch: Dispatch) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.Center)
                .padding(Dimensions.Padding.large),
        ) {
            Text(
                text = "Welcome to Eyespie",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Icon(
                imageVector = EyesPie.defaultIcon,
                modifier = Modifier.size(Dimensions.List.placeholder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    @Composable
    private fun BoxScope.RenderGenAI(state: OnboardingUiState, dispatch: Dispatch) {
        Text(
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(Dimensions.screen)
                .padding(top = Dimensions.Padding.large),
            text = stringResource(S.onboarding_genai_title)
        )
        Column(
            modifier = Modifier.align(Alignment.Center).padding(top = Dimensions.Padding.large),
        ) {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .padding(Dimensions.Padding.large),
                text = stringResource(S.onboarding_genai_text)
            )

            if (state.isBusy) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .sizeIn(48.dp)
                )
            } else {
                Spacer(Modifier.heightIn(48.dp))
            }

            if (state.isError) {
                Text(
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                        .padding(Dimensions.Padding.large),
                    text = stringResource(S.download_failed)
                )
            } else if (state.isBusy) {
                Text(
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                        .padding(Dimensions.Padding.large),
                    text = stringResource(S.downloading)
                )
            } else {
                Spacer(Modifier.heightIn(with(LocalDensity.current) { 12.sp.toDp() }))
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(Dimensions.Padding.large),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    enabled = state.isBusy.not(),
                    onClick = {
                        dispatch(OnboardingAction.NextPage)
                    }) {
                    Text(stringResource(S.no))
                }
                OutlinedButton(
                    enabled = state.isBusy.not(),
                    onClick = { dispatch(OnboardingAction.StartGenAI) }) {
                    Text(stringResource(S.yes))
                }
            }
        }
    }
}
