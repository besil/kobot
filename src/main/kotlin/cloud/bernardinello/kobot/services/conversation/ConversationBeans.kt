package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.StaticExpectedValues
import cloud.bernardinello.kobot.conversation.WaitForInputState
import cloud.bernardinello.kobot.services.memory.MemoryData
import cloud.bernardinello.kobot.services.memory.SessionData
import cloud.bernardinello.kobot.utils.InputKobotMessage
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

class InputChecker(data: MemoryData, mex: InputKobotMessage) {
    var valid: Boolean = true
    var message: String = ""
    var choices: List<String> = listOf()

    companion object {
        val log: Logger = LoggerFactory.getLogger(InputChecker::class.java)
    }

    init {
        log.trace("Looking into: {} {}", data.state.id, data.state.type)
        when (data.state) {
            is WaitForInputState -> when (data.state.expectedValues) {
                is StaticExpectedValues -> if (!data.state.expectedValues.values.contains(mex.text)) {
                    this.valid = false
                    this.message = data.state.expectedValues.onMismatch
                    this.choices = data.state.expectedValues.values
                }
                else -> log.trace(
                    "Expected values is: {}. Unhandled for now",
                    data.state.expectedValues::class.simpleName
                )
            }
            else -> log.trace("State is: {}. Unhandled for now", data.state::class.simpleName)
        }
    }

    fun isNotValid(): Boolean = !isValid()

    fun isValid(): Boolean = valid
}