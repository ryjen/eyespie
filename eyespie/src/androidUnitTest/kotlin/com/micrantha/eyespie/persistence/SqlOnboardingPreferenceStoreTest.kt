package com.micrantha.eyespie.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.micrantha.eyespie.data.EyesPieDatabase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SqlOnboardingPreferenceStoreTest {
    @Test
    fun completion_round_trips_through_sql_store() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            EyesPieDatabase.Schema.create(driver)
            val store = SqlOnboardingPreferenceStore(EyesPieDatabase(driver))

            assertFalse(store.isCompleted())
            store.markCompleted()
            assertTrue(store.isCompleted())
        } finally {
            driver.close()
        }
    }

    @Test
    fun migration_two_adds_preference_table_without_existing_app_state() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        try {
            driver.execute(
                identifier = null,
                sql = "CREATE TABLE GameEntity (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, creator_id TEXT NOT NULL)",
                parameters = 0,
                binders = null,
            )
            driver.execute(
                identifier = null,
                sql = "CREATE TABLE ThingEntity (id TEXT NOT NULL PRIMARY KEY, game_id TEXT NOT NULL, clue TEXT NOT NULL, target_embedding BLOB NOT NULL, match_threshold REAL NOT NULL, sort_order INTEGER NOT NULL, clue_expected_answer TEXT, clue_origin TEXT NOT NULL DEFAULT 'LEGACY', clue_authority_version INTEGER NOT NULL DEFAULT 1, generated_provider_id TEXT, generated_model_id TEXT, generated_confidence REAL)",
                parameters = 0,
                binders = null,
            )
            driver.execute(
                identifier = null,
                sql = "CREATE TABLE ThingProgressEntity (game_id TEXT NOT NULL, thing_id TEXT NOT NULL, player_id TEXT NOT NULL, matched INTEGER NOT NULL, best_similarity REAL, PRIMARY KEY (game_id, thing_id, player_id))",
                parameters = 0,
                binders = null,
            )

            EyesPieDatabase.Schema.migrate(driver, 2L, 3L)
            val store = SqlOnboardingPreferenceStore(EyesPieDatabase(driver))

            assertFalse(store.isCompleted())
            store.markCompleted()
            assertTrue(store.isCompleted())
        } finally {
            driver.close()
        }
    }
}
