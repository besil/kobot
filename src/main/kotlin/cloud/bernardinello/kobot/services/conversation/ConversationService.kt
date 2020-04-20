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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.lang.reflect.Method

@Service
class ConversationService(
    @Autowired val config: BotConfig,
    @Autowired val jdbcTemplate: JdbcTemplate,
    @Lazy @Autowired val memoryService: MemoryService
) {
    companion object {
        val log = LoggerFactory.getLogger(ConversationService::class.java)
    }

    fun visit(state: JdbcReadState, accumulator: Accumulator): Accumulator {
        log.trace("Visiting a jdbc-read state: {}", state.id)
        val context = accumulator.context
        val sessionKey = state.sessionField

        val sessionKeys = extractSessionKeys(state.query)
        val sql = replaceSessionKeys(state.query, sessionKeys, context)

        val queryForList: List<Map<String, Any>> = jdbcTemplate.queryForList(sql)
        log.trace("Query list: {}", queryForList)
        val values = queryForList.flatMap { it.values }
        accumulator.context[sessionKey] = values
        return accumulator
    }

    fun visit(state: StartState, accumulator: Accumulator): Accumulator {
        log.trace("Visiting start state: {}", state.id)
        return accumulator
    }

    fun visit(state: EndState, accumulator: Accumulator): Accumulator {
        log.trace("Visiting a end state: {}", state.id)
        log.trace("Should clean session...")
        return accumulator
    }

    fun visit(state: WaitForInputState, accumulator: Accumulator): Accumulator {
        log.trace("Visiting a wait for input")
        when (state.expectedValues) {
            is StaticExpectedValues -> {
                log.trace("Expected values: ${state.expectedValues.values}")
                accumulator.addMessage(choices = state.expectedValues.values)
            }
            is SessionExpectedValues -> {
                // check if key is present
                val key = state.expectedValues.key
                log.trace("Looking expected values key: $key")
                if (key !in accumulator.context)
                    throw ConversationServiceException("Session keys ['$key'] not found in current context")
                // check if it a list or not
                if (accumulator.context[key] !is List<*>)
                    throw ConversationServiceException("Session key '$key' doesn't contain a List: '${accumulator.context[key]}' found")
                log.trace("Expected values: ${accumulator.context[key]}")
                accumulator.addMessage(choices = (accumulator.context[key] as List<*>).map { it.toString() })
            }
        }
        return accumulator
    }

    fun extractSessionKeys(s: String): List<String> {
        val regex = """!\{(.*?)\}""".toRegex()

        if (regex.containsMatchIn(s))
            regex.findAll(s)

        val matches: List<MatchResult> = regex.findAll(s).toList()
        val keys = matches.map { it.groupValues.last() }
        return keys
    }

    fun visit(state: SendMexState, accumulator: Accumulator): Accumulator {
        log.trace("Visiting a send-mex")
        val mex = state.message
        val context = accumulator.context

        val keys = extractSessionKeys(mex)


        val outputMex = replaceSessionKeys(mex, keys, context)

        log.trace("Adding message: {}", outputMex)
        accumulator.addMessage(text = outputMex)
        return accumulator
    }

    private fun replaceSessionKeys(mex: String, keys: List<String>, context: SessionData): String {
        val missingSessionKeys = keys.filter { !context.contains(it) }.toList().sorted()
        if (missingSessionKeys.isNotEmpty()) {
            log.trace("Missing keys: {}", missingSessionKeys)
            throw ConversationServiceException("Session keys $missingSessionKeys not found in current context")
        }

        var outputMex = mex
        keys.forEach {
            outputMex = outputMex.replace("!{$it}", context[it].toString())
        }
        return outputMex
    }

    fun visit(visitable: BotState, accumulator: Accumulator): Accumulator {
        log.trace("Visiting a visitable of type: {}", visitable::class)
        try {
            val m: Method = this.javaClass.getMethod("visit", visitable::class.java, Accumulator::class.java)
            log.trace("Invoking method ${m.name} on object ${this::class.simpleName} with parameters types: ${visitable::class.simpleName}")
            return m.invoke(this, visitable, accumulator) as Accumulator
        } catch (ce: ConversationServiceException) {
            log.warn("Got exception while visiting state: {}", visitable.id)
            log.debug("Current accumulator: {}", accumulator)
            log.debug("Current session: {}", accumulator.context.data)
            log.trace("{}", ce)
            throw ce
        } catch (e: Exception) {
            log.trace("{}", e)
            log.error("Error visiting type: ${visitable::class}: $e")
            return accumulator
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
        val inputCheck: InputCheck = checkInput(currentState, memory.sessionData, input.text)
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
        val states: List<BotState> = this.config.statesUntilWait(memory.state, listOf(message.input.text))

        // for each state, perform specific action
        // in case of messages, accumulate them
        val acc: Accumulator = this.visit(states, context)

        val okm = OutputKobotMessage(
            chatId,
            acc.outputMessages,
            acc.choices
        )
        val newMemory = MemoryData(states.last(), acc.context)

        val ocm = OutputConversationMessage(chatId, okm, newMemory)
        memoryService.handle(ocm)
        return
    }

    fun visit(states: List<BotState>, context: SessionData): Accumulator {
        var acc = Accumulator(context)
        for (state in states) {
            acc = this.visit(state, acc)
        }
        return acc
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

    fun checkInput(state: BotState, context: SessionData, input: String): InputCheck {
        log.trace("Looking into: {} {}", state.id, state.type)

        if (state is WaitForInputState) {
            val expectedValues = state.expectedValues

            when (expectedValues) {
                is StaticExpectedValues -> {
                    log.trace("Got static values: {}", expectedValues)
                    if (!expectedValues.values.map { it.toString() }.contains(input))
                        return InputCheck(false, expectedValues.onMismatch, expectedValues.values)
                }
                is SessionExpectedValues -> {
                    val key = expectedValues.key
                    if (!context.contains(key))
                        throw ConversationServiceException("Session keys ['$key'] not found in current context")
                    if (context[key] !is List<*>)
                        throw ConversationServiceException("Session key '$key' doesn't contain a List: '${context[key]}' found")

                    // do the actual check
                    val values = (context[key] as List<*>).map { it.toString() }
                    if (!values.contains(input))
                        return InputCheck(false, expectedValues.onMismatch, values.map { it.toString() })
                }
                else -> log.trace("Expected values is: {}. Unhandled for now", expectedValues::class.simpleName)
            }
        }
        log.trace("Returning valid")
        return InputCheck(valid = true)
    }
}