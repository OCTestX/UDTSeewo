package model

import WorkDir
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class UsbConfig(
    val copySpeed: Long,
)