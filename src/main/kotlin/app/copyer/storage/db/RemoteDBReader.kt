package app.copyer.storage.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import logger
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.support.sqlite.SQLiteDialect
import repository.db.FsDB
import java.io.File
import java.util.*

class RemoteDBReader(private val remoteDBFile: File) {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val db = Database.connect(
        "jdbc:sqlite:${remoteDBFile.apply { logger.info("login[RemoteStorageTempDB]Sql: $this") }}",
        dialect = SQLiteDialect()
    )

    /**
     * 读取所有文件信息(文件所属的UsbId@文件的fileId)
     */
    suspend fun readAllFids(): List<String> {
        val fids = LinkedList<String>()
        listAllFsIdWithUsbId {
            fids.add(it)
        }
        return fids
    }

    /**
     * 以回调的方式读取所有文件信息(文件所属的UsbId@文件的fileId)
     */
    private suspend fun listAllFsIdWithUsbId(block: (String) -> Unit) {
        val usbIds = listAllUsbId()
        for (usbId in usbIds) {
            listAllFsId(usbId).forEach {
                block("$usbId @ $it")
            }
        }
    }

    /**
     * 获取所有的UsbId
     */
    private suspend fun listAllUsbId(): List<String> {
        return ioScope.async {
            db.from(FsDB.UsbsTable).select(FsDB.UsbsTable.usbId).map {
                it[FsDB.UsbsTable.usbId]!!.apply {
                    logger.debug("<$remoteDBFile>UsbId: $this")
                }
            }
        }.await()
    }

    /**
     * 获取指定UsbId下的所有FileId
     */
    private suspend fun listAllFsId(usbId: String): List<String> {
        return ioScope.async {
            val table = FsDB.UsbFilesTable(usbId)
            db.from(table).select(table.fileId).map { it[table.fileId]!! }
        }.await()
    }
}