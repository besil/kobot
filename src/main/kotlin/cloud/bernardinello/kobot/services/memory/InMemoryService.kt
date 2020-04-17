package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.EndState
import cloud.bernardinello.kobot.conversation.StartState
import cloud.bernardinello.kobot.layers.InputConversationMessage
import cloud.bernardinello.kobot.layers.InputKobotMessage
import cloud.bernardinello.kobot.layers.OutputConversationMessage
import cloud.bernardinello.kobot.services.conversation.ConversationService
import cloud.bernardinello.kobot.services.transport.TransportService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class InMemoryService(
    @Lazy @Autowired val transportService: TransportService,
    @Lazy @Autowired val conversationService: ConversationService,
    @Autowired val config: BotConfig
) : MemoryService {
    companion object {
        val log = LoggerFactory.getLogger(InMemoryService::class.java)
    }

    val startState: StartState = config.startState
    val cache: MutableMap<Long, MemoryData> = mutableMapOf()

    @Async
    override fun handle(outputMessage: OutputConversationMessage) {
        log.trace("Updating data for ${outputMessage.chatId}")

        val newMemory = outputMessage.memory
        val chatId = outputMessage.chatId
        if (newMemory.state is EndState) {
            log.trace("Removing session data for: ${outputMessage.chatId}")
            cache.remove(outputMessage.output.chatId)
        } else {
            cache[chatId] = newMemory
        }
        transportService.handle(outputMessage.output)
    }

    @Async
    override fun handle(message: InputKobotMessage) {
        log.trace("Receiving {}", message)
        if (!cache.containsKey(message.chatId)) {
            log.trace("Create new session for ${message.chatId}")
            cache[message.chatId] = MemoryData(startState, SessionData())
        }
        val input = InputConversationMessage(message.chatId, message, cache[message.chatId]!!)

        conversationService.handle(input)
    }
}