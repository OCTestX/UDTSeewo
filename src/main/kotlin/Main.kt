import app.copyer.Copyer
import app.copyer.storage.StorageServerClient
import app.webserver.WebServer
import kotlinx.coroutines.runBlocking
import utils.UsbListener
import kotlin.concurrent.thread

fun main() {
    Core.init()
    runBlocking {
        thread {
            WebServer.run()
        }
//        thread {
//            StorageServerClient.run()
//        }
        UsbListener {
            try {
                Copyer(it).main()
            } catch (e: Throwable) {
                logger.debug("USBListenerBlock:"+e.stackTraceToString())
            }
        }
    }
}
