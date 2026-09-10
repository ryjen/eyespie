import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.app.IosExternalIngress
import com.micrantha.eyespie.app.ScopedDiagnosticsExporter
import com.micrantha.eyespie.game.EyespieRuntimeBootstrapResult
import com.micrantha.eyespie.game.bootstrapIosEyespieRuntime
import com.micrantha.eyespie.sharing.IosGameDocumentTransfer
import com.micrantha.eyespie.sharing.IosGameSharePresenter
import com.micrantha.eyespie.telemetry.IosDiagnosticArtifactWriter
import platform.Foundation.NSLog
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        val bootstrap = remember {
            bootstrapIosEyespieRuntime().also { result ->
                if (result is EyespieRuntimeBootstrapResult.Failed) {
                    NSLog("Eyespie runtime initialization failed: ${result.cause.stackTraceToString()}")
                }
            }
        }
        val diagnosticsWriter = remember {
            IosDiagnosticArtifactWriter { controller }
        }

        when (bootstrap) {
            is EyespieRuntimeBootstrapResult.Failed -> {
                val diagnosticsExporter = remember(bootstrap, diagnosticsWriter) {
                    ScopedDiagnosticsExporter(
                        diagnosticExport = bootstrap.diagnostics.export,
                        writer = diagnosticsWriter,
                    )
                }
                AppUnavailable(diagnosticsExporter = diagnosticsExporter)
            }
            is EyespieRuntimeBootstrapResult.Ready -> {
                val documentTransfer = remember {
                    IosGameDocumentTransfer(
                        presenter = { controller },
                        externalDocumentSource = IosExternalIngress,
                    )
                }
                val sharePresenter = remember {
                    IosGameSharePresenter { controller }
                }
                App(
                    runtime = bootstrap.runtime,
                    documentTransfer = documentTransfer,
                    externalDocumentSource = IosExternalIngress,
                    sharePresenter = sharePresenter,
                    externalAppIntentSource = IosExternalIngress,
                    diagnosticArtifactWriter = diagnosticsWriter,
                )
            }
        }
    }
    return controller
}
