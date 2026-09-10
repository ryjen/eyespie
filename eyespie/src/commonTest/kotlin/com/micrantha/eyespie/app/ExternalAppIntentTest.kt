package com.micrantha.eyespie.app

import com.micrantha.eyespie.core.GameId
import com.micrantha.eyespie.core.PlayerId
import com.micrantha.eyespie.core.PlayerIdentity
import com.micrantha.eyespie.features.home.GameImportCanceller
import com.micrantha.eyespie.game.GameSnapshotLoader
import com.micrantha.eyespie.game.LocalGameFailure
import com.micrantha.eyespie.game.LocalGameFailureCode
import com.micrantha.eyespie.game.LocalGameResult
import com.micrantha.eyespie.game.LocalGameSnapshot
import com.micrantha.eyespie.game.LocalGameSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class ExternalAppIntentTest {
    @Test
    fun parser_accepts_only_one_bounded_local_game_segment() {
        val parsed = parseEyespieDeepLink(
            scheme = "eyespie",
            host = "game",
            pathSegments = listOf("game:1234-abcd"),
        )

        assertEquals(
            ExternalAppIntent.OpenLocalGame(GameId("game:1234-abcd")),
            parsed,
        )
        assertNull(parseEyespieDeepLink("https", "game", listOf("game:1234")))
        assertNull(parseEyespieDeepLink("eyespie", "import", listOf("game:1234")))
        assertNull(parseEyespieDeepLink("eyespie", "game", listOf("one", "two")))
        assertNull(parseEyespieDeepLink("eyespie", "game", listOf("game:bad?query")))
    }

    @Test
    fun existing_local_game_is_opened_from_home_without_import_authority() = runTest {
        val gameId = GameId("game:local")
        val navigation = RecordingNavigation()
        val imports = RecordingImportCanceller()
        val handler = ExternalAppIntentHandler(
            snapshotLoader = FixedSnapshotLoader(snapshot(gameId)),
            navigation = navigation,
            importCanceller = imports,
        )

        val result = handler.handle(ExternalAppIntent.OpenLocalGame(gameId))

        assertIs<ExternalAppIntentResult.Opened>(result)
        assertEquals(1, imports.cancels)
        assertEquals(
            listOf(
                NavigationCall.ReplaceAll(AppRoute.Home),
                NavigationCall.Push(AppRoute.GameDetail(gameId)),
            ),
            navigation.calls,
        )
    }

    @Test
    fun unknown_game_does_not_navigate_or_create_authority() = runTest {
        val navigation = RecordingNavigation()
        val imports = RecordingImportCanceller()
        val handler = ExternalAppIntentHandler(
            snapshotLoader = FixedSnapshotLoader(snapshot()),
            navigation = navigation,
            importCanceller = imports,
        )

        val result = handler.handle(ExternalAppIntent.OpenLocalGame(GameId("game:missing")))

        assertIs<ExternalAppIntentResult.NotFound>(result)
        assertEquals(0, imports.cancels)
        assertEquals(emptyList(), navigation.calls)
    }

    @Test
    fun failed_local_snapshot_lookup_does_not_navigate() = runTest {
        val navigation = RecordingNavigation()
        val handler = ExternalAppIntentHandler(
            snapshotLoader = object : GameSnapshotLoader {
                override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
                    LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.PERSISTENCE_FAILED))
            },
            navigation = navigation,
            importCanceller = RecordingImportCanceller(),
        )

        val result = handler.handle(ExternalAppIntent.OpenLocalGame(GameId("game:any")))

        assertIs<ExternalAppIntentResult.Failed>(result)
        assertEquals(emptyList(), navigation.calls)
    }
}

private fun snapshot(vararg gameIds: GameId): LocalGameSnapshot = LocalGameSnapshot(
    identity = PlayerIdentity(PlayerId("player-local"), "Agent"),
    games = gameIds.map { gameId ->
        LocalGameSummary(
            id = gameId,
            name = "Local game",
            things = emptyList(),
            localCreator = true,
        )
    },
)

private class FixedSnapshotLoader(
    private val snapshot: LocalGameSnapshot,
) : GameSnapshotLoader {
    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
        LocalGameResult.Success(snapshot)
}

private class RecordingImportCanceller : GameImportCanceller {
    var cancels = 0

    override fun cancelImport() {
        cancels += 1
    }
}

private sealed interface NavigationCall {
    data class Push(val route: AppRoute) : NavigationCall
    data class ReplaceAll(val route: AppRoute) : NavigationCall
}

private class RecordingNavigation : AppNavigation {
    val calls = mutableListOf<NavigationCall>()

    override fun push(route: AppRoute) {
        calls += NavigationCall.Push(route)
    }

    override fun replace(route: AppRoute) = Unit

    override fun replaceAll(route: AppRoute) {
        calls += NavigationCall.ReplaceAll(route)
    }

    override fun pop() = Unit
}
