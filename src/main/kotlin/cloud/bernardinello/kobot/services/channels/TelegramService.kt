package cloud.bernardinello.kobot.services.channels

import cloud.bernardinello.kobot.conf.TelegramConfig
import cloud.bernardinello.kobot.services.transport.TransportService
import cloud.bernardinello.kobot.utils.OutputKobotMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow

@Service
@ConditionalOnProperty(value = ["kobot.telegram.bot.name", "kobot.telegram.bot.token"])
class TelegramService(
    @Autowired val config: TelegramConfig,
    @Lazy @Autowired val transportService: TransportService
) : TelegramLongPollingBot(), ChannelService {
    companion object {
        val log = LoggerFactory.getLogger(TelegramService::class.java)
    }

    override fun getBotUsername(): String = config.name
    override fun getBotToken(): String = config.token

    @Async
    override fun onUpdateReceived(update: Update) {
        log.debug("Incoming message...")
        val chatId = update.message.chatId
        val message = update.message.text
        log.trace("chat id: {} - {}", chatId, message)
        transportService.handle(chatId, message)
    }

    override fun send(message: OutputKobotMessage) {
        val chatId = message.chatId
        val finalMex: String = message.messages.joinToString("\n")
        val finalChoices: List<String> = message.choices

        val km = if (finalChoices.isNotEmpty())
            ReplyKeyboardMarkup(finalChoices.chunked(2).map { pairs ->
                val kr = KeyboardRow()
                kr.addAll(pairs)
                kr
            })
        else ReplyKeyboardRemove()

        log.trace("Preparing message: {}, {}", chatId, finalMex)
        val sendMessage = SendMessage(chatId, finalMex)
        sendMessage.replyMarkup = km
        this.execute(sendMessage)
        log.trace("Message sent!")
    }
}