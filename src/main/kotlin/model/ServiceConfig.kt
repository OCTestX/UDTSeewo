package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logger
import repository.db.FsDB
import utils.link
import utils.linkFile
import java.io.File
import java.nio.charset.Charset

@Serializable
private data class ConfigFileContent(
    val defaultCopySpeed: Long,
    val serverPort: Int,
    val currentDir: String,
    val remoteServerAddress: String

)

//DebugFlag
private const val checkBansFileDebug = false

data class ServiceConfig(
    val defaultCopySpeed: Long,
    val serverPort: Int,
    val currentDir: File,
    val remoteServerAddress: String
) {
    val tempDir = File(currentDir, "Temp")
    val cacheDir = File(currentDir, "Cache")
    val storageDir = File(currentDir, "Storage")
    val fsDBFile = File(currentDir, "fs.db")
    val logsDir = File(currentDir, "Logs")
    val configFile = File(currentDir, "serviceConfig.json")
    //
    val commonUsbConfig = UsbConfig(defaultCopySpeed)
    companion object {
        private lateinit var serviceConfig: ServiceConfig
        fun getConfig(rootPath: String): ServiceConfig {
            if (!this::serviceConfig.isInitialized) {
                val configContent: ConfigFileContent = Json.decodeFromString(File(rootPath, "serviceConfig.json").apply {
                    if (exists().not()) {
                        val newConfig = ConfigFileContent(
                            defaultCopySpeed = 1024 * 1024 * 10,
                            serverPort = 8080,
                            currentDir = rootPath,
                            remoteServerAddress = "http://localhost:8080"
                        )
                        writeText(Json.encodeToString(newConfig))
                    }
                }.readText())
                serviceConfig = transformConfig(configContent)
            }
            return serviceConfig
        }
        private fun transformConfig(content: ConfigFileContent): ServiceConfig {
            return ServiceConfig(
                content.defaultCopySpeed,
                content.serverPort,
                File(content.currentDir),
                content.remoteServerAddress
            )
        }
    }

//    val bans: List<Regex> = currentDir.linkFile("bans.txt").apply { if (exists().not())createNewFile() }.readLines(Charset.forName("UTF-8")).map { it.toRegex() }
    val bans: List<Regex> = listOf()
    fun isBanFile(usbRootFile: File, file: File): Regex? = isBanFile(FsDB.toCleanPath(usbRootFile, file))
    private fun isBanFile(fromFpath: String): Regex? {
        return bans.find {
            it.matches(fromFpath).apply {
                if (checkBansFileDebug) logger.debug("尝试匹配(Regex: < $it >, text: < $fromFpath >), ${if (this) "成功" else "失败"}")
            }
        }
    }
}
