package cloud.bernardinello.kobot.layers.transport

import akka.actor.ActorRef
import akka.actor.UntypedAbstractActor
import cloud.bernardinello.kobot.conf.TelegramConfig
import cloud.bernardinello.kobot.layers.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

@Deprecated("Use services")
class CliTransportLayer : UntypedAbstractActor() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CliTransportLayer::class.java)
    }

    var memoryLayers: MutableList<ActorRef> = mutableListOf()

    override fun onReceive(message: Any?) {
        when (message) {
            "memory" -> {
                memoryLayers.add(sender)
            }
            "start" -> {
                print("> ")
                val input: String = readLine()!!
                memoryLayers.forEach { it.tell(InputKobotMessage(1L, input), self) }
            }
            is OutputKobotMessage -> {
                log.trace("Outputting message: ${message.messages}")
                message.messages.forEach { println(it) }
                message.choices.forEach { println("- $it") }
                self.tell("start", self)
            }
            else -> log.debug("Unknown message received: {}", message)
        }
    }
}

@Deprecated("Use services")
class MyTelegramBot(val config: TelegramConfig, val observer: ActorRef) : TelegramLongPollingBot() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(MyTelegramBot::class.java)
    }

    init {
        val botsApi = TelegramBotsApi()
        try {
            botsApi.registerBot(this)
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    override fun getBotUsername(): String = config.name
    override fun getBotToken(): String = config.token

    override fun onUpdateReceived(update: Update) {
        log.debug("Incoming message...")
        observer.tell(update, observer)
    }
}

@Deprecated("Use services")
@Suppress("UNUSED_PARAMETER")
class TelegramTransportLayer : KobotActor() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(TelegramTransportLayer::class.java)
    }

    var memoryLayers: MutableSet<ActorRef> = mutableSetOf()
    val bots: MutableSet<MyTelegramBot> = mutableSetOf()

    fun onReceive(update: Update) {
        log.trace("Received update!")
        val chatId: Long = update.message.chatId
        val text: String = update.message.text
        val input = InputKobotMessage(chatId, text)
        if (memoryLayers.isNotEmpty())
            memoryLayers.random().tell(input, self)
        else log.warn("No memory layer installed, unable to send message {}", input)
    }

    fun onReceive(outMex: OutputKobotMessage) {
        val chatId: Long = outMex.chatId
        val finalMex: String = outMex.messages.joinToString("\n")
        val finalChoices: List<String> = outMex.choices

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
        bots.forEach { it.execute(sendMessage) }
        log.trace("Message sent!")
    }

    fun onReceive(bot: MyTelegramBot) = bots.add(bot)
    fun onReceive(addMemory: AddMemoryLayer) = memoryLayers.add(sender)
    fun onReceive(remove: RemoveLayer) = memoryLayers.remove(sender)
}