package utils

import logger

inline fun <T> packResult(block: () -> Result<T>): Result<T> {
    return try {
        block()
    } catch (e: Throwable) {
        logger.error("被监测的Throw: " + e.stackTraceToString())
        Result.failure(e)
    }
}