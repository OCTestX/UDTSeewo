package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logger
import repository.db.FsDB
import java.io.File

@Serializable
private data class ConfigFileContent(
    val id: String,
    val defaultCopySpeed: Long,
    val serverPort: Int,
    val currentDir: String,
    val remoteServerAddress: String,
    val banFileRegex: List<String>,
    val usbConfig: Map<String, UsbConfig>,
)

data class ServiceConfig(
    val id: String,
    val defaultCopySpeed: Long,
    val serverPort: Int,
    val currentDir: File,
    val remoteServerAddress: String,
    val banFileRegex: List<Regex>,
    val usbConfig: Map<String, UsbConfig>,
) {
    val tempDir = File(currentDir, "Temp").apply { mkdirs() }
    val cacheDir = File(currentDir, "Cache").apply { mkdirs() }
    val storageDir = File(currentDir, "Storage").apply { mkdirs() }
    val fsDBFile = File(currentDir, "fs.db")
    val logsDir = File(currentDir, "Logs").apply { mkdirs() }
    val configFile = File(currentDir, "serviceConfig.json")
    val actionFile = File(currentDir, "action.json").apply { ActionPack.loadAction(File(currentDir, "action.json")) }
    companion object {
        private lateinit var serviceConfig: ServiceConfig
        fun getConfig(rootPath: String): ServiceConfig {
            if (!this::serviceConfig.isInitialized) {
                val configFile = File(rootPath, "serviceConfig.json")
                val configContent: ConfigFileContent = if (configFile.exists().not()) {
                    createNewConfig(configFile, rootPath)
                } else {
                    try {
                        Json.decodeFromString(
                            configFile.readText()
                        )
                    } catch (e: Throwable) {
                        logger.error("读取配置文件失败: ${e.message}, 尝试重新创建配置文件...")
                        createNewConfig(configFile, rootPath)
                    }
                }
                serviceConfig = transformConfig(configContent)
            }
            return serviceConfig
        }
        private fun createNewConfig(file: File, rootPath: String): ConfigFileContent {
            val newConfig = ConfigFileContent(
                id = System.nanoTime().toString(),
                defaultCopySpeed = 1024 * 1024 * 10,
                serverPort = 8080,
                currentDir = rootPath,
                remoteServerAddress = "http://localhost:28080",
                banFileRegex = emptyList(),
                usbConfig = emptyMap()
            )
            file.writeText(Json.encodeToString(newConfig))
            return newConfig
        }
        private fun transformConfig(content: ConfigFileContent): ServiceConfig {
            return ServiceConfig(
                content.id,
                content.defaultCopySpeed,
                content.serverPort,
                File(content.currentDir),
                content.remoteServerAddress,
                content.banFileRegex.map { Regex(it) },
                content.usbConfig,
            )
        }
    }

    fun isBanFile(usbRootFile: File, file: File): Regex? = isBanFile(FsDB.toCleanPath(usbRootFile, file))
    private fun isBanFile(fromFpath: String): Regex? {
        return banFileRegex.find {
            it.matches(fromFpath).apply {
                logger.debug("尝试匹配(Regex: < $it >, text: < $fromFpath >), ${if (this) "成功" else "失败"}")
            }
        }
    }

    fun getUsbConfig(usbId: String): UsbConfig = usbConfig[usbId]?: getDefaultUsbConfig()
    private fun getDefaultUsbConfig(): UsbConfig = UsbConfig(
        copySpeed = defaultCopySpeed,
    )

    fun getAction(): ActionPack = ActionPack.loadAction(actionFile)
}
