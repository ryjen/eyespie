import androidx.compose.runtime.Composable
import com.micrantha.eyespie.EyesPieTheme
import com.micrantha.eyespie.UIShow

@Composable
fun MainActivityContent() {
    EyesPieTheme(
        darkTheme = false,
        dynamicColor = false
    ) {
        UIShow()
    }
}
