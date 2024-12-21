import app.configer.Configure
import app.copyer.Copyer
import app.copyer.storage.StorageServerClient
import app.webserver.WebServer
import kotlinx.coroutines.runBlocking
import utils.UsbListener
import kotlin.concurrent.thread

fun main() {
    Core.init()
    runBlocking {
//        thread {
//            WebServer.run()
//        }
//        thread {
//            StorageServerClient.run()
//        }
        UsbListener {
            try {
                val needCopy = Configure(it).main()
                if (needCopy) {
                    logger.info("[Info] USBListenerBlock: USB device connected and isn't usbConfigureUDisk, start copying...")
                    Copyer(it).main()
                }
            } catch (e: Throwable) {
                logger.error("[Error] USBListenerBlock:"+e.stackTraceToString(), e)
            }
        }
    }
}
