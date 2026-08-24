package com.micrantha.eyespie.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.ThingId
import com.micrantha.eyespie.features.clueauthoring.ClueAuthor
import com.micrantha.eyespie.features.create.GameCreator
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.features.home.GameImportConfirmer
import com.micrantha.eyespie.features.home.GameImportPreparer
import com.micrantha.eyespie.features.home.HomeImportPreparationResult
import com.micrantha.eyespie.features.home.HomeImportResult
import com.micrantha.eyespie.features.play.GuessSubmitter
import com.micrantha.eyespie.game.AuthoredThing
import com.micrantha.eyespie.game.CreatedGame
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.GuessOutcome
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.testsupport.testGameSnapshot

internal object AppTestCapabilities :
    GameSnapshotLoader,
    GameImportPreparer,
    GameImportConfirmer,
    GameImportCanceller,
    GameCreator,
    ClueAuthor,
    GuessSubmitter {
    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
        LocalGameResult.Success(testGameSnapshot())

    override suspend fun prepareImport(): HomeImportPreparationResult =
        HomeImportPreparationResult.Terminal(HomeImportResult.Unavailable)

    override suspend fun confirmImport(): HomeImportResult = HomeImportResult.Unavailable

    override fun cancelImport() = Unit

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

    override suspend fun guess(
        gameId: GameId,
        thingId: ThingId,
        guessImage: CapturedImage,
    ): LocalGameResult<GuessOutcome> = error("not used")
}
