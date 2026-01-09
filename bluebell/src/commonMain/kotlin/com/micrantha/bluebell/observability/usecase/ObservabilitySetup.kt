package com.micrantha.bluebell.observability.usecase

import com.micrantha.bluebell.observability.domain.ObservabilityRepository
import com.micrantha.bluebell.observability.domain.SchemaMigration
import com.micrantha.bluebell.observability.entity.CacheConfig
import com.micrantha.bluebell.observability.entity.Destination
import com.micrantha.bluebell.observability.entity.EventSchema
import com.micrantha.bluebell.observability.entity.FirebaseConfig
import com.micrantha.bluebell.observability.entity.NewRelicConfig
import com.micrantha.bluebell.observability.entity.ObservabilityConfig
import com.micrantha.bluebell.observability.entity.PropertyType
import com.micrantha.bluebell.observability.entity.SchemaVersion
import com.micrantha.bluebell.observability.entity.TelemetryEvent
import com.micrantha.bluebell.observability.repository.InMemoryRetryQueue
import com.micrantha.bluebell.observability.repository.InMemorySchemaRegistry
import com.micrantha.bluebell.observability.repository.MemoryCache
import com.micrantha.bluebell.observability.repository.MultiTierEventCache
import com.micrantha.bluebell.observability.repository.ObservabilityDataRepository
import com.micrantha.bluebell.observability.repository.SQLiteEventCache
import com.micrantha.bluebell.observability.repository.destination.FirebaseDestination
import com.micrantha.bluebell.observability.repository.destination.LocalDatabaseDestination
import com.micrantha.bluebell.observability.repository.destination.NewRelicClient
import com.micrantha.bluebell.observability.repository.destination.NewRelicDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking

// Setting up the observability system
class ObservabilitySetup(
    private val database: Any,
    private val newRelicClient: NewRelicClient,
    private val firebaseAnalytics: Any
) {
    operator fun invoke(): ObservabilityRepository {
        // 1. Setup Schema Registry
        val schemaRegistry = InMemorySchemaRegistry()

        // Register schemas
        runBlocking {
            schemaRegistry.register(
                EventSchema(
                    name = "user_action",
                    version = 1,
                    properties = mapOf(
                        "action" to PropertyType.Enum(setOf("login", "logout", "signup")),
                        "user_id" to PropertyType.String,
                        "timestamp" to PropertyType.Number
                    ),
                    required = setOf("action", "user_id"),
                    description = "User authentication actions"
                )
            )

            // Register migration for version 2
            schemaRegistry.register(
                EventSchema(
                    name = "user_action",
                    version = 2,
                    properties = mapOf(
                        "action_type" to PropertyType.Enum(
                            setOf(
                                "login",
                                "logout",
                                "signup"
                            )
                        ),  // renamed
                        "user_id" to PropertyType.String,
                        "timestamp" to PropertyType.Number,
                        "ip_address" to PropertyType.String  // new field
                    ),
                    required = setOf("action_type", "user_id"),
                    deprecatedFields = setOf("action"),
                    fieldReplacements = mapOf("action" to "action_type")
                )
            )

            schemaRegistry.registerMigration(
                from = SchemaVersion("user_action", 1),
                to = SchemaVersion("user_action", 2),
                migration = object : SchemaMigration {
                    override fun migrate(event: TelemetryEvent): TelemetryEvent {
                        val newProperties = event.properties.toMutableMap()
                        newProperties["action_type"] = newProperties.remove("action")!!
                        return event // Would need proper copy with new properties
                    }
                }
            )
        }

        // 2. Setup Cache
        val memoryCache = MemoryCache()
        val diskCache = SQLiteEventCache(database)
        val cache = MultiTierEventCache(
            memoryCache = memoryCache,
            diskCache = diskCache,
            config = CacheConfig(
                maxMemoryEvents = 500,
                maxDiskEvents = 5000,
                evictionBatchSize = 50
            )
        )

        // 3. Setup Destinations
        val destinations = mapOf(

            Destination.NEW_RELIC to NewRelicDestination(
                newRelicClient, NewRelicConfig(
                    apiKey = "",
                    accountId = "",
                )
            ),

            Destination.FIREBASE to FirebaseDestination(
                firebaseAnalytics, FirebaseConfig(
                    appId = "",
                    apiKey = "",
                    projectId = ""
                )
            ),

            Destination.LOCAL_DB to LocalDatabaseDestination(database)
        )

        // 4. Create Repository
        return ObservabilityDataRepository(
            destinations = destinations,
            cache = cache,
            schemaRegistry = schemaRegistry,
            scope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
            retryQueue = InMemoryRetryQueue(),
            config = ObservabilityConfig()
        )
    }
}
