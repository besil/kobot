package cloud.bernardinello.kobot.services

import cloud.bernardinello.kobot.conf.TelegramConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update

@Service
class TelegramTransportService(
    @Autowired val telegramConfig: TelegramConfig
) : TelegramLongPollingBot() {
    override fun getBotUsername(): String {
        return telegramConfig.name
    }

    override fun getBotToken(): String {
        return telegramConfig.token
    }

    override fun onUpdateReceived(update: Update) {
        TODO("Not yet implemented")
    }

}