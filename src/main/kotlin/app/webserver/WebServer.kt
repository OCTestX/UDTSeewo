package app.webserver

import logger
import repository.DBFileManager
import repository.db.FsDB
import utils.autoTransferTo
import utils.packResult
import utils.toGson
import java.io.InputStream

object WebServer {
    fun run() {
//        embeddedServer(Netty, port = WorkDir.serviceConfig.serverPort, host = "0.0.0.0", module = { module() })
//            .start(wait = true)
    }
//    //每个U盘文件页中文件的数量
//    private const val DefaultSplitPageCount = 350
//    //@OptIn(ExperimentalSerializationApi::class)
//    @OptIn(DelicateCoroutinesApi::class)
//    fun Application.module() {
//        install(WebSockets)
////    install(ContentNegotiation) {
////        protobuf()
////    }
//        routing {
//            //获取所有U盘
//            get("/usbs/all") {
//                logger.info("Routing: /usbs/all")
//                val result = packResult {
//                    Result.success(FsDB.getUsbItems())
//                }
//                callback(result)
//            }
//            //获取目录下的文件
//            get("/files/{usbId}/listFiles") {
//                val result = packResult {
//                    val usbId = call.parameters["usbId"]
//                    val dirPath = call.request.headers["dirPath"]
//                    logger.info("Routing: /files/{$usbId}/listFiles <DirPath{$dirPath}>")
//                    call.requestParmeters("usbId" to usbId, "dirPath" to dirPath)
//                    val fs = FsDB.listFiles(usbId!!, dirPath!!)
//                    Result.success(fs)
//                }
//                callback(result)
//            }
//            //获取目录下的文件夹
//            get("/files/{usbId}/listDirs") {
//                val result = packResult {
//                    val usbId = call.parameters["usbId"]
//                    val dirPath = call.request.headers["dirPath"]
//                    logger.info("Routing: /files/{$usbId}/listDirs <DirPath{$dirPath}>")
//                    call.requestParmeters("usbId" to usbId, "dirPath" to dirPath)
//                    val fs = FsDB.listDirs(usbId!!, dirPath!!)
//                    Result.success(fs)
//                }
//                callback(result)
//            }
////            // TODO 待测试
////            get("/files/{usbId}/pages/{pageIndex}") {
////                val result = packResult {
////                    val usbId = call.parameters["usbId"]
////                    val pageIndex = call.parameters["pageIndex"]
////                    logger.info("Routing: /files/{$usbId}/pages/{$pageIndex}")
////                    call.requestParmeters("usbId" to usbId, "pageIndex" to pageIndex)
////                    val fs = FsDB.listFiles(usbId!!, pageIndex!!.toInt() * DefaultSplitPageCount, DefaultSplitPageCount)
////                    Result.success(fs)
////                }
////                callback(result)
////            }
////            //获取U盘中所有文件组成的页面数量
////            get("/files/{usbId}/totalPages") {
////                val result = packResult {
////                    val usbId = call.parameters["usbId"]
////                    logger.info("Routing: /files/{$usbId}/totalPages")
////                    call.requestParmeters("usbId" to usbId)
////                    val totalFiles = FsDB.getFilesCount(usbId!!)
////                    Result.success((totalFiles + DefaultSplitPageCount - 1) / DefaultSplitPageCount)
////                }
////                callback(result)
////            }
////            //获取U盘中所有文件的数量
////            get("/files/{usbId}/totalFiles") {
////                val result = packResult {
////                    val usbId = call.parameters["usbId"]
////                    logger.info("Routing: /files/{$usbId}/totalFiles")
////                    call.requestParmeters("usbId" to usbId)
////                    Result.success(FsDB.getFilesCount(usbId!!))
////                }
////                callback(result)
////            }
//            //下载文件
//            //TODO 以后加上断点续传
//            get("/files/{usbId}/downloadFile/{fileId}") {
//                val usbId = call.parameters["usbId"]
//                val fileId = call.parameters["fileId"]
//                call.requestParmeters("usbId" to usbId, "fileId" to fileId)
//                val file = DBFileManager.getFile(usbId!!, fileId!!)
//                if (file.exists()) {
//                    call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name}\"")
//                    call.respondFile(file)
//                } else {
//                    call.respondText("File not found", status = HttpStatusCode.NotFound)
//                }
//            }
////            //<---------------------------------------------------------------------------->
////            //加载聊天列表(ChatSessions)
////            get("/chatSessions/loadChatSession") {
////                val result = packResult {
////                    val userId = call.parameters["userId"]
////                    call.requestParmeters("userId" to userId)
////                    ChatSessionLogic.loadChatSessionsList(userId!!.toInt())
////                }
////                callback(result)
////            }
////            //添加聊天列表
////            get("/chatSessions/addChatSession") {
////                val result = packResult {
////                    val userId = call.parameters["userId"]
////                    val chatSessionId = call.parameters["chatSessionId"]
////                    call.requestParmeters("userId" to userId, "chatSessionId" to chatSessionId)
////                    ChatSessionLogic.addChatSession(userId!!.toInt(), chatSessionId!!.toInt())
////                }
////                callback(result)
////            }
////            //<---------------------------------------------------------------------------->
////            //加载聊天数据(ChatMsg)
////            get("/chatSessions/loadChatMsgs") {
////                val result = packResult {
////                    val chatSessionId = call.parameters["chatSessionId"]
////                    call.requestParmeters("chatSessionId" to chatSessionId)
////                    ChatRecordsLogic.loadChatRecordsList(chatSessionId!!.toInt())
////                }
////                callback(result)
////            }
////            //发送聊天数据
////            get("/chatSessions/sendChatMsg") {
////                val result = packResult {
////                    val content = call.parameters["content"]
////                    val type = call.parameters["type"]
////                    val fromUserId = call.parameters["fromUserId"]
////                    val chatSessionId = call.parameters["chatSessionId"]
////                    call.requestParmeters("content" to content, "type" to type, "fromUserId" to fromUserId, "chatSessionId" to chatSessionId)
////                    val msg = ChatRecordsLogic.storeChatRecord(content!!, type!!, fromUserId!!.toInt(), chatSessionId!!.toInt())
////                    msg.onSuccess {
////                        updateChatMsg(it)
////                    }
////                    msg
////                }
////                callback(result)
////            }
////            //监听聊天数据
////            webSocket("/chatSessions/listenerChatMsgs") {
////                ChatMsgListenerSessions.attachSession(this)
////            }
////            //<---------------------------------------------------------------------------->
////            //上传文件
////            webSocket("/files/uploadFile/{hash}/{onlyId}/{continueUpload}") {
////                val hash = call.parameters["hash"]
////                val onlyId = call.parameters["onlyId"]
////                val continueUpload = call.parameters["continueUpload"] == "true"
////                call.requestParmeters("hash" to hash, "onlyId" to onlyId)
////
////                val (outputStream, closer) = if (continueUpload) {
////                    FilesLogic.appendFile(hash!!, onlyId!!).getOrElse {
////                        logger.e(it.stackTraceToString())
////                        throw it
////                    }
////                } else {
////                    FilesLogic.putFile(hash!!, onlyId!!).getOrElse {
////                        logger.e(it.stackTraceToString())
////                        throw it
////                    }
////                }
////                while (true) {
////                    try {
////                        val receive = incoming.receive()
////                        if (receive !is Frame.Binary) break
////                        val data = receive.data
////                        outputStream.write(data)
////                        outputStream.flush()
////                        if (receive.fin) {
////                            closer(FilesManager.CloseType.Close)
////                            break
////                        }
////                    } catch (e: Throwable) {
////                        logger.e(e.stackTraceToString())
////                        if (!continueUpload) closer(FilesManager.CloseType.Pause)
////                        else closer(FilesManager.CloseType.Pause)
////                        break
////                    }
////                }
////            }
////            //下载文件: ByteChannel
////            get("/files/downloadFile/{hash}/{onlyId}") {
////                val result = packResult {
////                    val hash = call.parameters["hash"]
////                    val onlyId = call.parameters["onlyId"]
////                    call.requestParmeters("hash" to hash, "onlyId" to onlyId)
////                    FilesLogic.getFile(hash!!, onlyId!!)
////                }
////                callbackStream(result.map { it.getInputStream() })
////            }
////            //获取断点续传位置: Long
////            get("/files/continuePutFileStartPosition/{hash}/{onlyId}") {
////                val result = packResult {
////                    val hash = call.parameters["hash"]
////                    val onlyId = call.parameters["onlyId"]
////                    call.requestParmeters("hash" to hash, "onlyId" to onlyId)
////                    FilesLogic.continuePutFileStartPosition(hash!!, onlyId!!)
////                }
////                callback(result)
////            }
////            //获取文件基本信息
////            get("/files/getFileInfo/{hash}/{onlyId}") {
////                val result = packResult {
////                    val hash = call.parameters["hash"]
////                    val onlyId = call.parameters["onlyId"]
////                    call.requestParmeters("hash" to hash, "onlyId" to onlyId)
////                    val file = FilesLogic.getFile(hash!!, onlyId!!).getOrThrow().run {
////                        if (!exits) {
////                            FilesLogic.getTmpFile(hash, onlyId).getOrThrow()
////                        } else this
////                    }
////                    val info = mapOf(
////                        "name" to file.name,
////                        "length" to file.length,
////                        "continuePutFileStartPosition" to FilesLogic.continuePutFileStartPosition(hash, onlyId).getOrThrow(),
////                    )
////                    Result.success(info)
////                }
////                callback(result)
////            }
//        }
//    }
//
//    suspend fun PipelineContext<Unit, ApplicationCall>.callbackBytes(result: Result<ByteArray>) {
//        if (result.isSuccess) {
//            call.respondBytes(status = HttpStatusCode.OK) { result.getOrThrow() }
//        } else {
////        logger.e(result.exceptionOrNull()?.message?:"")
//            call.respondText(result.exceptionOrNull()?.message ?: "Unknown error", status = HttpStatusCode.InternalServerError)
//        }
//    }
//
//    suspend inline fun <reified T> PipelineContext<Unit, ApplicationCall>.callback(result: Result<T>) {
//        if (result.isSuccess) {
//            call.respondText(toGson(result.getOrThrow()), status = HttpStatusCode.OK)
//        } else {
////                logger.e(result.exceptionOrNull()?.message?:"")
//            call.respondText(result.exceptionOrNull()?.message ?: "Unknown error", status = HttpStatusCode.InternalServerError)
//        }
//    }
//
//    suspend fun PipelineContext<Unit, ApplicationCall>.callbackStream(result: Result<InputStream>) {
//        if (result.isSuccess) {
//            call.respondOutputStream {
//                result.getOrThrow().autoTransferTo(this)
//            }
//        } else {
////                logger.e(result.exceptionOrNull()?.message?:"")
//            call.respondText(result.exceptionOrNull()?.message ?: "Unknown error", status = HttpStatusCode.InternalServerError)
//        }
//    }
}