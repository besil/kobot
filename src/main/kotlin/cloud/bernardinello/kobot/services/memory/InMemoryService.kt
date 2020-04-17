package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.EndState
import cloud.bernardinello.kobot.conversation.StartState
import cloud.bernardinello.kobot.services.conversation.ConversationService
import cloud.bernardinello.kobot.services.transport.TransportService
import cloud.bernardinello.kobot.utils.InputConversationMessage
import cloud.bernardinello.kobot.utils.InputKobotMessage
import cloud.bernardinello.kobot.utils.OutputConversationMessage
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
    override fun handle(message: OutputConversationMessage) {
        log.trace("Updating data for ${message.chatId}")

        val newMemory = message.memory
        val chatId = message.chatId
        if (newMemory.state is EndState) {
            log.trace("Removing session data for: ${message.chatId}")
            cache.remove(message.output.chatId)
        } else {
            cache[chatId] = newMemory
        }
        transportService.handle(message.output)
    }

    @Async
    override fun handle(message: InputKobotMessage) {
        log.trace("Receiving {}", message)
        if (!cache.containsKey(message.chatId)) {
            log.trace("Create new session for ${message.chatId}")
            cache[message.chatId] = MemoryData(startState, SessionData())
        }
        val input = InputConversationMessage(
            message.chatId,
            message,
            cache[message.chatId]!!
        )

        conversationService.handle(input)
    }
}