package com.micrantha.eyespie.persistence

import com.micrantha.eyespie.data.EyesPieDatabase

interface EyespieDatabaseFactory {
    fun create(): EyesPieDatabase
}
