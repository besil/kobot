package cloud.bernardinello.kobot.layers

import cloud.bernardinello.kobot.services.memory.MemoryData


//sealed class KobotMessage(val chatId: Long)

// Input
data class InputKobotMessage(val chatId: Long, val text: String)
data class InputConversationMessage(val chatId: Long, val input: InputKobotMessage, val memory: MemoryData)

// Output
data class OutputKobotMessage(val chatId: Long, val messages: List<String>, val choices: List<String> = listOf())
data class OutputConversationMessage(val chatId: Long, val output: OutputKobotMessage, val memory: MemoryData)

// Utilities
object RemoveLayer

object AddConversationLayer
object AddMemoryLayer
object AddTransportLayer

class GetMemory(val chatId: Long)