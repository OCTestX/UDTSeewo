package utils

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object UsbUtils {
    fun getDiskSerialNumberByFile(devicePath: File): String {
        val uuidFile = devicePath.linkDir("System Volume Information").linkFile("IndexerVolumeGuid")
        return filterAlphanumeric(uuidFile.readText().replace("{", "").replace("}", "").replace("-", "").trim())
    }
    private fun filterAlphanumeric(input: String): String {
        // 正则表达式，匹配任何非字母和数字的字符
        val regex = Regex("[^a-zA-Z0-9]")

        // 使用正则表达式过滤掉非字母数字字符
        val filtered = input.replace(regex, "")

        return filtered
    }
    @Deprecated("Use getDiskSerialNumberByFile instead")
    fun getDiskSerialNumber(devicePath: File): String? {
        val command = when (getSystemType()) {
            SystemType.Win -> "wmic path Win32_PhysicalMedia where DriveType=2 get SerialNumber"
            SystemType.Linux -> "blkid /dev/${devicePath.absolutePath} | grep UUID"
            SystemType.Mac -> TODO()
        }
        val processBuilder = ProcessBuilder(command.split(" "))
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (line!!.contains("SerialNumber")) continue
            if (line!!.contains("UUID")) continue
            return line
        }
        return null
    }
}