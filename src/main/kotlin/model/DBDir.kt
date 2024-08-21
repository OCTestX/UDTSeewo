package model

import kotlinx.serialization.Serializable

@Serializable
data class DBDir(
    val usbId: String,
    val path: String,
    val dirId: String,
    val parentDirId: String,
    val name: String
)
