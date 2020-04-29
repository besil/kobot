package cloud.bernardinello.kobot.conf

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.utils.KobotParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Paths


@Configuration
class KobotConfiguration {
    companion object {
        val log = LoggerFactory.getLogger(KobotConfiguration::class.java)
    }

    @Bean
    fun botConfig(@Value("\${kobot.conversation.path}") kobotConversationPath: String): BotConfig {
        log.info("Loading conversation file from: {}", kobotConversationPath)
        return KobotParser.parse(Paths.get(kobotConversationPath))
    }
}