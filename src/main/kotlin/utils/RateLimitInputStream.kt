package utils

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

class RateLimitInputStream(`in`: InputStream, private val speed: Long) : FilterInputStream(`in`) {
    private val speedBySecond = if (speed < 0) {
        Long.MAX_VALUE
    } else speed
    var startTime = System.nanoTime()
    var spend = 0L
    fun checkTime(): Long? {
        //重置带宽
        val t = System.nanoTime() - startTime
        return if (t >= 1000000000) {
            spend = 0
            startTime = System.nanoTime()
            null
        } else ((1000000000 - t) / 1000000)
    }
    fun checkSpend() {
        if (spend >= speedBySecond) {
            val sleep = checkTime()
            sleep?.let {
                Thread.sleep(it)
            }
        }
    }

    @Throws(IOException::class)
    override fun read(): Int {
        checkTime()
        val result = super.read()
        spend ++
        checkSpend()
        return result
    }

    @Throws(IOException::class)
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        checkTime()
        val result = super.read(b, off, len)
        if (result != -1) {
            spend += result
            checkSpend()
        }
        return result
    }
}