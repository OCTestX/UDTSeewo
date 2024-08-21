package utils

enum class SystemType {
    Win, Linux, Mac
}
fun getSystemType(): SystemType {
    return SystemType.Win
//    return when (System.getProperty("os.name").toLowerCase()) {
//        "win" -> SystemType.Win
//        "linux" -> SystemType.Linux
//        "mac" -> SystemType.Mac
//        else -> throw Exception("Unsupported system type")
//    }
}