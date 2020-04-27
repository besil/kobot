package cloud.bernardinello.kobot.runner

import cloud.bernardinello.kobot.services.channels.TelegramService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
@ConditionalOnProperty(value = ["telegram.bot.name", "telegram.bot.token"])
class TelegramRunner(
    @Autowired val telegramService: TelegramService
) : CommandLineRunner {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TelegramRunner::class.java)
    }

    override fun run(vararg args: String) {
        log.info("Starting Telegram runner")
        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(telegramService)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}
