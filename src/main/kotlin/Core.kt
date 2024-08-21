import mu.KotlinLogging
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.appender.ConsoleAppender
import org.apache.logging.log4j.core.appender.FileAppender
import org.apache.logging.log4j.core.config.AppenderRef
import org.apache.logging.log4j.core.config.LoggerConfig
import org.apache.logging.log4j.core.layout.PatternLayout
import org.slf4j.LoggerFactory
import utils.File
import utils.currentTime
import java.nio.charset.Charset

object Core {
    private var inited = false
    fun init() {
        if (!inited) {
            //TODO 初始化
            Thread.setDefaultUncaughtExceptionHandler(Inc.GlobalExceptionHandler)
            //清理temp文件夹
            WorkDir.serviceConfig.tempDir.deleteRecursively()
            WorkDir.serviceConfig.tempDir.mkdirs()
            inited = true
        }
    }
    object Inc {
        object GlobalExceptionHandler : Thread.UncaughtExceptionHandler {
            private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

            override fun uncaughtException(thread: Thread, exception: Throwable) {
                logger.error("The uncaught exception in thread ${thread.name}: ${exception.message}", exception)
            }
        }
    }
}

val logger by lazy {
    val ctx = LogManager.getContext(false) as LoggerContext
    val config = ctx.configuration

    val layout = PatternLayout.newBuilder()
        .withCharset(Charset.forName("UTF-8"))
        .withConfiguration(config)
        .withPattern("%d %p %c{1.} [%t] %m%n")
        .build()

    val appender = ConsoleAppender.newBuilder()
        .setName("Console")
        .setImmediateFlush(true)
        .setLayout(layout)
        .build()

    appender.start()
    config.addAppender(appender)


//    val policy1 = TimeBasedTriggeringPolicy.newBuilder()
//        .withModulate(true)
//        .withInterval(1)
//        .build()

//    val policy2 = SizeBasedTriggeringPolicy.createPolicy("1MB")

    val fileAppender = FileAppender.newBuilder()
        .setName("fileAppender")
        .setImmediateFlush(true)
        .withFileName(File(WorkDir.serviceConfig.logsDir, "$currentTime.log").absolutePath)
//        .withFilePattern("log/application.log.%d{MMddyyyy}")
        .setLayout(layout)
//        .withPolicy(policy1)
//        .withPolicy(policy2)
        .build()

    fileAppender.start()
    config.addAppender(fileAppender)


    val ref1 = AppenderRef.createAppenderRef("Console", null, null)
    val ref2 = AppenderRef.createAppenderRef("fileAppender", null, null)
    val refs = listOf(ref1, ref2).toTypedArray()
    val loggerConfig1 = LoggerConfig.createLogger(false, Level.DEBUG, "UDT",
    "true", refs, null, config, null)
//    val loggerConfig2 = LoggerConfig.createLogger(false, Level.DEBUG, "ee.ff.gg.hh",
//    "true", refs, null, serviceConfig, null)
    loggerConfig1.addAppender(appender, null, null)
    loggerConfig1.addAppender(fileAppender, null, null)
//    loggerConfig2.addAppender(appender, null, null)
//    loggerConfig2.addAppender(fileAppender, null, null)
    config.addLogger("UDT", loggerConfig1)
//    serviceConfig.addLogger("ee.ff.gg.hh", loggerConfig2)
    ctx.updateLoggers()
    KotlinLogging.logger("UDT")
}