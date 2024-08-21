package utils

import logger
import org.slf4j.Logger
import org.slf4j.event.Level
import org.slf4j.event.Level.*

fun Logger.warn(t: Throwable) {
    warn(t.stackTraceToString())
}
inline fun <reified T> T.plog(level: Level = Level.DEBUG, output: (T) -> String = { it.toString() }): T {
    when(level) {
        ERROR -> logger.error(output(this))
        WARN -> logger.warn(output(this))
        INFO -> logger.info(output(this))
        DEBUG -> logger.debug(output(this))
        TRACE -> logger.trace(output(this))
    }
    return this
}

inline fun <reified T> T.plog(startWith: String, level: Level = Level.DEBUG): T = plog(level) {
    "$startWith${it.toString()}"
}

inline fun <reified T> T.plog(enable: Boolean, level: Level = Level.DEBUG, output: (T) -> String = { it.toString() }): T = apply {
    if (enable) plog(level, output)
}

inline fun <reified T> T.plog(enable: Boolean, startWith: String, level: Level = Level.DEBUG): T = apply {
    if (enable) plog(startWith, level)
}