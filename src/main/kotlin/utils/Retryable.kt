package utils

/**
 * @param limit 为-1时无限
 * @param checkResult 返回true时返回
 * @param catchErr 返回true时自动以异常中断
 */
inline fun <reified R> retryable(limit: Int = 30, checkResult: (R) -> Boolean = { true }, catchErr: (e: Throwable) -> Boolean = { false }, finish: () -> Unit, block: () -> R): R {
    var count = limit
    var leastValue: R? = null
    var leastException: Throwable? = null
    while (limit == -1 || count > 0) {
        try {
            val value = block()
            leastValue = value
            if (checkResult(value)) {
                finish()
                return value
            }
        } catch (e: Throwable) {
            leastException = e
            if (catchErr(e)) {
                finish()
                throw RetryableException(leastValue, leastException)
            }
        }
        count --
    }
    finish()
    throw RetryableException.LimitedException(leastValue, leastException)
}

open class RetryableException(val leastValue: Any? = null, val leastException: Throwable?, msg: String = leastException?.message?:"未知错误"): Exception(msg, leastException) {
    class LimitedException(leastValue: Any?, leastException: Throwable?): RetryableException(leastValue, leastException, "超过重试限制")
}