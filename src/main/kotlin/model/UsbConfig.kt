package model

import WorkDir
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UsbConfig(
    val defaultCopySpeed: Long,
) {
    companion object {
        fun getConfig(usb: Usb): UsbConfig {
            return run {
                val locateTheUsb = File(usb.root, ".udtConfig.json")
                if (locateTheUsb.exists()) return@run Json.decodeFromString(locateTheUsb.readText())
                return@run WorkDir.serviceConfig.commonUsbConfig
            }
        }
    }
}