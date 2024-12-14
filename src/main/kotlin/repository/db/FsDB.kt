package repository.db

import WorkDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import logger
import model.*
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import org.ktorm.support.sqlite.SQLiteDialect
import utils.*
import java.io.File

object FsDB {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val db = Database.connect(
        "jdbc:sqlite:${WorkDir.serviceConfig.fsDBFile.apply { logger.info("loginSql: $this") }}",
        dialect = SQLiteDialect()
    )

    //DebugFlag
    private val InsertFileDebug = true

    private val rootDirId = "DIR-ID-Root"

    /**
     * get root dir
     */
    fun getUsbRootDir(usbId: String): DBDir {
        return db.from(DirsTable).select().where {
            //root path
            (DirsTable.usbId eq usbId) and (DirsTable.path eq "/")
        }.map {
            //I promise, the usb root dir is exited.
            //Because before invoke this method, invoked <insertNewUsb>, the <insertNewUsb> will register the root dir.
            DBDir(it[DirsTable.usbId]!!, it[DirsTable.path]!!, it[DirsTable.dirId]!!, it[DirsTable.parentDirId]!!, it[DirsTable.path]!!.split("/").last())
        }.first()
    }

    /**
     * 读取DBDir,如果不存在,会返回null
     */
    fun getDirById(dirId: String): DBDir? {
        return db.from(DirsTable).select().where {
            //dirId已经足够查到唯一一个了
//            DirsTable.usbId eq usbId
            DirsTable.dirId eq dirId
        }.map {
            DBDir(it[DirsTable.dirId]!!, it[DirsTable.path]!!, it[DirsTable.dirId]!!, it[DirsTable.parentDirId]!!, it[DirsTable.path]!!.split("/").last())
        }.firstOrNull()
    }

    /**
     * 读取DBDir,如果不存在,会返回null
     */
    fun getDirByPath(usbId: String, path: String): DBDir? {
        return db.from(DirsTable).select().where {
            //dirId已经足够查到唯一一个了
            (DirsTable.usbId eq usbId) and (DirsTable.path eq path)
        }.map {
            DBDir(usbId, it[DirsTable.path]!!, it[DirsTable.dirId]!!, it[DirsTable.parentDirId]!!, it[DirsTable.path]!!.split("/").last())
        }.firstOrNull()
    }
    /**
     * 读取DBDir,如果不存在,会创建,如果创建失败,会返回null
     */
    fun getDir(usbId: String, clearPath: String, parentDirId: String): DBDir? {
        val result = db.from(DirsTable).select().where {
            (DirsTable.usbId eq usbId) and (DirsTable.path eq clearPath)
        }.map { DBDir(usbId, it[DirsTable.path]!!, it[DirsTable.dirId]!!, it[DirsTable.parentDirId]!!, it[DirsTable.path]!!.split("/").last()) }
        if (result.size == 1) {
            return result[0]
        } else if (result.isEmpty()) {
            val parentResult = db.from(DirsTable).select().where {
                (DirsTable.usbId eq usbId) and (DirsTable.dirId eq parentDirId)
            }.map { }
            if (parentResult.size == 1) {
                logger.debug("创建: $clearPath")
                val dirId = "DIR-ID-" + getOnlyId()
                db.insert(DirsTable) {
                    set(DirsTable.usbId, usbId)
                    set(DirsTable.path, clearPath)
                    set(DirsTable.dirId, dirId)
                    set(DirsTable.parentDirId, parentDirId)
                }
                return DBDir(usbId, clearPath, dirId, parentDirId, clearPath.split("/").last())
            } else if (parentResult.isEmpty()) {
                return null
            } else throw IllegalStateException("根据父Id($parentDirId)创建DirId时发现父Id违反了sql约束条件(usbId: $usbId)")
        } else throw IllegalStateException("判断(${clearPath})是否存在时发现违反了sql约束条件(usbId: $usbId)")
    }

    /**
     * 只能读取dir的DirId值,如果不存在,会创建,如果创建失败,会返回null
     */
    fun getDirId(usbId: String, clearPath: String, parentDirId: String): String? {
        return getDir(usbId, clearPath, parentDirId)?.dirId
    }

