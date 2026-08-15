import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController { App() }
