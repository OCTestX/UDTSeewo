package app.actionpack

import app.configer.Configure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import logger
import model.*
import repository.DBFileManager
import repository.db.FsDB
import utils.autoTransferTo
import java.io.File

/**
 * @see EventListenerType.Load
 * have executable actions:
 * @see WorkType.DeleteDBUsb
 * @see WorkType.DeleteDBUsbDir
 * @see WorkType.DeleteDBUsbFile
 *
 * @see EventListenerType.WhenManagerUDiskMounted
 * have executable actions:
 * @see WorkType.ProvideFileToManagerUDisk
 *
 */

object ActionExecutor {
    private var actionWorkJob = runAction()
    private val scope = CoroutineScope(Dispatchers.IO)
    val eventListenerFlow = MutableSharedFlow<EventListener>()

    fun reloadAction() {
        actionWorkJob.cancel()
        actionWorkJob = runAction()
    }

    private fun runAction() = scope.launch {
        val actionPack = WorkDir.serviceConfig.getAction()
        val actions = actionPack.actions

        launch(Dispatchers.IO) {
            eventListenerFlow.collect { eventListener ->
                try {
                    when (eventListener) {
                        is EventListener.Load -> {
                            actions.filter { it.eventListener == eventListener.type }.forEach { action ->
                                if (action.work == WorkType.DeleteDBUsb) {
                                    val work = Work.DeleteDBUsb(action.data)
                                    work.start()
                                } else if (action.work == WorkType.DeleteDBUsbDir) {
                                    val work = Work.DeleteDBUsbDir(action.data)
                                    work.start()
                                } else if (action.work == WorkType.DeleteDBUsbFile) {
                                    val work = Work.DeleteDBUsbFile(action.data)
                                    work.start()
                                }
                            }
                        }
                        is EventListener.WhenManagerUDiskMounted -> {
                            actions.filter { it.eventListener == eventListener.type }.forEach { action ->
                                if (action.work == WorkType.ProvideFileToManagerUDisk) {
                                    val work = Work.ProvideFileToManagerUDisk(action.data)
                                    work.start(eventListener.usb, eventListener.usbConfigure)
                                }
                            }
                        }
                        is EventListener.WhenManagerUDiskUnmounted -> {

                        }

                        is EventListener.WhenCommonUDiskMounted -> {

                        }
                        is EventListener.WhenCommonUDiskUnmounted -> {

                        }
                    }
                } catch (e: Throwable) {
                    logger.error("Error while running action: $eventListener", e)
                }
            }
        }

        launch(Dispatchers.IO) {
            eventListenerFlow.emit(EventListener.Load)
        }
    }
}



fun Work.ProvideFileToManagerUDisk.start(managerUDisk: Usb, managerUDiskConfigure: Configure) {
    if (managerUDisk.root.exists()) {
        val storageDir = managerUDiskConfigure.storageDir.apply { mkdirs() }
        val parentDir = File(storageDir, parentDirId).apply { mkdirs() }
        val targetFile = File(parentDir, fileId)
        val fromFile = DBFileManager.getFile(usbId, fileId)
        fromFile.autoTransferTo(targetFile, append = true)
    }
}

fun Work.DeleteDBUsb.start() {
    DBFileManager.deleteUsb(usbId)
    FsDB.deleteUsb(usbId)
}

suspend fun Work.DeleteDBUsbDir.start() {
    //注意是先删原始文件再删数据库的数据，因为在删除指定目录下的文件时需要通过数据库查询在该文件夹下的文件
    DBFileManager.deleteUsbDir(usbId, dirId)
    FsDB.deleteUsbDir(usbId, dirId)
}

fun Work.DeleteDBUsbFile.start() {
    DBFileManager.deleteUsbFile(usbId, fileId)
    FsDB.deleteUsbFile(usbId, parentDirId, fileId)
}

/**
 * 所有参数均由内部提供
 */
@Serializable
sealed class EventListener(val type: EventListenerType) {
    data object Load : EventListener(EventListenerType.Load)
    data class WhenManagerUDiskMounted(val usb: Usb, val usbConfigure: Configure) : EventListener(EventListenerType.WhenManagerUDiskMounted)
    data class WhenManagerUDiskUnmounted(val usbId: String) : EventListener(EventListenerType.WhenManagerUDiskUnmounted)
    data class WhenCommonUDiskMounted(val usb: Usb) : EventListener(EventListenerType.WhenCommonUDiskMounted)
    data class WhenCommonUDiskUnmounted(val usbId: String) : EventListener(EventListenerType.WhenCommonUDiskUnmounted)
}
