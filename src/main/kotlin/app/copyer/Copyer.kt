package app.copyer

import WorkDir
import app.actionpack.ActionExecutor
import app.actionpack.EventListener
import kotlinx.coroutines.runBlocking
import logger
import model.Usb
import model.UsbConfig
import repository.db.FsDB
import utils.UsbUtils
import utils.UsbUtils.getUsb
import utils.fileWalkOnUsb
import utils.hidden
import utils.listenerUsbBeenRemoved
import java.io.File
import java.nio.file.Files
import javax.swing.filechooser.FileSystemView

//DebugFlag
private const val filterCopiedDebug = true
class Copyer(private val usbRootFile: File) {
    private val usb = getUsb(usbRootFile)
    private val config = getConfig(usb)
    private val storagePaths = runBlocking { FsDB.listFilesPath(usb.usbId) }
    suspend fun main() {
        ActionExecutor.eventListenerFlow.emit(EventListener.WhenCommonUDiskMounted(usb))
        listenerUsbBeenRemoved(usbRootFile) {
            ActionExecutor.eventListenerFlow.emit(EventListener.WhenCommonUDiskUnmounted(usb.usbId))
        }
        logger.debug("USB: <${usbRootFile}><${usb.usbId}>")
        fileWalkOnUsb(usb, initRootDir = {
            // getRootDirId
            FsDB.getUsbRootDir(usb.usbId).dirId
        }, walkDir = { file, dir ->
//            logger.debug("WalkDir: $file, $dir <${FsDB.toCleanPath(usb.root, file)}>")
            //所有的根目录的父目录是"DIR-ID-Root"
            FsDB.getDirId(usb.usbId, FsDB.toCleanPath(usb.root, file), dir.value)?:throw IllegalStateException("getDirPath或fileWalkOnUsb算法出错!($usb , $file, $dir)")
        }, walkFile = { file, _, parentDirId ->
            val banMatch = WorkDir.serviceConfig.isBanFile(usb.root, file)
            if (banMatch != null && filterCopiedDebug) {
                logger.debug("文件被跳过,因为它被位于ban.txt中的Regex匹配(file: < $file >, cleanFile: < ${FsDB.toCleanPath(usbRootFile, file)} >, regex: < $banMatch >)")
            } else {
                val path = FsDB.toFpath(usb.root, file)
                if (!storagePaths.contains(path)) copy(file, parentDirId)
                else if (filterCopiedDebug) {
                    logger.debug("一个文件被跳过复制,因为已经存在于数据库中(usb: < ${usb.usbId} >, rootDir: < ${usb.root} >, file: < ${file.absolutePath} >, fpath: < $path >)")
                }
            }
        })
        logger.info("所有文件全部复制完成")
        FsDB.cleanUsbPopException(usb.usbId)
    }
    private fun copy(file: File, parentDirId: String) {
        FsDB.insertFile(usb, file, parentDirId, config)
        logger.debug("Copied[from: ${usb.usbId} $parentDirId]: $file")
    }

    private fun getConfig(usb: Usb): UsbConfig {
        return WorkDir.serviceConfig.getUsbConfig(usb.usbId)
    }

}