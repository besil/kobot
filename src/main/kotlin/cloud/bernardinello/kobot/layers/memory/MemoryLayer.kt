package cloud.bernardinello.kobot.layers.memory

import akka.actor.ActorRef
import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.EndState
import cloud.bernardinello.kobot.conversation.StartState
import cloud.bernardinello.kobot.layers.*
import cloud.bernardinello.kobot.services.memory.MemoryData
import cloud.bernardinello.kobot.services.memory.NoMemoryData
import cloud.bernardinello.kobot.services.memory.SessionData
import org.slf4j.Logger
import org.slf4j.LoggerFactory


@Suppress("UNUSED_PARAMETER")
@Deprecated("Use services")
class InMemoryLayer(config: BotConfig) : KobotActor() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(InMemoryLayer::class.java)
    }

    var conversationLayers: MutableSet<ActorRef> = mutableSetOf()
    var transportLayers: MutableSet<ActorRef> = mutableSetOf()
    val startState: StartState = config.startState
    val cache: MutableMap<Long, MemoryData> = mutableMapOf()

    fun onReceive(message: InputKobotMessage) {
        log.trace("Receiving {}", message)
        if (!cache.containsKey(message.chatId)) {
            log.trace("Create new session for ${message.chatId}")
            cache[message.chatId] = MemoryData(startState, SessionData())
        }
        val input = InputConversationMessage(message.chatId, message, cache[message.chatId]!!)

        if (conversationLayers.isNotEmpty())
            conversationLayers.random().tell(input, self)
        else
            log.warn("No conversation layer installed, unable to send message {}", input)
    }

    fun onReceive(message: OutputConversationMessage) {
        log.trace("Updating data for ${message.chatId}")

        val newMemory = message.memory
        val chatId = message.chatId
        if (newMemory.state is EndState) {
            log.trace("Removing session data for: ${message.chatId}")
            cache.remove(message.output.chatId)
        } else {
            cache[chatId] = newMemory
        }

        if (transportLayers.isNotEmpty())
            transportLayers.random().tell(message.output, self)
        else log.warn("No transportation layer installed, unable to send message {}", message.output)
    }

    fun onReceive(get: GetMemory) {
        val output = cache[get.chatId]
        log.trace("Requesting ${get.chatId} from cache. Content: ${output}")
        if (output != null)
            sender.tell(output, self)
        else
            sender.tell(NoMemoryData(get.chatId), self)
    }

    fun onReceive(message: RemoveLayer) = run {
        conversationLayers.remove(sender)
        transportLayers.remove(sender)
    }

    fun onReceive(message: AddConversationLayer) = conversationLayers.add(sender)
    fun onReceive(message: AddTransportLayer) = transportLayers.add(sender)
}
