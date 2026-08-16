import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import com.micrantha.eyespie.App
import com.micrantha.eyespie.AppUnavailable
import com.micrantha.eyespie.game.createIosEyespieRuntime
import com.micrantha.eyespie.sharing.IosBundleActions
import com.micrantha.eyespie.sharing.IosGameDocumentTransfer
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    lateinit var controller: UIViewController
    controller = ComposeUIViewController {
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
            val documentTransfer = remember {
                IosGameDocumentTransfer { controller }
            }
            var contentVersion by remember { mutableIntStateOf(0) }
            Box {
                key(contentVersion) {
                    App(runtime)
                }
                IosBundleActions(
                    runtime = runtime,
                    transfer = documentTransfer,
                    onImported = { contentVersion += 1 },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                )
            }
        }
    }
    return controller
}
