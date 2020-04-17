package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.layers.InputKobotMessage
import cloud.bernardinello.kobot.layers.OutputConversationMessage


interface MemoryService {
    fun handle(message: InputKobotMessage)
    fun handle(message: OutputConversationMessage)
}