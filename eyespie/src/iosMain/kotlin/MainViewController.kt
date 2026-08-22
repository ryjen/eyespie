import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createIosEyespieRuntime
import com.micrantha.eyespie.sharing.IosGameDocumentTransfer
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
                IosGameDocumentTransfer { controller }
            }
            App(runtime, documentTransfer)
        }
    }
    return controller
}
