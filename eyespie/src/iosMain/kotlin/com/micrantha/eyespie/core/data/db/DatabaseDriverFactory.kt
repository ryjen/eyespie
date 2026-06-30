package com.micrantha.eyespie.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.micrantha.eyespie.data.EyesPieDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(EyesPieDatabase.Schema, "eyespie.db")
    }
}
