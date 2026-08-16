package com.micrantha.eyespie.imaging.calibration

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.micrantha.eyespie.BuildConfig
import com.micrantha.eyespie.core.MatchEngine
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.IMAGE_EMBEDDER_MODEL_SHA256
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.loadAndroidImageEmbeddingModel
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhysicalImageEmbeddingCalibrationTest {
    @Test
    fun collectPinnedPhysicalCalibrationReport() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fixtureContext = instrumentation.context
        val targetContext = instrumentation.targetContext
        val model = loadAndroidImageEmbeddingModel(targetContext)
        check(model.sha256 == IMAGE_EMBEDDER_MODEL_SHA256) {
            "verified Android model digest does not match active embedding contract"
        }
        val generator = MediaPipeImageEmbeddingGenerator(
            context = targetContext,
            modelBuffer = model.directBuffer(),
        )
        val fixtureSpecs = loadFixtureSpecs(fixtureContext)

        val fixtures = fixtureSpecs.map { spec ->
            val bytes = fixtureContext.assets.open("$FIXTURE_DIRECTORY/${spec.fileName}").use {
                it.readBytes()
            }
            val actualDigest = MessageDigest.getInstance("SHA-256").digest(bytes).toLowerHex()
            check(actualDigest == spec.sha256) {
                "calibration fixture failed SHA-256 verification: ${spec.id}"
            }
            val capturedImage = CapturedImage.fromEncoded(bytes)
            val runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) {
                generator.generate(capturedImage)
            }
            summarizeImageEmbeddingCalibrationFixture(
                id = spec.id,
                role = spec.role,
                sourceSha256 = actualDigest,
                runs = runs,
            )
        }

        val packageInfo = targetContext.packageManager.getPackageInfo(targetContext.packageName, 0)
        val report = ImageEmbeddingCalibrationReport(
            platform = "android",
            application = ImageEmbeddingCalibrationApplication(
                version = requireNotNull(packageInfo.versionName) {
                    "installed Android app version name is unavailable"
                },
                build = packageInfo.longVersionCode.toInt(),
            ),
            device = ImageEmbeddingCalibrationDevice(
                manufacturer = Build.MANUFACTURER,
                model = "${Build.MODEL} [device=${Build.DEVICE}; hardware=${Build.HARDWARE}]",
                os = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            ),
            runtime = ImageEmbeddingCalibrationRuntime(
                name = "MediaPipe Tasks Vision",
                version = BuildConfig.MEDIAPIPE_TASKS_VISION_VERSION,
            ),
            model = ImageEmbeddingCalibrationModel(sha256 = model.sha256),
            matchPolicy = ImageEmbeddingCalibrationMatchPolicy(
                cosineThreshold = MatchEngine.DEFAULT_THRESHOLD,
            ),
            fixtures = fixtures,
        )

        val outputDirectory = File(targetContext.filesDir, "image-embedding-calibration")
        check(outputDirectory.mkdirs() || outputDirectory.isDirectory) {
            "cannot create app-private calibration report directory"
        }
        File(outputDirectory, "android.json").writeText(report.toCalibrationJson(), Charsets.UTF_8)
    }

    private fun loadFixtureSpecs(context: android.content.Context): List<FixtureSpec> {
        val payload = context.assets.open("$FIXTURE_DIRECTORY/manifest.json")
            .bufferedReader(Charsets.UTF_8)
            .use { JSONObject(it.readText()) }
        check(payload.getInt("schema_version") == 1) {
            "unsupported staged calibration fixture manifest"
        }
        val array = payload.getJSONArray("fixtures")
        check(array.length() == 4) { "calibration fixture manifest must contain exactly four fixtures" }
        return List(array.length()) { index ->
            val raw = array.getJSONObject(index)
            FixtureSpec(
                id = raw.getString("id"),
                role = raw.getString("role"),
                fileName = raw.getString("file_name"),
                sha256 = raw.getString("sha256"),
            )
        }.also { fixtures ->
            check(fixtures.map { it.id } == EXPECTED_FIXTURE_IDS) {
                "calibration fixture ids/order do not match the reviewed evidence contract"
            }
        }
    }

    private data class FixtureSpec(
        val id: String,
        val role: String,
        val fileName: String,
        val sha256: String,
    )

    private fun ByteArray.toLowerHex(): String = buildString(size * 2) {
        for (byte in this@toLowerHex) {
            val value = byte.toInt() and 0xff
            append(HEX[value ushr 4])
            append(HEX[value and 0x0f])
        }
    }

    private companion object {
        const val FIXTURE_DIRECTORY = "image-embedding-calibration"
        const val HEX = "0123456789abcdef"
        val EXPECTED_FIXTURE_IDS = listOf("burger", "burger_crop", "burger_rotated", "cat")
    }
}
