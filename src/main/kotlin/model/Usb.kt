package model

import java.io.File

data class Usb(
    val root: File,//以/分割
    val usbName: String,
    val totalSize: Long,
    val freeSize: Long,
    val usbId: String,
    val popCount: Int
)
