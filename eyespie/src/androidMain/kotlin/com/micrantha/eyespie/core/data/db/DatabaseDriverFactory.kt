package com.micrantha.eyespie.core.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.micrantha.eyespie.data.EyesPieDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(EyesPieDatabase.Schema, context, "eyespie.db")
    }
}
