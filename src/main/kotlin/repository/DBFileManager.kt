package repository

import WorkDir
import model.DBUsbFile
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
     * 直接删除文件
     */
    fun deleteFile(usbId: String, fileId: String) {
        getFile(usbId, fileId).delete()
    }
}