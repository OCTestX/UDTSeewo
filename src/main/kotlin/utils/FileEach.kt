package utils

import logger
import model.Usb
import repository.db.FsDB
import repository.expection.UsbPopException
import java.io.File

private fun <R> fileWalk(
    rootWalking: WalkingDir<R>,
    walkDir: (dir: File, walkingDir: WalkingDir<R>) -> R,
    walkFile: (file: File, dir: WalkingDir<R>, value: R) -> Unit,
    walkingUnknownInEach: (File, WalkingDir<R>) -> Unit,
    walkingUnknown: (WalkingDir<R>) -> Unit
){
    val root = rootWalking.dir
    if (root.isDirectory) {
        for (f in root.listFiles()?: emptyArray()){
            if (f.isDirectory) {
                val value = walkDir(f, rootWalking)
                val dir = WalkingDir(f, value)
                fileWalk(dir, walkDir, walkFile, walkingUnknownInEach, walkingUnknown)
            } else if (f.isFile) {
                walkFile(f, rootWalking, rootWalking.value!!)
            } else {
                walkingUnknownInEach(f, rootWalking)
            }
        }
    } else {
        walkingUnknown(rootWalking)
    }
}
fun <R> fileWalkOnUsb(usb: Usb, initRootDir: (root: File) -> R, walkDir: (dir: File, walkingDir: WalkingDir<R>) -> R, walkFile: (file: File, dir: WalkingDir<R>, value: R) -> Unit) {
    fileWalk(WalkingDir(usb.root, initRootDir(usb.root)), walkDir, walkFile, walkingUnknownInEach = { file: File, _: WalkingDir<R> ->
        if (usb.root.exists().not()) {
            FsDB.plusUsbPopException(usb.usbId)
            throw UsbPopException("(U盘可能已弹出)Not a file or directory: $file")
        } else {
            logger.error("无法访问文件: usb: $usb file: $file")
        }
    }, walkingUnknown = {
        if (usb.root.exists().not()) {
            FsDB.plusUsbPopException(usb.usbId)
            throw UsbPopException("(U盘可能已弹出)Not a file or directory(WalkingDir): $it")
        } else {
            logger.error("无法访问文件: usb: $usb dir: $it")
        }
    })
}
data class WalkingDir<R>(val dir: File, val value: R)

private fun fileEach(usb: Usb, root: File, file: (File) -> Unit, popError: (UsbPopException) -> Unit) {
    val files = root.listFiles()!!
    for (f in files) {
        if (f.isDirectory) {
            try {
                fileEach(usb, f, file) {
                    popError(it)
                }
            } catch (e: UsbPopException) {
                popError(e)
                break
            }
        } else if (f.isFile) {
            file(f)
        } else {
            if (usb.root.exists().not()) {
                FsDB.plusUsbPopException(usb.usbId)
                throw UsbPopException("(U盘可能已弹出)Not a file or directory: $f")
            } else {
                logger.error("无法访问文件: usb: $usb file: $f")
            }
        }
    }
}
private fun fileEach(usb: Usb, file: (File) -> Unit) {
    fileEach(usb, usb.root, file) {
        throw it
    }
}
fun fileEachCommon(root: File, file: (File) -> Unit) {
    try {
        val files = root.listFiles()!!
        for (f in files) {
            if (f.isDirectory) {
                fileEachCommon(f, file)
            } else if (f.isFile) {
                file(f)
            } else {
                logger.error("无法访问文件: file: $f")
            }
        }
    } catch (e: Throwable) {
        logger.error(e.stackTraceToString() + "root: $root")
    }
}