    /**
     * 首先搬运到Storage
     * 之后插入数据库
     */
    fun insertFile(usb: Usb, file: File, parentDirId: String, config: UsbConfig) {
        val fpath = toFpath(usb.root, file)
        val fileId = getOnlyId()
        insertFileByInputStream(file, usb.usbId, fpath, parentDirId, fileId, config.copySpeed)
    }

    /**
     * @param sourceFile 可以是任意路径下的文件,关键文件信息由参数提供,以此,它可以作为StorageServer接收上传文件的存储
     */
    private fun insertFileByInputStream(sourceFile: File, usbId: String, fpath: String, parentDirId: String, fileId: String, speed: Long = Long.MAX_VALUE) {
        if (InsertFileDebug) logger.debug("基本信息准备(fpath:< $fpath >, fileId: < $fileId >, parentDirId: < $parentDirId >)")

        //先以.开头表示临时文件
        var targetFile = File(WorkDir.serviceConfig.storageDir, usbId, ".$fileId")

        if (InsertFileDebug) logger.debug("临时文件(targetFile:< $targetFile >)")

        //TODO 以后可以针对某些U盘设置不同的速度
        sourceFile.autoTransferTo(targetFile, speed, true)

        if (InsertFileDebug) logger.debug("临时文件已存储完毕(targetFile:< $targetFile >)[${targetFile.length().fileSize} / ${sourceFile.length().fileSize}]")

        //最后替换文件名.表示完成文件
        targetFile = targetFile.rename(fileId)

        if (InsertFileDebug) logger.debug("临时文件已替换成正式文件(targetFile:< $targetFile >)[${targetFile.length().fileSize}]")

        val table = UsbFilesTable(usbId)

        if (InsertFileDebug) logger.debug("预备插入数据库: ${table.tableName}")

        val createTime = System.nanoTime()

//        val parentDirPath = fpath.split("/").dropLast(1).joinToString("/").run { ifEmpty { "/" } }
//        val parentDirId = getDirId(usbId,parentDirPath).dirId

        db.insert(table) {
            set(it.fpath, fpath)
            set(it.fileId, fileId)
            set(it.size, sourceFile.length())
            set(it.createTime, createTime)
            set(it.parentDirId, parentDirId)
        }

        if (InsertFileDebug) logger.debug("插入数据库完成: ${table.tableName} (fileId: < $fileId >, createTime: $createTime)")
    }

//    private fun getDirId(usbId: String, dirPath: String): DBDir {
//        logger.debug("getDirId($usbId, $dirPath)")
//        var path = dirPath
//        var parentDirId = "-1"
//        val addDirs = mutableListOf<String>()
//        while (true) {
//            val dir = db.from(DirsTable).select().where {
//                DirsTable.usbId eq usbId
//                DirsTable.path eq path
//            }.map {
//                val dbpath = it[DirsTable.path]!!
//                val name = dbpath.split("/").last()
//                DBDir(it[DirsTable.usbId]!!, dbpath, it[DirsTable.dirId]!!, it[DirsTable.parentDirId]!!, name)
//            }.firstOrNull()
//            if (dir == null) {
//                logger.debug("$path 不存在")
//                if (path.isNotEmpty()) {
//                    logger.debug("$path 加入待创建列表")
//                    addDirs.add(path)
//                }
//                if (path.contains("/") && path != "/") {
//                    path = path.split("/").dropLast(1).joinToString("/")
//                    if (path.isEmpty()) path = "/"
//                } else break
//            } else if (addDirs.isEmpty()) return dir else {
//                logger.debug("更新parentDirId $parentDirId")
//                parentDirId = dir.dirId
//                break
//            }
//        }
//        if(addDirs.isNotEmpty()) {
//            addDirs.reverse()
//            for (addDir in addDirs) {
//                logger.debug("创建: $addDir")
//                val dirId = "DIR-ID-" + getOnlyId()
//                db.insert(DirsTable) {
//                    set(DirsTable.usbId, usbId)
//                    set(DirsTable.path, addDir)
//                    set(DirsTable.dirId, dirId)
//                    set(DirsTable.parentDirId, parentDirId)
//                }
//                parentDirId = dirId
//            }
//            return getDirId(usbId, dirPath)
//        }
//        throw RuntimeException("找不到父dir")
//    }

