@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.micrantha.eyespie.imaging.calibration

import com.micrantha.eyespie.core.MatchEngine
import com.micrantha.eyespie.imaging.CapturedImage
import com.micrantha.eyespie.imaging.MediaPipeImageEmbeddingGenerator
import com.micrantha.eyespie.imaging.loadIosImageEmbeddingModel
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.UIKit.UIDevice
import platform.posix.uname
import platform.posix.utsname
import kotlin.Throws

/**
 * Explicit physical-device calibration entry point exported to the Debug iOS wrapper.
 *
 * The wrapper calls this only when the calibration launch environment is enabled. Fixture bytes
 * are generated from the pinned provenance manifest and packaged only by the opt-in Debug pod.
 */
class IosImageEmbeddingCalibrationCollector {
    @Throws(Exception::class)
    fun collect(): String = runBlocking {
        val model = loadIosImageEmbeddingModel()
        val generator = MediaPipeImageEmbeddingGenerator(modelPathProvider = { model.path })
        val fixtures = loadRuntimeFixtureSpecs().map { spec ->
            val bytes = loadResourceBytes(spec.fileName)
            val actualDigest = bytes.toByteString().sha256().hex()
            check(actualDigest == spec.sha256) {
                "calibration fixture failed SHA-256 verification"
            }
            val image = CapturedImage.fromEncoded(bytes)
            val runs = List(IMAGE_EMBEDDING_CALIBRATION_REPEAT_COUNT) {
                generator.generate(image)
            }
            summarizeImageEmbeddingCalibrationFixture(
                id = spec.id,
                role = spec.role,
                sourceSha256 = actualDigest,
                runs = runs,
            )
        }

        val bundle = NSBundle.mainBundle
        val device = UIDevice.currentDevice
        ImageEmbeddingCalibrationReport(
            platform = "ios",
            application = ImageEmbeddingCalibrationApplication(
                version = bundleString(bundle, "CFBundleShortVersionString"),
                build = bundleString(bundle, "CFBundleVersion").toIntOrNull()
                    ?: error("installed iOS app build number is invalid"),
            ),
            device = ImageEmbeddingCalibrationDevice(
                manufacturer = "Apple",
                model = hardwareModelIdentifier(device),
                os = "${device.systemName()} ${device.systemVersion}",
            ),
            runtime = ImageEmbeddingCalibrationRuntime(
                name = "EyespieMediaPipeTasksVision",
                version = bundleString(bundle, "EyespieMediaPipeTasksVersion"),
            ),
            model = ImageEmbeddingCalibrationModel(sha256 = model.sha256),
            matchPolicy = ImageEmbeddingCalibrationMatchPolicy(
                cosineThreshold = MatchEngine.DEFAULT_THRESHOLD,
            ),
            fixtures = fixtures,
        ).toCalibrationJson()
    }

    private fun loadRuntimeFixtureSpecs(): List<FixtureSpec> {
        val encoded = loadResourceBytes(RUNTIME_MANIFEST_FILE)
        val manifest = encoded.decodeToString()
        val matches = FIXTURE_ENTRY_REGEX.findAll(manifest).toList()
        check(matches.size == EXPECTED_FIXTURE_IDS.size) {
            "calibration runtime manifest must contain exactly four fixtures"
        }
        val fixtures = matches.map { match ->
            FixtureSpec(
                fileName = match.groupValues[1],
                id = match.groupValues[2],
                role = match.groupValues[3],
                sha256 = match.groupValues[4],
            )
        }
        val canonicalEntries = matches.joinToString(separator = ",") { it.value }
        val canonicalManifest =
            "{\"fixtures\":[$canonicalEntries],\"schema_version\":1}\n"
        check(manifest == canonicalManifest) {
            "calibration runtime manifest is not canonical"
        }
        check(fixtures.map { it.id } == EXPECTED_FIXTURE_IDS) {
            "calibration runtime fixture ids/order do not match the reviewed evidence contract"
        }
        check(fixtures.map { it.role } == EXPECTED_FIXTURE_ROLES) {
            "calibration runtime fixture roles do not match the reviewed evidence contract"
        }
        check(fixtures.map { it.fileName } == EXPECTED_FIXTURE_FILES) {
            "calibration runtime fixture filenames do not match the reviewed evidence contract"
        }
        return fixtures
    }

    private fun loadResourceBytes(fileName: String): ByteArray {
        val extension = fileName.substringAfterLast('.', missingDelimiterValue = "")
        val baseName = fileName.removeSuffix(if (extension.isEmpty()) "" else ".$extension")
        check(baseName.isNotEmpty() && extension.isNotEmpty()) {
            "calibration resource name is invalid"
        }
        val path = NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
            ?: error("calibration resource is unavailable")
        return FileSystem.SYSTEM.read(path.toPath()) { readByteArray() }
    }

    private fun bundleString(bundle: NSBundle, key: String): String =
        (bundle.objectForInfoDictionaryKey(key) as? String)
            ?.takeIf { it.isNotBlank() }
            ?: error("required iOS bundle identity is unavailable")

    private fun hardwareModelIdentifier(device: UIDevice): String = memScoped {
        val info = alloc<utsname>()
        if (uname(info.ptr) == 0) {
            info.machine.toKString().ifBlank { device.model() }
        } else {
            device.model()
        }
    }

    private data class FixtureSpec(
        val fileName: String,
        val id: String,
        val role: String,
        val sha256: String,
    )

    private companion object {
        const val RUNTIME_MANIFEST_FILE = "manifest.json"
        val EXPECTED_FIXTURE_IDS = listOf("burger", "burger_crop", "burger_rotated", "cat")
        val EXPECTED_FIXTURE_ROLES = listOf("reference", "related", "related", "unrelated")
        val EXPECTED_FIXTURE_FILES = listOf(
            "burger.jpg",
            "burger_crop.jpg",
            "burger_rotated.jpg",
            "cat.jpg",
        )
        val FIXTURE_ENTRY_REGEX = Regex(
            """\{"file_name":"([a-z0-9_]+\.jpg)","id":"([a-z0-9_]+)","role":"([a-z]+)","sha256":"([0-9a-f]{64})"}""",
        )
    }
}
