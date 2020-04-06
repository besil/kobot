package cloud.bernardinello.kobot.utils

import cloud.bernardinello.kobot.conversation.BotConfigException
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Path

class BotConfigParserException(message: String) : Exception(message)

object KobotParser {
    val log: Logger = LoggerFactory.getLogger(KobotParser::class.java)
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    init {
        LogUtils.setLogLevel()
    }

    inline fun <reified T> parse(json: Path): T {
        return parse(json.toFile())
    }

    inline fun <reified T> parse(json: File): T {
        return parse(json.readText())
    }

    inline fun <reified T> parse(json: String): T {
        try {
            log.trace("Parsing json: $json")
            return objectMapper.readValue(json, T::class.java)
        } catch (e: Exception) {
            log.trace("{}", e)

            val ex = when (e) {
                is JsonParseException -> BotConfigParserException("Json $json is malformed")
                else -> BotConfigException(e.message!!)
            }

            log.trace("Throwing: {} - {}", ex::class, e.message)
            throw ex
        }
    }
}
