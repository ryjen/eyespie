package com.micrantha.eyespie.features.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeContent
import com.micrantha.eyespie.features.home.HomeImportPreparationResult
import com.micrantha.eyespie.features.home.HomeImportResult
import com.micrantha.eyespie.features.home.HomeLoader
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.features.play.PlayGameContent
import com.micrantha.eyespie.features.play.PlayGameLoader
import com.micrantha.eyespie.features.utility.UtilityContent
import com.micrantha.eyespie.features.utility.UtilityLoader
import com.micrantha.eyespie.game.AuthoredThing
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.imaging.CapturedImage

internal object AppTestCapabilities :
    HomeLoader,
    GameImportPreparer,
    GameImportConfirmer,
    GameImportCanceller,
    UtilityLoader,
    GameCreator,
    ClueAuthor,
    PlayGameLoader,
    GuessSubmitter {
    override suspend fun load(): LocalGameResult<HomeContent> =
        LocalGameResult.Success(HomeContent("Agent", "player-1", emptyList()))

    override suspend fun prepareImport(): HomeImportPreparationResult =
        HomeImportPreparationResult.Terminal(HomeImportResult.Unavailable)

    override suspend fun confirmImport(): HomeImportResult = HomeImportResult.Unavailable

    override fun cancelImport() = Unit

    override suspend fun loadUtility(): LocalGameResult<UtilityContent> =
        LocalGameResult.Success(UtilityContent("Agent", "player-1"))

    override suspend fun create(
        name: String,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<CreatedGame> = error("not used")

    override suspend fun addClue(
        gameId: GameId,
        clueText: String,
        expectedAnswer: String,
        targetImage: CapturedImage,
    ): LocalGameResult<AuthoredThing> = error("not used")

    override suspend fun load(gameId: GameId, thingId: ThingId): LocalGameResult<PlayGameContent> =
        LocalGameResult.Success(PlayGameContent("Trip", "Find it", false, null))

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = error("not used")
}
