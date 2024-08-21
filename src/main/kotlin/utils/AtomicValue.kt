package utils

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

suspend fun AtomicBoolean.waitBeTrue(delay: Long = 10) {
    while (true) {
        if (this.get()) {
            return
        } else {
            delay(delay)
        }
    }
}