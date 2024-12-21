package app.configer

import WorkDir
import app.actionpack.ActionExecutor
import app.actionpack.EventListener
import logger
import model.ActionPack
import utils.UsbUtils.getUsb
import utils.listenerUsbBeenRemoved
import utils.newFile
import java.io.File

class Configure(private val usbRootFile: File) {
    private val usb = getUsb(usbRootFile)
    val basicConfigDir = File(usbRootFile, ".udtManager")
    val configDir = File(basicConfigDir, "config_${WorkDir.serviceConfig.id}")
    val dbFile = File(configDir, "fs.db")
    val storageDir = File(configDir, "storage")
    val serviceFile = File(configDir, "serviceConfig.json")
    val actionFile = File(configDir, "action.json")

    //need Copy?
    suspend fun main(): Boolean {
        logger.debug("Configure-USB: check basicConfigDir exists status")
        if (!basicConfigDir.exists()) {
            logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> basicConfigDir is not exists, skip configure")
            return true
        }

        logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}>")
        if (!configDir.exists()) {
            logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> create config dir")
            configDir.mkdirs()
        }

        WorkDir.serviceConfig.fsDBFile.copyTo(dbFile, overwrite = true)
        logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> copy fs.db to $configDir")

        if (!storageDir.exists()) {
            logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> create storage dir")
            storageDir.mkdirs()
        }

        if (serviceFile.exists()) {
            val newConfig = serviceFile.readText()
            val originalConfig = WorkDir.serviceConfig.configFile.readText()
            if (newConfig!= originalConfig) {
                logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> update new serviceConfig.json")
                serviceFile.copyTo(WorkDir.serviceConfig.configFile, overwrite = true)
            }
        } else {
            WorkDir.serviceConfig.configFile.copyTo(serviceFile, overwrite = true)
            logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> copy serviceConfig.json to $configDir")
        }

        //action.json需要管理软件创建
        if (actionFile.exists()) {
            logger.debug("Configure-USB: <${usbRootFile}><${usb.usbId}> read action.json")
            val tmpActionFile = WorkDir.serviceConfig.tempDir.newFile("action.json").apply {
                writeText(actionFile.readText())
            }
            val actionPackFormUsb = ActionPack.loadAction(tmpActionFile)
            val actionFormLocal = WorkDir.serviceConfig.getAction()
            if (actionPackFormUsb != actionFormLocal) {
                tmpActionFile.copyTo(WorkDir.serviceConfig.actionFile, overwrite = true)
                ActionExecutor.reloadAction()
            }
        }

//        WorkDir.currentManagerUDisks[usb.usbId] = usb to this
        ActionExecutor.eventListenerFlow.emit(EventListener.WhenManagerUDiskMounted(usb, this))
        listenerUsbBeenRemoved(usb.root) {
//            WorkDir.currentManagerUDisks.remove(usb.usbId)
            ActionExecutor.eventListenerFlow.emit(EventListener.WhenManagerUDiskUnmounted(usb.usbId))
        }
        return false
    }
}