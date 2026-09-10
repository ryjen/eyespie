package com.micrantha.eyespie.telemetry

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiagnosticExportTest {
    @Test
    fun exportContainsBoundedReleaseRuntimeAndStableRecordProvenance() {
        val history = BoundedDiagnosticSink(capacity = 2)
        history.record(
            DiagnosticRecord(
                operation = DiagnosticOperation.GAME_CREATE,
                result = DiagnosticResult.SUCCESS,
                durationMillis = 12,
                correlation = correlation(traceId = 11, spanId = 11),
            ),
        )
        history.record(
            DiagnosticRecord(
                operation = DiagnosticOperation.GAME_GUESS,
                result = DiagnosticResult.FAILED,
                code = DiagnosticCode.PERSISTENCE_FAILED,
                durationMillis = 34,
                correlation = correlation(traceId = 11, spanId = 12, parentSpanId = 11),
            ),
        )
        val service = DiagnosticExportService(
            history = history,
            identityProvider = DiagnosticIdentityProvider {
                DiagnosticIdentity(
                    release = DiagnosticReleaseIdentity(
                        appVersion = "0.1.0",
                        appBuild = 2,
                        sourceRevision = "a".repeat(40),
                    ),
                    runtime = DiagnosticRuntimeIdentity(
                        platform = DiagnosticPlatform.ANDROID,
                        osVersion = "Android 16",
                        mediaPipeVersion = "0.10.26",
                    ),
                )
            },
        )

        val encoded = service.encodeJson()
        assertTrue(encoded.size <= DiagnosticExportEnvelope.MAX_BYTES)

        val root = Json.parseToJsonElement(encoded.decodeToString()).jsonObject
        assertEquals("ryjen/eyespie", root.getValue("repository").jsonPrimitive.content)
        assertEquals(3, root.getValue("schema_version").jsonPrimitive.content.toInt())
        assertEquals(0, root.getValue("evicted_records").jsonPrimitive.content.toLong())
        assertEquals(0, root.getValue("dropped_records").jsonPrimitive.content.toLong())
        assertFalse(root.getValue("snapshot_incomplete").jsonPrimitive.content.toBoolean())

        val release = root.getValue("release").jsonObject
        assertEquals("0.1.0", release.getValue("app_version").jsonPrimitive.content)
        assertEquals("2", release.getValue("app_build").jsonPrimitive.content)
        assertEquals("a".repeat(40), release.getValue("source_revision").jsonPrimitive.content)
        assertEquals("4", release.getValue("database_schema_version").jsonPrimitive.content)
        assertEquals("1", release.getValue("bundle_schema_version").jsonPrimitive.content)

        val runtime = root.getValue("runtime").jsonObject
        assertEquals("android", runtime.getValue("platform").jsonPrimitive.content)
        assertEquals("Android 16", runtime.getValue("os_version").jsonPrimitive.content)
        assertEquals("0.10.26", runtime.getValue("mediapipe_version").jsonPrimitive.content)
        assertEquals("1024", runtime.getValue("embedding_dimensions").jsonPrimitive.content)

        val records = root.getValue("records").jsonArray
        assertEquals(2, records.size)
        assertEquals(2, records[0].jsonObject.getValue("schema_version").jsonPrimitive.content.toInt())
        assertEquals("game_create", records[0].jsonObject.getValue("operation").jsonPrimitive.content)
        assertEquals("success", records[0].jsonObject.getValue("result").jsonPrimitive.content)
        val rootCorrelation = records[0].jsonObject.getValue("correlation").jsonObject
        assertEquals(traceId(11), rootCorrelation.getValue("trace_id").jsonPrimitive.content)
        assertEquals(spanId(11), rootCorrelation.getValue("span_id").jsonPrimitive.content)
        assertFalse("parent_span_id" in rootCorrelation)

        assertEquals("game_guess", records[1].jsonObject.getValue("operation").jsonPrimitive.content)
        assertEquals("persistence_failed", records[1].jsonObject.getValue("code").jsonPrimitive.content)
        val childCorrelation = records[1].jsonObject.getValue("correlation").jsonObject
        assertEquals(traceId(11), childCorrelation.getValue("trace_id").jsonPrimitive.content)
        assertEquals(spanId(12), childCorrelation.getValue("span_id").jsonPrimitive.content)
        assertEquals(spanId(11), childCorrelation.getValue("parent_span_id").jsonPrimitive.content)
    }

    @Test
    fun exportReportsDiagnosticLossAndIncompleteSnapshotWithoutDroppedPayload() {
        val history = object : DiagnosticHistory {
            override fun snapshot(): DiagnosticSnapshot = DiagnosticSnapshot(
                records = listOf(
                    DiagnosticRecord(
                        operation = DiagnosticOperation.GAME_CREATE,
                        result = DiagnosticResult.SUCCESS,
                        durationMillis = 7,
                        correlation = correlation(traceId = 21, spanId = 21),
                    ),
                ),
                evictedRecords = 2,
                droppedRecords = 3,
                incomplete = true,
            )

            override fun clear() = Unit
        }
        val service = DiagnosticExportService(
            history = history,
            identityProvider = DiagnosticIdentityProvider {
                DiagnosticIdentity(
                    release = DiagnosticReleaseIdentity("0.1.0", 1),
                    runtime = DiagnosticRuntimeIdentity(
                        platform = DiagnosticPlatform.ANDROID,
                        osVersion = "Android 16",
                        mediaPipeVersion = "0.10.26",
                    ),
                )
            },
        )

        val root = Json.parseToJsonElement(service.encodeJson().decodeToString()).jsonObject
        assertEquals(2, root.getValue("evicted_records").jsonPrimitive.content.toLong())
        assertEquals(3, root.getValue("dropped_records").jsonPrimitive.content.toLong())
        assertTrue(root.getValue("snapshot_incomplete").jsonPrimitive.content.toBoolean())
        assertEquals(1, root.getValue("records").jsonArray.size)
    }

    @Test
    fun sourceRevisionIsOptionalButMustBeCanonicalWhenPresent() {
        val identity = DiagnosticReleaseIdentity(
            appVersion = "0.1.0",
            appBuild = 1,
            sourceRevision = null,
        )
        assertEquals(null, identity.sourceRevision)

        assertFailsWith<IllegalArgumentException> {
            DiagnosticReleaseIdentity(
                appVersion = "0.1.0",
                appBuild = 1,
                sourceRevision = "main",
            )
        }
    }

    @Test
    fun exportRejectsHistoryThatViolatesThePortableRecordBound() {
        val oversized = object : DiagnosticHistory {
            override fun snapshot(): DiagnosticSnapshot = DiagnosticSnapshot(
                records = List(DiagnosticExportEnvelope.MAX_RECORDS + 1) {
                    DiagnosticRecord(
                        operation = DiagnosticOperation.GAME_SNAPSHOT_LOAD,
                        result = DiagnosticResult.SUCCESS,
                        durationMillis = 0,
                        correlation = correlation(traceId = 31, spanId = 31),
                    )
                },
                evictedRecords = 0,
                droppedRecords = 0,
            )

            override fun clear() = Unit
        }
        val service = DiagnosticExportService(
            history = oversized,
            identityProvider = DiagnosticIdentityProvider {
                DiagnosticIdentity(
                    release = DiagnosticReleaseIdentity("0.1.0", 1),
                    runtime = DiagnosticRuntimeIdentity(
                        platform = DiagnosticPlatform.IOS,
                        osVersion = "iOS 18.0",
                        mediaPipeVersion = "0.10.26.2",
                    ),
                )
            },
        )

        assertFailsWith<IllegalArgumentException> { service.snapshot() }
    }

    @Test
    fun portableJsonHasNoGenericMessageAttributeOrPayloadChannel() {
        val service = DiagnosticExportService(
            history = BoundedDiagnosticSink(),
            identityProvider = DiagnosticIdentityProvider {
                DiagnosticIdentity(
                    release = DiagnosticReleaseIdentity("0.1.0", 1),
                    runtime = DiagnosticRuntimeIdentity(
                        platform = DiagnosticPlatform.ANDROID,
                        osVersion = "Android 16",
                        mediaPipeVersion = "0.10.26",
                    ),
                )
            },
        )
        val encoded = service.encodeJson().decodeToString()

        listOf("message", "attributes", "payload", "path", "image", "embedding").forEach { field ->
            assertFalse("\"$field\"" in encoded)
        }
    }

    private fun correlation(
        traceId: Int,
        spanId: Int,
        parentSpanId: Int? = null,
    ): DiagnosticCorrelation = DiagnosticCorrelation(
        traceId = traceId(traceId),
        spanId = spanId(spanId),
        parentSpanId = parentSpanId?.let(::spanId),
    )

    private fun traceId(value: Int): String = value.toString(16).padStart(32, '0')
    private fun spanId(value: Int): String = value.toString(16).padStart(16, '0')
}
