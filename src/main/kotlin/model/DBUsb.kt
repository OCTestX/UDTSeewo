package model

import kotlinx.serialization.Serializable

@Serializable
data class DBUsb(
    val name: String,
    val totalSize: Long,
    val freeSize: Long,
    val usbId: String,
    val popCount: Int,
)