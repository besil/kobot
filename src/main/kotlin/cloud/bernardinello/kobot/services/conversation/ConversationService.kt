package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.*
import cloud.bernardinello.kobot.layers.InputConversationMessage
import cloud.bernardinello.kobot.layers.InputKobotMessage
import cloud.bernardinello.kobot.layers.OutputConversationMessage
import cloud.bernardinello.kobot.layers.OutputKobotMessage
import cloud.bernardinello.kobot.services.memory.MemoryData
import cloud.bernardinello.kobot.services.memory.MemoryService
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
        val input: InputKobotMessage = message.input
        val chatId = input.chatId

        log.trace("Current state is: {}", memory.state.id)
        log.trace("Current session data are: {}", memory.sessionData)
        log.trace("Handling message: {}", input.text)

        val inputCheck = InputChecker(memory, input)
        if (inputCheck.isNotValid()) {
            log.trace("Choice not recognized: ${inputCheck.choices}")
            val okm =
                OutputKobotMessage(chatId, messages = listOf(inputCheck.message), choices = inputCheck.choices)
            val ocm = OutputConversationMessage(chatId, okm, memory)
//            return ocm
            memoryService.handle(ocm)
        } else {
            if (memory.state is WaitForInputState) {
                log.trace("Checking session-field of this wait-for-input state")
                val sessionField = memory.state.sessionField
                if (memory.state.sessionField.isNotEmpty()) {
                    log.trace("Adding {} to current session with key {}", input.text, sessionField)
                    memory.sessionData[sessionField] = input.text
                }
            }

            log.trace("Looking for next wait from: ${memory.state} with choices: ${listOf(message.input.text)}")
            val states: List<BotState> =
                this.config.statesUntilWait(memory.state, listOf(message.input.text))
            log.trace("Traversing states: {}", states.map { "${it.id}:${it.type}" }.toList())

            val acc = Accumulator(memory.sessionData)
            for (state in states) {
                this.visit(state, acc)
            }

            val okm = OutputKobotMessage(chatId, acc.outputMessages, acc.choices)
            val newMemory = MemoryData(states.last(), memory.sessionData)

            val ocm = OutputConversationMessage(chatId, okm, newMemory)
//            return ocm
            memoryService.handle(ocm)
        }
    }
}