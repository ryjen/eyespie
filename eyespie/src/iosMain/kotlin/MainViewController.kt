import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createIosEyespieRuntime
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    val runtime = remember {
        try {
            createIosEyespieRuntime()
        } catch (_: Exception) {
            null
        }
    }
    if (runtime == null) {
        AppUnavailable()
    } else {
        App(runtime)
    }
}