    suspend fun listAllFsIdWithUsbId(block: (String) -> Unit) {
        val usbIds = listAllUsbId()
        for (usbId in usbIds) {
            listAllFsId(usbId).forEach {
                block("$usbId @ $it")
            }
        }
    }
    private suspend fun listAllUsbId(): List<String> {
        return ioScope.async {
            db.from(UsbsTable).select(UsbsTable.usbId).map { it[UsbsTable.usbId]!! }
        }.await()
    }
    private suspend fun listAllFsId(usbId: String): List<String> {
        return ioScope.async {
            val table = UsbFilesTable(usbId)
            db.from(table).select(table.fileId).map { it[table.fileId]!! }
        }.await()
    }
//    suspend fun listFiles(usbId: String, dirPath: String): List<DBUsbFile> {
//        return ioScope.async {
//            val dirId = getDirId(usbId, dirPath).dirId
//            val table = UsbFilesTable(usbId)
//            db.from(table).select().where {
//                table.parentDirId eq dirId
//            }.map {
//                val path = fromFpath(it[table.fpath]!!)
//                DBUsbFile(
//                    usbId,
//                    path.split("/").last(),
//                    path,
//                    it[table.fileId]!!,
//                    it[table.size]!!,
//                    it[table.createTime]!!,
//                    it[table.parentDirId]!!
//                )
//            }
//        }.await()
//    }
//    suspend fun listDirs(usbId: String, dirPath: String): List<DBDir> {
//        return ioScope.async {
//            val dirId = getDirId(usbId, dirPath).dirId
//            val table = DirsTable
//            db.from(table).select().where {
//                table.parentDirId eq dirId
//            }.map {
//                val dbpath = it[DirsTable.path]!!
//                val name = dbpath.split("/").last()
//                DBDir(usbId, it[DirsTable.path]!!, it[table.dirId]!!, it[table.parentDirId]!!, name)
//            }
//        }.await()
//    }
////
////    /**
////     * 一次性加载的数据太多!!!!
////     * 获取数据库储存的文件
////     */
////    private suspend fun listFiles(usbId: String): List<DBUsbFile> {
////        return ioScope.async {
////            val table = UsbFilesTable(usbId)
////            db.from(table).select().map {
////                val path = fromFpath(it[table.fpath]!!)
////                DBUsbFile(
////                    usbId,
////                    path.split("/").last(),
////                    path,
////                    it[table.fileId]!!,
////                    it[table.size]!!,
////                    it[table.createTime]!!,
////                    it[table.parentDirId]!!
////                )
////            }
////        }.await()
////    }
    /**
     * 一次性加载的数据太多!!!
     * 获取数据库储存的文件path
     */
    suspend fun listFilesPath(usbId: String): List<String> {
        return ioScope.async {
            val table = UsbFilesTable(usbId)
            db.from(table).select().map {
                it[table.fpath]!!
            }
        }.await()
    }

    suspend fun listFileInParentDir(usbId: String, parentDirId: String): List<DBUsbFile> {
        return ioScope.async {
            val table = UsbFilesTable(usbId)
            db.from(table).select().where {
                table.parentDirId eq parentDirId
            }.map {
                transformToDBUsbFile(usbId, table, it)
            }
        }.await()
    }
//
//    /**
//     * 获取U盘存储的全部文件数量
//     */
//    suspend fun getFilesCount(usbId: String): Int {
//        return ioScope.async {
//            val table = UsbFilesTable(usbId)
//            db.from(table).select().totalRecordsInAllPages
//        }.await()
//    }
//    //TODO 待测试
//    suspend fun listFiles(usbId: String, startOff: Int, limit: Int): List<DBUsbFile> {
//        return ioScope.async {
//            val table = UsbFilesTable(usbId)
//            db.from(table).select().limit(startOff, limit).map {
//                val path = fromFpath(it[table.fpath]!!)
//                DBUsbFile(
//                    usbId,
//                    path.split("/").last(),
//                    path,
//                    it[table.fileId]!!,
//                    it[table.size]!!,
//                    it[table.createTime]!!,
//                    it[table.parentDirId]!!,
//                )
//            }
//        }.await()
//    }
    /**
     * 通过fileId获取文件
     */
    suspend fun getFile(usbId: String, fileId: String): DBUsbFile? {
        return ioScope.async {
            val table = UsbFilesTable(usbId)
            db.from(table).select().where {
                table.fileId eq fileId
            }.map {
                transformToDBUsbFile(usbId, table, it)
            }.firstOrNull()
        }.await()
    }

