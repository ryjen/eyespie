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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ExternalAppIntentTest {
    @Test
    fun encoded_path_shape_rejects_platform_normalization_ambiguity() {
        assertTrue(hasCanonicalEyespieDeepLinkPath("/game:1234"))
        assertFalse(hasCanonicalEyespieDeepLinkPath(null))
        assertFalse(hasCanonicalEyespieDeepLinkPath(""))
        assertFalse(hasCanonicalEyespieDeepLinkPath("/"))
        assertFalse(hasCanonicalEyespieDeepLinkPath("game:1234"))
        assertFalse(hasCanonicalEyespieDeepLinkPath("//game:1234"))
        assertFalse(hasCanonicalEyespieDeepLinkPath("/game:1234/"))
        assertFalse(hasCanonicalEyespieDeepLinkPath("/one/two"))
    }

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
        val navigation = ExternalIntentRecordingNavigation()
        val imports = ExternalIntentRecordingImportCanceller()
        val handler = ExternalAppIntentHandler(
            snapshotLoader = ExternalIntentFixedSnapshotLoader(snapshot(listOf(gameId))),
            navigation = navigation,
            importCanceller = imports,
        )

        val result = handler.handle(ExternalAppIntent.OpenLocalGame(gameId))

        assertIs<ExternalAppIntentResult.Opened>(result)
        assertEquals(1, imports.cancels)
        assertEquals(
            listOf(
                ExternalIntentNavigationCall.ReplaceAll(AppRoute.Home),
                ExternalIntentNavigationCall.Push(AppRoute.GameDetail(gameId)),
            ),
            navigation.calls,
        )
    }

    @Test
    fun unknown_game_does_not_navigate_or_create_authority() = runTest {
        val navigation = ExternalIntentRecordingNavigation()
        val imports = ExternalIntentRecordingImportCanceller()
        val handler = ExternalAppIntentHandler(
            snapshotLoader = ExternalIntentFixedSnapshotLoader(snapshot()),
            navigation = navigation,
            importCanceller = imports,
        )

        val result = handler.handle(ExternalAppIntent.OpenLocalGame(GameId("game:missing")))

        assertIs<ExternalAppIntentResult.NotFound>(result)
        assertEquals(0, imports.cancels)
        assertEquals(emptyList<ExternalIntentNavigationCall>(), navigation.calls)
    }

    @Test
    fun failed_local_snapshot_lookup_does_not_navigate() = runTest {
        val navigation = ExternalIntentRecordingNavigation()
        val handler = ExternalAppIntentHandler(
            snapshotLoader = object : GameSnapshotLoader {
                override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
                    LocalGameResult.Failure(LocalGameFailure(LocalGameFailureCode.PERSISTENCE_FAILED))
            },
            navigation = navigation,
            importCanceller = ExternalIntentRecordingImportCanceller(),
        )

        val result = handler.handle(ExternalAppIntent.OpenLocalGame(GameId("game:any")))

        assertIs<ExternalAppIntentResult.Failed>(result)
        assertEquals(emptyList<ExternalIntentNavigationCall>(), navigation.calls)
    }
}

private fun snapshot(gameIds: List<GameId> = emptyList()): LocalGameSnapshot = LocalGameSnapshot(
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

private class ExternalIntentFixedSnapshotLoader(
    private val snapshot: LocalGameSnapshot,
) : GameSnapshotLoader {
    override suspend fun loadSnapshot(): LocalGameResult<LocalGameSnapshot> =
        LocalGameResult.Success(snapshot)
}

private class ExternalIntentRecordingImportCanceller : GameImportCanceller {
    var cancels = 0

    override fun cancelImport() {
        cancels += 1
    }
}

private sealed interface ExternalIntentNavigationCall {
    data class Push(val route: AppRoute) : ExternalIntentNavigationCall
    data class ReplaceAll(val route: AppRoute) : ExternalIntentNavigationCall
}

private class ExternalIntentRecordingNavigation : AppNavigation {
    val calls = mutableListOf<ExternalIntentNavigationCall>()

    override fun push(route: AppRoute) {
        calls += ExternalIntentNavigationCall.Push(route)
    }

    override fun replace(route: AppRoute) = Unit

    override fun replaceAll(route: AppRoute) {
        calls += ExternalIntentNavigationCall.ReplaceAll(route)
    }

    override fun pop() = Unit
}
