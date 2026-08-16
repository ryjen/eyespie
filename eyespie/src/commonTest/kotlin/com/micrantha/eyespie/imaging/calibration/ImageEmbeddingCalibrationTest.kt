package com.micrantha.eyespie.imaging.calibration

import com.micrantha.eyespie.imaging.IMAGE_EMBEDDING_DIMENSIONS
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_ID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ImageEmbeddingCalibrationTest {
    @Test
    fun summarizesExactlyFiveCanonicalRuns() {
        val reference = vector(1f, 0f)
        val shifted = vector(0.8f, 0.6f)

        val fixture = summarizeImageEmbeddingCalibrationFixture(
            id = "burger",
            role = "reference",
            sourceSha256 = "a".repeat(64),
            runs = listOf(reference, shifted, shifted, shifted, shifted),
        )

        assertEquals(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT, fixture.repeatCount)
        assertEquals(reference, fixture.embedding)
        assertEquals(0.8, fixture.repeatCosineMin, absoluteTolerance = 1e-6)
        assertEquals(0.6, fixture.repeatMaxAbsDelta, absoluteTolerance = 1e-6)
    }

    @Test
    fun rejectsUnboundedOrIncompleteRunSets() {
        val reference = vector(1f, 0f)

        assertFailsWith<IllegalArgumentException> {
            summarizeImageEmbeddingCalibrationFixture(
                id = "burger",
                role = "reference",
                sourceSha256 = "a".repeat(64),
                runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT - 1) { reference },
            )
        }
        assertFailsWith<IllegalArgumentException> {
            summarizeImageEmbeddingCalibrationFixture(
                id = "burger",
                role = "reference",
                sourceSha256 = "a".repeat(64),
                runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT + 1) { reference },
            )
        }
    }

    @Test
    fun rejectsNonCanonicalEmbeddingEvidence() {
        val malformed = vector(1f, 0f).toMutableList().also { it[3] = Float.NaN }

        assertFailsWith<IllegalArgumentException> {
            summarizeImageEmbeddingCalibrationFixture(
                id = "burger",
                role = "reference",
                sourceSha256 = "a".repeat(64),
                runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) { malformed },
            )
        }
    }

    @Test
    fun rendersStableSchemaAndEscapesPlatformStrings() {
        val fixture = summarizeImageEmbeddingCalibrationFixture(
            id = "burger",
            role = "reference",
            sourceSha256 = "a".repeat(64),
            runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) { vector(1f, 0f) },
        )
        val report = ImageEmbeddingCalibrationReport(
            platform = "android",
            application = ImageEmbeddingCalibrationApplication(version = "0.1.0", build = 1),
            device = ImageEmbeddingCalibrationDevice(
                manufacturer = "test\"maker",
                model = "model\\one",
                os = "test\nos",
            ),
            runtime = ImageEmbeddingCalibrationRuntime(name = "runtime", version = "1.0"),
            model = ImageEmbeddingCalibrationModel(sha256 = "b".repeat(64)),
            matchPolicy = ImageEmbeddingCalibrationMatchPolicy(cosineThreshold = 0.75),
            fixtures = listOf(fixture),
        )

        val json = report.toCalibrationJson()

        assertTrue(json.contains("\"report_schema_version\": 2"))
        assertTrue(json.contains("\"version\": \"0.1.0\", \"build\": 1"))
        assertTrue(json.contains("\"id\": \"$IMAGE_EMBEDDER_MODEL_ID\""))
        assertTrue(json.contains("\"dimensions\": $IMAGE_EMBEDDING_DIMENSIONS"))
        assertTrue(json.contains("test\\\"maker"))
        assertTrue(json.contains("model\\\\one"))
        assertTrue(json.contains("test\\nos"))
    }

    private fun vector(first: Float, second: Float): List<Float> =
        buildList(IMAGE_EMBEDDING_DIMENSIONS) {
            add(first)
            add(second)
            repeat(IMAGE_EMBEDDING_DIMENSIONS - 2) { add(0f) }
        }
}
