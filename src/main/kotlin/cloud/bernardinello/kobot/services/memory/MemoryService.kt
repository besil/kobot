package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.utils.InputKobotMessage
import cloud.bernardinello.kobot.utils.OutputConversationMessage


interface MemoryService {
    fun handle(message: InputKobotMessage)
    fun handle(message: OutputConversationMessage)
}