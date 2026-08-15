package com.micrantha.eyespie.persistence

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.micrantha.eyespie.data.EyesPieDatabase

class AndroidEyespieDatabaseFactory(
    private val context: Context,
) : EyespieDatabaseFactory {
    override fun create(): EyesPieDatabase = EyesPieDatabase(
        AndroidSqliteDriver(
            EyesPieDatabase.Schema,
            context,
            DATABASE_NAME,
        ),
    )

    private companion object {
        const val DATABASE_NAME = "eyespie.db"
    }
}
