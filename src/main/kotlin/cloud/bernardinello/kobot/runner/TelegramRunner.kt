package cloud.bernardinello.kobot.runner

import cloud.bernardinello.kobot.services.channels.TelegramService
import cloud.bernardinello.kobot.services.transport.TransportService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class TelegramRunner(
    @Autowired val telegramService: TelegramService,
    @Autowired val transportService: TransportService
) : CommandLineRunner {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TelegramRunner::class.java)
    }

    override fun run(vararg args: String) {
        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(telegramService)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}
