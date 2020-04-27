package cloud.bernardinello.kobot.conf

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

data class TelegramConfig(val name: String, val token: String)

@Configuration
@ConditionalOnProperty(value = ["telegram.bot.name", "telegram.bot.token"])
class TelegramConfiguration {
    @Bean
    fun telegramConfig(
        @Value("\${telegram.bot.name}") name: String,
        @Value("\${telegram.bot.token}") token: String
    ) = TelegramConfig(name, token)
}

