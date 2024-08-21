package utils

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

fun File.transferFilePartTo(targetFile: File, startOffset: Long = 0): Long {
    if (length() < startOffset) throw IllegalStateException("传输文件字节数超出源文件: $this[${length() < startOffset}]")
    if (length() == startOffset) return 0
    val output = if (startOffset == 0L) targetFile.outputStream() else targetFile.appendOutputStream()
    val input = inputStream()
    if (startOffset != 0L) input.skip(startOffset)
    val blockSize = 1024
    val buffer = ByteArray(blockSize)
    var bytesRead: Int
    var transfered = 0L
    while (input.read(buffer).also { bytesRead = it } != -1) {
        output.write(buffer, 0, bytesRead)
        transfered += bytesRead
    }
    input.close()
    output.close()
    return transfered
}

fun File.autoTransferTo(out: File, speedLimit: Long = Long.MAX_VALUE, append: Boolean = false) {
    if (!isFile) {
        throw Exception("仅支持复制文件: $this -> $out")
    }
    if (!out.exists()) {
        out.parentFile.mkdirs()
        out.createNewFile()
    } else if (!out.isFile) {
        throw Exception("仅支持复制文件: $this -> $out")
    }
    if (append) {
        if (length() <= out.length()) {
            return
        }
        inputStream().autoTransferTo(out.appendOutputStream(), out.length(), speedLimit)
    } else inputStream().autoTransferTo(out.outputStream(), speedLimit = speedLimit)
}
fun InputStream.autoTransferTo(out: OutputStream, startOffset: Long = 0L, speedLimit: Long = Long.MAX_VALUE) {
    try {
        if (startOffset > 0L) {
            skip(startOffset)
        }
        RateLimitInputStream(this, speedLimit).transferTo(out)
    } finally {
        close()
        out.close()
    }
}