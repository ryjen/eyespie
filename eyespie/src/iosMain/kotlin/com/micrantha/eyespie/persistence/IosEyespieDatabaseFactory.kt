package com.micrantha.eyespie.persistence

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.micrantha.eyespie.data.EyesPieDatabase

class IosEyespieDatabaseFactory : EyespieDatabaseFactory {
    override fun create(): EyesPieDatabase = EyesPieDatabase(
        NativeSqliteDriver(
            EyesPieDatabase.Schema,
            DATABASE_NAME,
        ),
    )

    private companion object {
        const val DATABASE_NAME = "eyespie.db"
    }
}
