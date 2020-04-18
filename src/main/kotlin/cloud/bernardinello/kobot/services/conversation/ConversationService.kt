package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.*
import cloud.bernardinello.kobot.services.memory.MemoryData
import cloud.bernardinello.kobot.services.memory.MemoryService
import cloud.bernardinello.kobot.services.memory.SessionData
import cloud.bernardinello.kobot.utils.InputConversationMessage
import cloud.bernardinello.kobot.utils.InputKobotMessage
import cloud.bernardinello.kobot.utils.OutputConversationMessage
import cloud.bernardinello.kobot.utils.OutputKobotMessage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.lang.reflect.Method

@Service
class ConversationService(
    @Autowired val config: BotConfig,
    @Lazy @Autowired val memoryService: MemoryService
) {
    companion object {
        val log = LoggerFactory.getLogger(ConversationService::class.java)
    }

    fun visit(state: JdbcReadState, accumulator: Accumulator) {
        log.trace("Visiting a jdbc-read state")
    }

    @Suppress("UNUSED_PARAMETER")
    fun visit(state: StartState, accumulator: Accumulator) {
        log.trace("Visiting a Start State")
    }

    @Suppress("UNUSED_PARAMETER")
    fun visit(state: EndState, accumulator: Accumulator) {
        log.trace("Visiting a End State")
        log.trace("Should clean session...")
    }

    fun visit(state: WaitForInputState, accumulator: Accumulator) {
        log.trace("Visiting a wait for input")
        when (state.expectedValues) {
            is StaticExpectedValues -> accumulator.addMessage(choices = state.expectedValues.values)
        }
    }

    fun visit(state: SendMexState, accumulator: Accumulator) {
        log.trace("Visiting a send-mex")
        val mex = state.message
        val regex = ".*!\\{(.*)}.*".toRegex()

        val outputMex: String =
            if (regex.matches(mex)) {
                log.trace("Found regexp match of session key in message")
                val sessionParamName: String = regex.find(mex)!!.groupValues[1]
                log.trace("Looking for param {} in session data: {}", sessionParamName, accumulator.session)
                val sessionValue = accumulator.session[sessionParamName].toString()

                mex.replace("!{$sessionParamName}", sessionValue)
            } else mex

        log.trace("Adding message: {}", outputMex)
        accumulator.addMessage(text = outputMex)
    }

    fun visit(visitable: BotState, accumulator: Accumulator) {
        log.trace("Visiting a visitable of type: {}", visitable::class)
        try {
            val m: Method = this.javaClass.getMethod("visit", visitable::class.java, Accumulator::class.java)
            log.trace("Invoking method ${m.name} on object ${this::class.simpleName} with parameters types: ${visitable::class.simpleName}")
            m.invoke(this, visitable, accumulator)
        } catch (e: Exception) {
            log.trace("{}", e)
            log.error("Error visiting type: ${visitable::class}: $e")
        }
    }

    @Async
    fun handle(message: InputConversationMessage) {
        val memory: MemoryData = message.memory
        val currentState = memory.state
        val input: InputKobotMessage = message.input
        val chatId = input.chatId

        log.trace("Current state is: {}", currentState.id)
        log.trace("Current session data are: {}", memory.sessionData)
        log.trace("Handling message: {}", input.text)

        // validate input
        // If input is != choices, return with same choices and on-mismatch input

        val inputCheck: InputCheck = checkInput(currentState, input.text)
        if (inputCheck.isNotValid()) {
            val okm: OutputKobotMessage = inputCheck.kobotMessage(chatId)
            val ocm = OutputConversationMessage(chatId, okm, memory)
            memoryService.handle(ocm)
            return
        }

        // if input is correct
        // Update the Context accordingly to current state
        val context: SessionData = updateContext(currentState, memory.sessionData, input.text)
        // get all the states between current and next wait-for-input (or end)
        val states: List<BotState> =
            this.config.statesUntilWait(memory.state, listOf(message.input.text))

        // for each state, perform specific action
        // in case of messages, accumulate them
        val acc = Accumulator(context)
        for (state in states) {
            this.visit(state, acc)
        }

        val okm = OutputKobotMessage(
            chatId,
            acc.outputMessages,
            acc.choices
        )
        val newMemory = MemoryData(states.last(), context)

        val ocm = OutputConversationMessage(chatId, okm, newMemory)
        memoryService.handle(ocm)
        return
    }

    fun updateContext(currentState: BotState, context: SessionData, input: String): SessionData {
        if (currentState is WaitForInputState) {
            log.trace("Checking session-field of this wait-for-input state")
            val sessionField = currentState.sessionField
            if (currentState.sessionField.isNotEmpty()) {
                log.trace("Adding {} to current session with key {}", input, sessionField)
                context[sessionField] = input
            }
        }
        return context
    }

    fun checkInput(state: BotState, input: String): InputCheck {
        log.trace("Looking into: {} {}", state.id, state.type)

        if (state is WaitForInputState) {
            val expectedValues = state.expectedValues

            if (expectedValues is StaticExpectedValues) {
                log.trace("Got static values: {}", expectedValues)
                if (!expectedValues.values.contains(input))
                    return InputCheck(false, expectedValues.onMismatch, expectedValues.values)
            } else
                log.trace("Expected values is: {}. Unhandled for now", expectedValues::class.simpleName)
        }
        log.trace("Returning valid")
        return InputCheck(valid = true)
    }
}