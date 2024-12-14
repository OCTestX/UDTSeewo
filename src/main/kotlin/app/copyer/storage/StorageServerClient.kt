package app.copyer.storage

import WorkDir
import app.copyer.storage.db.RemoteDBReader
import kotlinx.coroutines.*
import logger
import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import repository.DBFileManager
import repository.Http
import repository.db.FsDB
import utils.*
import java.io.File
import java.io.InterruptedIOException
import java.nio.charset.Charset
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

//client是以":"分割的,前面是host,后面是port
//object StorageServerClient: ApiClient(WorkDir.serviceConfig.remoteServerAddress) {
object StorageServerClient {
    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out java.security.cert.X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = emptyArray()
    })
    private val sslContext = SSLContext.getInstance("SSL")
    init {
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
    }

    //从服务器获取的所有fid
    private var tmpAllFids: MutableList<String> = mutableListOf()

    fun run() {
        ioScope.launch {
            while (true) {
                try {
                    main()
                    tmpAllFids.clear()
                    //等待1秒
                    delay(1000)
                } catch (e: Throwable) {
                    logger.error("StorageServerClient执行到一半中途崩溃", e)
                }
            }
        }
    }
    private suspend fun main() = CoroutineScope(Dispatchers.IO).async {
        logger.debug("StorageServerClient-main")
        val asyncRemoteFids: Deferred<List<String>> = async {
            if (tmpAllFids.isEmpty()) {
                //下载db文件
                val result = retractableWebRequest {
                    Http.downloadDBClient.newCall(Requests.downloadDB).execute()
                }
                if (result.code == 200) {
                    //临时存储到一个文件中
                    val target = requestTempFile("_fs.db")
                    result.body!!.byteStream().autoTransferTo(target.outputStream())
                    val remoteDB = RemoteDBReader(target)
                    logger.debug("远程db文件下载完成")
                    try {
                        //读取文件信息,便于下面的排除法获取需要上传的文件
                        tmpAllFids.clear()
                        remoteDB.readAllFids().apply {
                            // 有点晦涩难懂,这是复制字符串
                            for (str in this) {
                                tmpAllFids.add(str)
                            }
                        }
                    } catch (e: Throwable) {
                        logger.error("readAllFids:" + e.stackTraceToString())
                        throw e
                    }
                } else {
                    logger.info("无法下载远程db文件(${result.code}): $result")
                    throw IllegalStateException("无法下载远程db文件(${result.code}): $result")
                }
            } else {
                logger.info("跳过重新读取数据库")
                tmpAllFids
            }
        }
        //读取本地的文件信息
        val localFids = mutableListOf<String>()
        FsDB.listAllFsIdWithUsbId {
            localFids.add(it)
        }
        //等待远程信息读取完成
        val remoteFids = asyncRemoteFids.await()
//        //仅供Debug
//        println("LOCAL: " + localFids.joinToString(", "))
//        println("REMOTE:" + remoteFids.joinToString(", "))
        //排除法
        localFids.removeAll(remoteFids)
        //剩下的都是需要上传的文件
        for (fid in localFids) {
            retryable(finish = {
                tmpAllFids.remove(fid)
            }) {
                //分解文件信息
                val (usbId, fileId) = fid.split(" @ ")
                if (DBFileManager.getFile(usbId, fileId).exists().not()) return@retryable
                //上传出错会中断，下面不会被执行
                retractableWebRequest {
                    uploadFile(usbId, fileId)
                }
                //删除已经上传的文件
                DBFileManager.deleteUsbFile(usbId, fileId)
                val f1 = FsDB.getFile(usbId, fileId)
                if (f1 != null) {
                    logger.info("因为文件已经上传完毕,所以删除: $usbId : $fileId [${f1.name}, ${fileSize(f1.size)}]")
                } else {
                    logger.error("[未知文件]因为文件已经上传完毕,所以删除: $usbId : $fileId")
                }
            }
        }
        //清空缓存
        tmpAllFids.clear()
        logger.debug("StorageServerClient-main finish!")
    }.await()

    private suspend fun <R: Any?> retractableWebRequest(limit: Int = 25, block: suspend  () -> R): R {
        var retry = 0
        var currentError: Throwable? = null
        while (retry <= limit) {
            try {
                //这里有可能发生异常
                return block()
            } catch (e: InterruptedIOException) {
                currentError = e
                retry ++
                logger.warn("只是连接中断,重试: $retry")
            } catch (e: Exception) {
                currentError = e
                retry ++
                logger.warn("发生错误!!!,重试: $retry", e)
            }
        }
        logger.error("多次尝试依然失败!")
        throw currentError!!
    }


    /**
     * 上传文件数据
     */
    private suspend fun preUploadFile(fileId: String, file: File) = CoroutineScope(Dispatchers.IO).async {
        //用来断点续传,获取上传起点
        val size = getPreUploadFileDataStartOff(fileId)
        //如果起点>=文件大小,则被认定为上传完成
        if (size >= file.length()) return@async
        //没有上传完成,就找到临时存储的tmp目录
        val tempFile = WorkDir.serviceConfig.tempDir.linkFile(file.name)
        //重构缓存
        if (tempFile.exists()) tempFile.delete()
        tempFile.createNewFile()
        //这里是关键,文件的缓存会从需要的文件数据位置开始构建
        file.transferFileSkipPartTo(tempFile, size)
        //开始上传文件数据
        preUploadFileData(fileId, tempFile)
    }

    /**
     * 这才是核心函数
     * 先上传文件数据
     * 再上传文件信息
     */
    private suspend fun uploadFile(usbId: String, fileId: String) {
        //获取复制的文件
        val file = DBFileManager.getFile(usbId, fileId)
        //提前上传文件数据
        retractableWebRequest {
            preUploadFile(fileId, file).await()
        }
        //获取数据库文件
        val sourceFile = FsDB.getFile(usbId, fileId)!!
        val path = sourceFile.path
        logger.info("uploadFile: $usbId, $fileId, ${sourceFile.dirId}")
        //上传文件信息,包括usbId,大小等,还会等待服务器的返回码响应
        val request = Requests.uploadFile(usbId, sourceFile.path, fileId, file.length(), sourceFile.createTime, sourceFile.dirId, File(path))
        val result = retractableWebRequest {
            Http.client.newCall(request).execute()
        }
        logger.info(result.code.toString())
        //读取返回码,对症下药
        when(result.code) {
            200 -> logger.debug("文件远程上传成功(${sourceFile.path})[${sourceFile.size}: ${fileSize(sourceFile.size)}]")
            201 -> logger.debug("文件远程上传成功2(${sourceFile.path})[已存在于云端?这里不应该出现,算法出错了!]")
            770 -> {
                logger.debug("文件远程上传失败(${sourceFile.path})[UsbId无效,需要提供Usb信息与从该文件出发到u盘根目录的文件夹信息,准备修复]")
                fixUsb(usbId)
                fixDir(usbId, sourceFile.path)
                logger.debug("准备再次文件远程上传(${sourceFile.path})")
                uploadFile(usbId, fileId)
            }
            771 -> {
                logger.debug("文件远程上传失败(${sourceFile.path})[父目录Id无效,需要提供从该文件出发到u盘根目录的文件夹信息,准备修复]")
                fixDir(usbId, sourceFile.path)
                logger.debug("准备再次文件远程上传(${sourceFile.path})")
                uploadFile(usbId, fileId)
            }
            772 -> {
                logger.debug("文件远程上传失败(${sourceFile.path})没有预先上传文件")
                preUploadFile(fileId, file).await()
                logger.debug("准备再次文件远程上传(${sourceFile.path})")
                uploadFile(usbId, fileId)
            }
            else -> {
                logger.debug("文件远程上传异常(${sourceFile.path})Code: ${result.code}, result: $result")
            }
        }
    }
    /**
     * 获取fileId上传起点
     * 不应该单独调用,preUploadFile函数回处理好这些事
     */
    private suspend fun getPreUploadFileDataStartOff(fileId: String): Long {
        //告诉服务器,请求上传fileId的起点
        val request = Requests.getPreUploadFileDataStartOff(fileId)
        val response = retractableWebRequest {
            Http.client.newCall(request).execute()
        }
        val size = response.body!!.string().toLong()
        logger.info("文件起点是: $size: ${fileSize(size)}")
        return size
    }

    /**
     * 上传文件(中的数据,因为传入的文件是已经到达指定位置的缓存,所有可用被看成完整文件上传)
     * 不应该单独调用,preUploadFile函数回处理好这些事
     */
    private suspend fun preUploadFileData(fileId: String, file: File): Boolean {
        val request = Requests.preUploadFileData(fileId, file)
        val response = retractableWebRequest {
            Http.createUploadFileDataClient(file.length()).newCall(request).execute()
        }
        if (response.isSuccessful) {
            logger.debug("文件远程预先上传成功(${file.path})[上传了: ${file.length()}: ${fileSize(file.length())}]")
            return true
        } else {
            logger.debug("文件远程预先上传失败(${file.path}, ${response.code})")
            return false
        }
    }
    private suspend fun fixUsb(usbId: String) {
        val usb = FsDB.getUsb(usbId)!!
        val request = Requests.fixUsb(usb.name, usb.totalSize, usb.freeSize, usb.usbId, usb.popCount)
        val result = retractableWebRequest {
            Http.client.newCall(request).execute()
        }
        when(result.code) {
            200 -> logger.debug("UsbId远程修复成功($usb)")
            201, 202 -> logger.debug("UsbId远程修复成功2($usb)[已存在于云端?这里不应该出现,算法出错了!]")
            else -> throw IllegalStateException("UsbId远程修复失败($usb)[$result]")
        }
    }
    private suspend fun fixDir(usbId: String, dirPath: String) {
        //获取父目录
        val steps = dirPath.split("/").dropLast(1)
        logger.info("STEPS: $steps")
        //由内向外的修复路径
        val dirPaths = mutableListOf<String>()
        for (i in steps.indices) {
            //倒序遍历出path
            val path = steps.dropLast(i).joinToString("/")
            if (path.isNotEmpty()) {
                logger.info("---------->$path")
                dirPaths.add(path)
            } else {
                //遇到根了
                logger.info("遇到根了---------->/")
                dirPaths.add("/")
            }
        }

        logger.info("倒序遍历完成")
        for (path in dirPaths) {
            logger.info("---------->path: $path")
            //获取对应目录的dirId
            val dir = FsDB.getDirByPath(usbId, path)!!
            //修复
            val request = Requests.fixParentDir(usbId, dir.path, dir.dirId, dir.parentDirId)
            val result = retractableWebRequest {
                Http.client.newCall(request).execute()
            }
            when(result.code) {
                200 -> {
                    logger.debug("ParentDirId远程修复成功($usbId)[$$dirPath]")
                    return
                }
                201 -> {
                    logger.debug("ParentDirId远程修复成功2($usbId)[已存在于云端?这里不应该出现,算法出错了!]")
                    return
                }
                //没修好,继续向内修复
                770 -> logger.debug("ParentDirId远程修复暂时失败($path),ParentId不存在(这时你需要先创建它的父文件夹信息,再回头创建这个文件夹信息)")
                else -> throw IllegalStateException("ParentDirId远程修复失败($usbId)[$result]")
            }
        }
        throw IllegalStateException("ParentDirId远程修复失败,无法找到合适的Dir($usbId)[path: $dirPath] [$dirPaths]")
    }
    //将请求封装成一个函数
    private object Requests{
        val downloadDB = Request.Builder().url(WorkDir.serviceConfig.remoteServerAddress + "/dbDownloadLeast").get().build()

        fun getPreUploadFileDataStartOff(fileId: String): Request {
            logger.debug("StorageServerClient调用getPreUploadFileDataStartOff[$fileId]")
            return Request.Builder().url(WorkDir.serviceConfig.remoteServerAddress + "/getPreUploadFileDataStartOff").headers(
                Headers.headersOf(
                    "fileId" , fileId,
                )
            ).get().build()
        }
        fun preUploadFileData(fileId: String, file: File): Request {
            logger.debug("preUploadFileData[$fileId, $file]")
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("application/octet-stream".toMediaType()))
                .build()

            val request = Request.Builder()
                .url(WorkDir.serviceConfig.remoteServerAddress + "/preUploadFileData")
                .headers(
                    Headers.headersOf(
                        "fileId", fileId
                    )
                )
                .post(requestBody)
                .build()
            return request
        }
        fun uploadFile(usbId: String, fpath: String, fileId: String, size: Long, createTime: Long, parentDirId: String, file: File): Request {
            logger.debug("StorageServerClient调用uploadFile[$usbId, $fpath, $fileId, $size, $createTime, $parentDirId, $file]")
            val base64Fpath = fpath.toKCode64()
            return Request.Builder().url(WorkDir.serviceConfig.remoteServerAddress + "/uploadFile").headers(
                Headers.headersOf(
                    "usbId" , usbId,
                    "fpath" , base64Fpath,
                    "fileId" , fileId,
                    "size" , size.toString(),
                    "createTime" , createTime.toString(),
                    "parentDirId" , parentDirId
                )
            ).get().build()
        }
        @OptIn(ExperimentalEncodingApi::class)
        fun fixUsb(name: String, totalSize: Long, freeSize: Long, usbId: String, popCount: Int): Request {
//            val base64 = Base64.encode("".toByteArray(Charset.forName("UTF-8")))
//            Base64.decode(base64).toString(Charset.forName("UTF-8"))
            val nameBase64 = Base64.encode(name.toByteArray(Charset.forName("UTF-8")))
            return Request.Builder().url(WorkDir.serviceConfig.remoteServerAddress + "/fixUsbTable").headers(
                Headers.headersOf(
                    //TODO()需要使用编码
                    "nameBase64" , nameBase64,
                    "totalSize" , totalSize.toString(),
                    "freeSize" , freeSize.toString(),
                    "usbId" , usbId,
                    "popCount" , popCount.toString(),
                )
            ).get().build()
        }
        fun fixParentDir(usbId: String, path: String, dirId: String, parentDirId: String): Request {
            logger.info("$dirId -> $parentDirId")
            return Request.Builder().url(WorkDir.serviceConfig.remoteServerAddress + "/fixParentDir").headers(
                Headers.headersOf(
                    "usbId" , usbId,
                    "path" , path,
                    "dirId" , dirId,
                    "parentDirId" , parentDirId,
                )
            ).get().build()
        }
    }
}