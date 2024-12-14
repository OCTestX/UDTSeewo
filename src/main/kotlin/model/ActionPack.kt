package model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import logger
import java.io.File

@Serializable
data class ActionPack(
    val actions: List<Action>
) {
    fun toJson(): String {
        return Json.encodeToString(this)
    }
    companion object {
        private fun loadAction(json: String): ActionPack {
            return Json.decodeFromString<ActionPack>(json)
        }

        /**
         * 加载ActionPack，如果文件不存在，则创建空白配置文件
         */
        fun loadAction(actionFile: File): ActionPack {
            return if (actionFile.exists()) {
                try {
                    loadAction(actionFile.readText())
                } catch (e: Throwable) {
                    logger.error("读取配置文件失败: ${e.message}, 尝试重新创建配置文件...")
                    createAction(actionFile)
                }

            } else {
                createAction(actionFile)
            }
        }

        private fun createAction(actionFile: File): ActionPack {
            val actionPack = ActionPack(listOf())
            actionFile.writeText(Json.encodeToString(actionPack))
            return actionPack
        }

        @Serializable
        class Action(
            val id: String,
            val name: String,
            val description: String,
            val eventListener: EventListenerType,
            val work: WorkType,
            val data: List<String?>,
        )
    }
}

@Serializable
enum class EventListenerType {
    Load,
    WhenManagerUDiskMounted,
    WhenManagerUDiskUnmounted,
    WhenCommonUDiskMounted,
    WhenCommonUDiskUnmounted,
}

@Serializable
enum class WorkType {
    NOP,
    ProvideFileToManagerUDisk,
    DeleteDBUsb,
    DeleteDBUsbDir,
    DeleteDBUsbFile,
}

/**
 * 参数由用户提供，内部自动转换
 */
@Serializable
sealed class Work(val type: WorkType) {
    abstract fun toWorkData(): List<String?>
    data class ProvideFileToManagerUDisk(val usbId: String, val parentDirId: String, val fileId: String): Work(WorkType.ProvideFileToManagerUDisk) {
        constructor(data: List<String?>): this(data[0]!!, data[1]!!, data[2]!!)
        override fun toWorkData(): List<String?> = listOf(usbId, parentDirId, fileId)
    }
    data class DeleteDBUsb(val usbId: String): Work(WorkType.DeleteDBUsb) {
        constructor(data: List<String?>): this(data[0]!!)
        override fun toWorkData(): List<String?> = listOf(usbId)
    }

    data class DeleteDBUsbDir(val usbId: String, val dirId: String): Work(WorkType.DeleteDBUsb) {
        constructor(data: List<String?>): this(data[0]!!, data[1]!!)
        override fun toWorkData(): List<String?> = listOf(usbId, dirId)
    }

    data class DeleteDBUsbFile(val usbId: String, val parentDirId: String, val fileId: String): Work(WorkType.DeleteDBUsbFile) {
        constructor(data: List<String?>): this(data[0]!!, data[1]!!, data[2]!!)
        override fun toWorkData(): List<String?> = listOf(usbId, parentDirId, fileId)
    }
}