    /**
     * 在数据库新建一个U盘
     */
    fun insertNewUsb(name: String, usbId: String, totalSize: Long, freeSize: Long) {
        //register a new usb
        db.insert(UsbsTable) {
            set(it.name, name)
            set(it.totalSize, totalSize)
            set(it.freeSize, freeSize)
            set(it.usbId, usbId)
            set(it.popCount, 0)
        }
        //给u盘文件开一个数据表
        db.useConnection {
            it.prepareStatement(
                "CREATE TABLE \"$usbId\" (\n" +
                        "\t\"fpath\"\tTEXT NOT NULL UNIQUE,\n" +
                        "\t\"fileId\"\tTEXT NOT NULL UNIQUE,\n" +
                        "\t\"size\"\tINTEGER NOT NULL,\n" +
                        "\t\"createTime\"\tINTEGER NOT NULL,\n" +
                        "\t\"parentDirId\"\tTEXT NOT NULL,\n" +
                        "\tPRIMARY KEY(\"fpath\")\n" +
                        ")"
            ).execute()
        }
        //给U盘注册一个父目录
        db.insert(DirsTable) {
            set(it.usbId, usbId)
            set(it.path, "/")
            set(it.dirId, "DIR-ID-" + getOnlyId())
            set(it.parentDirId, rootDirId)
        }
    }

    /**
     * 更新数据库的U盘
     */
    fun updateUsb(name: String, usbId: String, totalSize: Long, freeSize: Long) {
        logger.debug("updateUsb($name, $usbId, $totalSize, $freeSize)")
        db.update(UsbsTable) {
            set(it.name, name)
            set(it.totalSize, totalSize)
            set(it.freeSize, freeSize)
            where { UsbsTable.usbId eq usbId }
        }
    }

    fun getUsb(usbId: String): DBUsb? {
        return db.from(UsbsTable).select().where {
            UsbsTable.usbId eq usbId
        }.map {
            DBUsb(
                it[UsbsTable.name]!!,
                it[UsbsTable.totalSize]!!,
                it[UsbsTable.freeSize]!!,
                it[UsbsTable.usbId]!!,
                it[UsbsTable.popCount]!!,
            )
        }.firstOrNull()
    }

    /**
     * 在数据库中读取所有U盘信息
     */
    fun getUsbItems(): List<DBUsb> {
        return db.from(UsbsTable).select().map {
            DBUsb(
                it[UsbsTable.name]!!,
                it[UsbsTable.totalSize]!!,
                it[UsbsTable.freeSize]!!,
                it[UsbsTable.usbId]!!,
                it[UsbsTable.popCount]!!,
            )
        }
    }

    /**
     * 删除数据库中的U盘
     */
    fun deleteUsb(usbId: String) {
        db.deleteAll(UsbFilesTable(usbId))
        db.delete(UsbsTable) {
            it.usbId eq usbId
        }
        db.delete(DirsTable) {
            it.usbId eq usbId
        }
    }

    /**
     * 删除数据库中的U盘的文件夹
     */
    fun deleteUsbDir(usbId: String, dirId: String) {
        db.delete(DirsTable) {
            (it.usbId eq usbId) and (it.dirId eq dirId)
        }
        db.delete(UsbFilesTable(usbId)) {
            it.parentDirId eq dirId
        }
    }

    /**
     * 删除数据库中的U盘的文件夹
     */
    fun deleteUsbFile(usbId: String, parentDirId: String, fileId: String) {
        db.delete(UsbFilesTable(usbId)) {
            (it.parentDirId eq parentDirId) and (it.fileId eq fileId)
        }
    }

