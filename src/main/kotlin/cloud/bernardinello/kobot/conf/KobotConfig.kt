package cloud.bernardinello.kobot.conf

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.utils.KobotParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths

@Configuration
class KobotConfig {
    companion object {
        val log = LoggerFactory.getLogger(KobotConfig::class.java)
    }

    @Bean
    fun botConfig(@Value("\${conversation.path}") conversationPath: String): BotConfig {
        log.info("Creating a bot config from conversation file: {}", conversationPath)
        return KobotParser.parse(Paths.get(conversationPath))
    }

    @Bean
    fun telegramConfig(@Value("\${bot.name}") name: String, @Value("\${bot.token}") token: String): TelegramConfig {
        log.debug("Creating a bot config: {} - {}", name, token)
        return TelegramConfig(name, token)
    }

}