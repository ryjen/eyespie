package com.micrantha.eyespie.features.onboarding.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import cafe.adriel.voyager.kodein.rememberScreenModel
import com.micrantha.bluebell.arch.Dispatch
import com.micrantha.bluebell.ui.components.StateRenderer
import com.micrantha.bluebell.ui.theme.Dimensions
import com.micrantha.eyespie.app.S
import com.micrantha.eyespie.core.ui.Screen
import com.micrantha.eyespie.features.onboarding.ui.components.ClickableAnimatedPagerIndicator
import eyespie.composeapp.generated.resources.no
import eyespie.composeapp.generated.resources.onboarding_genai_text
import eyespie.composeapp.generated.resources.onboarding_genai_title
import eyespie.composeapp.generated.resources.yes
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

    enum class OnboardingPage {
        GenAI
    }

    @Composable
    override fun Render(
        state: OnboardingUiState,
        dispatch: Dispatch
    ) {
        val pagerState = rememberPagerState(
            initialPage = 0,
            pageCount = { OnboardingPage.entries.size }
        )

        HorizontalPager(
            state = pagerState,
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when (it) {
                    OnboardingPage.GenAI.ordinal -> RenderGenAI(state, dispatch)
                }
                ClickableAnimatedPagerIndicator(
                    pagerState = pagerState,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(Dimensions.screen),
                )
            }
        }
    }

    @Composable
    private fun BoxScope.RenderGenAI(state: OnboardingUiState, dispatch: Dispatch) {

        Text(
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = Dimensions.Padding.large),
            text = stringResource(S.onboarding_genai_title)
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
        ) {
            Text(
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
                    .padding(Dimensions.Padding.large),
                text = stringResource(S.onboarding_genai_text)
            )

            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(Dimensions.Padding.large),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton({
                    dispatch(OnboardingAction.Next)
                }) {
                    Text(stringResource(S.no))
                }
                OutlinedButton({ dispatch(OnboardingAction.Download) }) {
                    Text(stringResource(S.yes))
                }
            }
        }
    }
}