    /**
     * 内部使用!!!,标记U盘又一次异常弹出
     */
    fun plusUsbPopException(usbId: String) {
        val usb = getUsbItems().find { it.usbId == usbId }
        if (usb != null) {
            db.update(UsbsTable) {
                set(UsbsTable.popCount, usb.popCount + 1)//原来的数加1
                where { it.usbId eq usbId }
            }
        } else {
            logger.error("plusUsbPopException: usbId: $usbId Not Found!")
        }
    }
    /**
     * 内部使用!!!,标记U盘一次正常弹出
     */
    fun cleanUsbPopException(usbId: String) {
        val usb = getUsbItems().find { it.usbId == usbId }
        if (usb != null) {
            db.update(UsbsTable) {
                set(UsbsTable.popCount, 0)//注意这里是0
                where { it.usbId eq usbId }
            }
        } else {
            logger.error("plusUsbPopException: usbId: $usbId Not Found!")
        }
    }

    class UsbFilesTable(usbId: String): Table<Nothing>(usbId) {
        //注意，这里是经过xbase64编码过的路径，解码后的路径以/分割
        val fpath = varchar("fpath")
        val fileId = varchar("fileId")
        val size = long("size")
        val createTime = long("createTime")
        val parentDirId = varchar("parentDirId")
    }
    object UsbsTable: Table<Nothing>("USB") {
        val name = varchar("name")
        val totalSize = long("totalSize")
        val freeSize = long("freeSize")
        val usbId = varchar("usbId")
        val popCount = int("popCount")
    }
    object DirsTable: Table<Nothing>("Dirs") {
        val usbId = varchar("usbId")
        val path = varchar("path")
        val dirId = varchar("dirId")
        val parentDirId = varchar("parentDirId")
    }
    //DebugFlag
    private const val fpathTransformDebug: Boolean = false

    /**
     * 获取相对路径
     * rootFilePath一定要换成绝对路径
     * sourcePath同理
     * 编码后不包含rootPath,例如root：/Project/UDTService source： /Project/UDTService/jjd/tr， 编码后解码：jjd/tr
     */
    fun toCleanPath(rootFilePath: File, sourcePath: File): String = toCleanPath(rootFilePath.absolutePath, sourcePath.absolutePath)
    @Deprecated("仅供测试使用的内部函数")
    fun toCleanPath(rootFilePath: String, sourcePath: String): String {
        return sourcePath.replace(rootFilePath, "")//获取相对路径第一步
            .plog(fpathTransformDebug) { "获取CleanPath第一步(去除U盘路径): <$sourcePath> -> < $it >" }
            .plog(fpathTransformDebug) { "获取CleanPath第二步(统一linux分割符): < $it > -> < ${it.linux} >" }
            .linux//统一分割符
            .run { "/$this" }//使用"/"作为根目录
    }

    /**
     * 获取相对路径并转换成fpath
     * rootFilePath一定要换成绝对路径
     * sourcePath同理
     * 编码后的fpath解码出来不包含rootPath,例如root：/Project/UDTService source： /Project/UDTService/jjd/tr， 编码后解码：jjd/tr
     */
    fun toFpath(rootFilePath: File, sourcePath: File): String = toFpath(rootFilePath.absolutePath, sourcePath.absolutePath)
    @Deprecated("仅供测试使用的内部函数")
    fun toFpath(rootFilePath: String, sourcePath: String): String {
        return toCleanPath(rootFilePath, sourcePath)
//            .plog(fpathTransformDebug) { "获取FPath第三步(KCode64转换): < $it > -> < ${it.toKCode64()} >" }
//            .toKCode64()//转换
    }

    /**
     * 将fpath转换成相对路径
     */
    fun fromFpath(fpath: String): String {
        return fpath
//            .fromKCode64()
            .plog(fpathTransformDebug) { "获取相对路径: < $fpath > -> < $it >" }
    }

    fun transformToDBUsbFile(usbId: String,usbFilesTable: UsbFilesTable, rowSet: QueryRowSet): DBUsbFile {
        val path = fromFpath(rowSet[usbFilesTable.fpath]!!)
        return DBUsbFile(
            usbId,
            path.split("/").last(),
            path,
            rowSet[usbFilesTable.fileId]!!,
            rowSet[usbFilesTable.size]!!,
            rowSet[usbFilesTable.createTime]!!,
            rowSet[usbFilesTable.parentDirId]!!,
        )
    }
}