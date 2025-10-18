package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.core.ui.Screen
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingAction
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingPage
import com.micrantha.eyespie.features.onboarding.domain.entities.OnboardingUiState
import com.micrantha.eyespie.features.onboarding.ui.components.ClickableAnimatedPagerIndicator
import com.micrantha.eyespie.features.onboarding.ui.components.RenderGenAI
import com.micrantha.eyespie.features.onboarding.ui.components.RenderWelcome
import kotlinx.coroutines.delay

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
}
