package com.micrantha.eyespie.core.ui

import com.micrantha.bluebell.arch.Action
import com.micrantha.bluebell.arch.Dispatcher
import com.micrantha.bluebell.arch.FakeDispatcher
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
    override var di: DI = DI {},
    override val router: FakeRouter = FakeRouter(),
    override val dispatcher: Dispatcher = FakeDispatcher()
) : ScreenContext {
    override val i18n: LocalizedRepository get() = TODO()
    override val fileSystem: FileSystem get() = TODO()

    fun copy(
        di: DI = this.di,
        router: FakeRouter = this.router,
        dispatcher: Dispatcher = this.dispatcher
    ) = FakeScreenContext(di, router, dispatcher)
}
