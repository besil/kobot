package cloud.bernardinello.kobot.runner

import cloud.bernardinello.kobot.conf.TelegramConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Component
class TelegramRunner(@Autowired val config: TelegramConfig) : TelegramLongPollingBot(), CommandLineRunner {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TelegramRunner::class.java)
    }

    override fun getBotUsername(): String = config.name
    override fun getBotToken(): String = config.token

    override fun onUpdateReceived(update: Update) {
        log.debug("Incoming message...")
        log.trace("chat id: {} - {}", update.message.chatId, update.message.text)
    }

    override fun run(vararg args: String) {
        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(this)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }
}
