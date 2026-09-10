import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.app.IosExternalIngress
import com.micrantha.eyespie.game.createIosEyespieRuntime
import com.micrantha.eyespie.sharing.IosGameDocumentTransfer
import com.micrantha.eyespie.sharing.IosGameSharePresenter
import platform.Foundation.NSLog
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
        val runtime = remember {
            try {
                createIosEyespieRuntime()
            } catch (exception: Exception) {
                NSLog("Eyespie runtime initialization failed: ${exception.stackTraceToString()}")
                null
            }
        }
        if (runtime == null) {
            AppUnavailable()
        } else {
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
                runtime = runtime,
                documentTransfer = documentTransfer,
                externalDocumentSource = IosExternalIngress,
                sharePresenter = sharePresenter,
                externalAppIntentSource = IosExternalIngress,
            )
        }
    }
    return controller
}
