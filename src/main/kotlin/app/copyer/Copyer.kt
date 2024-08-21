package app.copyer

import WorkDir
import kotlinx.coroutines.runBlocking
import logger
import model.Usb
import model.UsbConfig
import repository.db.FsDB
import utils.UsbUtils
import utils.fileWalkOnUsb
import utils.hidden
import java.io.File
import java.nio.file.Files
import javax.swing.filechooser.FileSystemView

//DebugFlag
private const val filterCopiedDebug = true
class Copyer(private val usbRootFile: File) {
    private val usb = getUsb()
    private val config = getConfig(usb)
    private val storagePaths = runBlocking { FsDB.listFilesPath(usb.usbId) }
    fun main() {
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
        return UsbConfig.getConfig(usb)
    }

    private fun getUsb(): Usb {
        val usbs = FsDB.getUsbItems()
        val name = FileSystemView.getFileSystemView().getSystemDisplayName(usbRootFile)
        val totalSize = usbRootFile.totalSpace
        val freeSize = usbRootFile.freeSpace
        val usbId = getUsbId(usbRootFile)
        val usb = usbs.find { it.usbId == usbId }
        val popCount = usb?.popCount?:0
        //不存在说明该U盘新插入
        if (usb == null) {
            FsDB.insertNewUsb(name, usbId, totalSize, freeSize)
        } else FsDB.updateUsb(name, usbId, totalSize, freeSize)
        return Usb(usbRootFile, name, totalSize, freeSize, usbId, popCount)
    }

    /**
     * 使用U盘根目录获取UsbId
     */
    private fun getUsbId(rootFile: File): String {
        logger.debug("获取U盘ID[${rootFile.absolutePath}][UUIDFile]: ${UsbUtils.getDiskSerialNumberByFile(rootFile)}")
        return UsbUtils.getDiskSerialNumberByFile(rootFile)
//        val f = File(rootFile, ".udtid")
//        if (!f.exists()) {
//            f.createNewFile()
//            f.writeText(System.nanoTime().toString())
//            f.hidden()
//        }
//        var usbId = f.readText()
//        if (usbId.isEmpty()) {
//            usbId = System.nanoTime().toString()
//            f.writeText(usbId)
//        }
//        return usbId
    }
}