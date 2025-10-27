package com.micrantha.bluebell.observability

import mu.KLogger
import mu.KotlinLogging
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class LoggerDelegate<in R : Any> : ReadOnlyProperty<R, KLogger> {
    override fun getValue(thisRef: R, property: KProperty<*>): KLogger {
        return KotlinLogging.logger {}
    }
}

fun logger() = LoggerDelegate<Any>()

fun KLogger.warn(msg: Any?) = warn { msg }
fun KLogger.info(msg: Any?) = info { msg }
fun KLogger.error(msg: Any?) = error { msg }
fun KLogger.error(msg: Any?, throwable: Throwable?) = error(throwable) { msg }
fun KLogger.debug(msg: Any?) = debug { msg }
