package cloud.bernardinello.kobot.services.transport

import cloud.bernardinello.kobot.services.channels.ChannelService
import cloud.bernardinello.kobot.services.memory.MemoryService
import cloud.bernardinello.kobot.utils.InputKobotMessage
import cloud.bernardinello.kobot.utils.OutputKobotMessage
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class TransportService(
    @Autowired val memoryService: MemoryService,
    @Lazy @Autowired val channel: ChannelService
) {
    @Async
    fun handle(chatId: Long, message: String) {
        val input = InputKobotMessage(chatId, message)
        memoryService.handle(input)
    }

    fun handle(message: OutputKobotMessage) {
        channel.send(message)
    }
}