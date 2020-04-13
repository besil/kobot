package cloud.bernardinello.kobot.services

import cloud.bernardinello.kobot.conversation.BotConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class KobotService(@Autowired val botConfig: BotConfig) {
    fun foo() = println("Bot config is: $botConfig")
}