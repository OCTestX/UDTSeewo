package utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

val usbListener = AtomicBoolean(true)
suspend fun UsbListener(result: suspend (File) -> Unit) {
    when(getSystemType()) {
        SystemType.Win -> {
            var driver = File.listRoots()
            while (true) {
                val tmp = File.listRoots()
                //新增了设备
                if (tmp.size > driver.size) {
                    tmp.filter { it !in driver }.forEach {
                        //TODO U盘根目录有".skipUDT"文件将不会进行任何操作
                        if (File(it, ".skipUDT").exists().not()){
                            usbListener.waitBeTrue()
                            result(it)
                        }
                    }
                }
                driver = tmp
                delay(100)
            }
        }
        SystemType.Linux -> TODO()
        SystemType.Mac -> TODO()
    }
}
fun switchUsbListenerStatus(status: Boolean) {
    TODO()
}
fun getUsbListenerStatus() = usbListener.get()

suspend fun listenerUsbBeenRemoved(usbPath: File, removed: suspend () -> Unit) {
    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch {
        while (true) {
            if (usbPath.exists().not()) {
                removed()
                break
            }
            delay(800)
        }
    }
}