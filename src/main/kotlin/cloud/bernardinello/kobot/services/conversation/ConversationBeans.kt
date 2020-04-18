package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.services.memory.SessionData
import cloud.bernardinello.kobot.utils.OutputKobotMessage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Accumulator(val session: SessionData) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(Accumulator::class.java)
    }

    val outputMessages = mutableListOf<String>()
    var choices = listOf<String>()

    fun addMessage(text: String = "", choices: List<String> = listOf()) {
        if (text.isNotEmpty())
            outputMessages.add(text)
        if (choices.isNotEmpty())
            this.choices = choices
    }
}

data class InputCheck(val valid: Boolean, val message: String = "", val choices: List<String> = listOf()) {
    fun isNotValid(): Boolean = !valid

    fun kobotMessage(chatId: Long): OutputKobotMessage {
        return OutputKobotMessage(
            chatId,
            messages = listOf(message),
            choices = choices
        )
    }
}