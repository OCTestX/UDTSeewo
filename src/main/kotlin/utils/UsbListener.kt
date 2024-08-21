package utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicBoolean

val usbListener = AtomicBoolean(true)
suspend fun UsbListener(result: (File) -> Unit) {
    when(getSystemType()) {
        SystemType.Win -> {
            var driver = File.listRoots()
            while (true) {
                val tmp = File.listRoots()
                //新增了设备
                if (tmp.size > driver.size) {
                    tmp.filter { it !in driver }.forEach {
                        //U盘根目录有".skipUDT"文件将不会进行任何操作
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