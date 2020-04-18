package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.*
import cloud.bernardinello.kobot.services.memory.MemoryService
import cloud.bernardinello.kobot.services.memory.SessionData
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.mockk.mockk

class ConversationServiceTest : StringSpec() {

    init {
        val config = mockk<BotConfig>(relaxUnitFun = true)
        val memoryService = mockk<MemoryService>(relaxUnitFun = true)

        "checkInput should return on-mismatch message when invalid input is provided" {
            val conversationService = ConversationService(config, memoryService)

            val wfi = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                expectedValues = StaticExpectedValues(listOf("ciao", "mondo"), "Error!")
            )
            val inputCheck: InputCheck = conversationService.checkInput(wfi, "hello")
            inputCheck.valid shouldBe false
            inputCheck.message shouldBe "Error!"
            inputCheck.choices shouldBe listOf("ciao", "mondo")

            val inputCheck2 = conversationService.checkInput(wfi, "ciao")
            inputCheck2.valid shouldBe true
            inputCheck2.message shouldBe ""
            inputCheck2.choices shouldBe listOf()
        }

        "update context should save variables in session fields" {
            val conversationService = ConversationService(config, memoryService)
            val wfi = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                expectedValues = StaticExpectedValues(listOf("ciao", "mondo"), "Error!"),
                sessionField = "foo"
            )

            val context = SessionData()
            conversationService.updateContext(wfi, context, "ciao")
            context.contains("foo") shouldBe true
            context["foo"] shouldBe "ciao"
        }

        "update context on start-state should do nothing" {
            val conversationService = ConversationService(config, memoryService)
            val context = SessionData()
            conversationService.updateContext(StartState("start"), context, "foo")
            context.data.size shouldBe 0
        }

        "visiting a send state" {
            val conversationService = ConversationService(config, memoryService)
            val states = listOf(
                SendMexState("send", "hello world"), SendMexState("2", "ciao mondo")
            )
            val context = SessionData()
            val acc = conversationService.visit(states, context)
            acc.outputMessages.toList() shouldBe listOf("hello world", "ciao mondo")
            acc.choices shouldBe listOf()
            acc.context.data.size shouldBe 0
        }

        "visiting a static wait-for-input state with no session" {
            val conversationService = ConversationService(config, memoryService)
            val states = listOf(
                WaitForInputState(
                    id = "wfi",
                    expectedType = "string",
                    expectedValues = StaticExpectedValues(
                        values = listOf("yes", "no"),
                        onMismatch = "error"
                    ),
                    sessionField = "foo"
                )
            )
            val context = SessionData()
            val acc = conversationService.visit(states, context)
            acc.outputMessages.toList() shouldBe listOf()
            acc.choices shouldBe listOf("yes", "no")
            acc.context.data.size shouldBe 0
        }

        "visiting a session wait-for-input state with no key session should throw exception" {
            val conversationService = ConversationService(config, memoryService)
            val state = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                expectedValues = SessionExpectedValues(
                    key = "foo"
                )
            )
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(SessionData()))
            }.message shouldContain "Session key 'foo' is not present in context data"
        }

        "a session wait-for-input expected values must be a collection" {
            val conversationService = ConversationService(config, memoryService)
            val state = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                expectedValues = SessionExpectedValues(
                    key = "foo"
                )
            )

            var context = SessionData()
            context["foo"] = 1
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session key 'foo' doesn't contain a List: '1' found"

            context["foo"] = "ciao"
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session key 'foo' doesn't contain a List: 'ciao' found"

            context["foo"] = listOf("ciao")
            val acc = conversationService.visit(state, Accumulator(context))
            acc.choices shouldBe listOf("ciao")
        }

    }
}