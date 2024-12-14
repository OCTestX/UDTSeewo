package repository

import WorkDir
import model.DBUsbFile
import repository.db.FsDB
import utils.File
import java.io.File
import java.io.InputStream

object DBFileManager {
    fun getInputStream(dbUsbFile: DBUsbFile): InputStream {
        return getFile(dbUsbFile).inputStream()
    }
    fun getFile(dbUsbFile: DBUsbFile): File {
        return getFile(dbUsbFile.usbId, dbUsbFile.fileId)
    }
    fun getFile(usbId: String, fileId: String): File = File(WorkDir.serviceConfig.storageDir, usbId, fileId)

    /**
     * 直接删除USB
     */
    fun deleteUsb(usbId: String) {
        File(WorkDir.serviceConfig.storageDir, usbId).deleteRecursively()
    }

    /**
     * 直接删除USB中的目录
     */
    suspend fun deleteUsbDir(usbId: String, dirId: String) {
        val files = FsDB.listFileInParentDir(usbId, dirId)
        files.forEach {
            deleteUsbFile(usbId, it.fileId)
        }
    }

    /**
     * 直接删除文件
     */
    fun deleteUsbFile(usbId: String, fileId: String) {
        getFile(usbId, fileId).delete()
    }
}