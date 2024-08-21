package model

import kotlinx.serialization.Serializable

@Serializable
data class DBUsbFile(
    val usbId: String,
    val name: String,
    val path: String,//以/分割
    val fileId: String,
    val size: Long,
    val createTime: Long,
    val dirId: String,
)