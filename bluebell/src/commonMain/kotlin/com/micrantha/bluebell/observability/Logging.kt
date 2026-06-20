package com.micrantha.bluebell.observability

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LoggerDelegate<in R : Any> : ReadOnlyProperty<R, KLogger> {
    override fun getValue(thisRef: R, property: KProperty<*>): KLogger {
        return KotlinLogging.logger{}
    }
}

fun logger() = LoggerDelegate<Any>()

fun KLogger.debug(message: Any?) = this.debug { message }
fun KLogger.info(message: Any?) = this.info { message }
fun KLogger.warn(message: Any?) = this.warn { message }
fun KLogger.error(message: Any?) = this.error { message }
fun KLogger.error(message: Any?, throwable: Throwable?) {
    if (throwable == null) this.error { message } else this.error(throwable) { message }
}
fun KLogger.error(throwable: Throwable, message: Any?) = this.error(throwable) {message}
