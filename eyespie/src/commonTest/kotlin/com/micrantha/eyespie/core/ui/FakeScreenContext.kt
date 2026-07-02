package com.micrantha.eyespie.core.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.i18n.repository.LocalizedRepository
import com.micrantha.bluebell.platform.FileSystem
import com.micrantha.bluebell.ui.components.Router
import com.micrantha.bluebell.ui.components.Router.Options
import com.micrantha.bluebell.ui.screen.ScreenContext
import kotlinx.coroutines.CoroutineScope
import org.kodein.di.DI

class FakeRouter : Router {
    var lastNavigatedTo: Screen? = null
    var lastOptions: Options? = null

    override fun <T : Screen> navigate(screen: T, options: Options) {
        lastNavigatedTo = screen
        lastOptions = options
    }

    override fun navigateBack(): Boolean = true
    override val canGoBack: Boolean = false
    override val screen: Screen get() = lastNavigatedTo!!
}

class FakeScreenContext(
    override val di: DI = DI {}
) : ScreenContext {
    override val i18n: LocalizedRepository get() = TODO()
    override val router = FakeRouter()
    override val dispatcher = object : Dispatcher {
        override val dispatchScope: CoroutineScope get() = TODO()
        override fun dispatch(action: Action) {
            // No-op for now
        }
        override suspend fun send(action: Action) {
            // No-op for now
        }
    }
    override val fileSystem: FileSystem get() = TODO()
}
