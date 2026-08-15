import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import com.micrantha.eyespie.game.createIosOfflineRuntime
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    val runtime = createIosOfflineRuntime()
    return ComposeUIViewController { App(runtime) }
}
