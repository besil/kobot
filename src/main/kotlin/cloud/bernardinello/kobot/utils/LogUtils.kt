package cloud.bernardinello.kobot.utils

import ch.qos.logback.classic.Level
import org.slf4j.LoggerFactory

object LogUtils {
    fun setLogLevel(default: String = "trace") {
        val level: ch.qos.logback.classic.Level = when (System.getenv("LOGGING_LEVEL") ?: default) {
            "trace", "TRACE" -> Level.TRACE
            "debug", "DEBUG" -> Level.DEBUG
            "info", "INFO" -> Level.INFO
            "warn", "WARN", "warning", "WARNING" -> Level.WARN
            "error", "ERROR" -> Level.ERROR
            else -> Level.INFO
        }
        val root = LoggerFactory.getLogger("cloud.bernardinello.kobot") as ch.qos.logback.classic.Logger
        root.level = level
    }

}