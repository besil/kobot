package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.StartState
import cloud.bernardinello.kobot.conversation.StaticExpectedValues
import cloud.bernardinello.kobot.conversation.WaitForInputState
import cloud.bernardinello.kobot.services.memory.MemoryService
import cloud.bernardinello.kobot.services.memory.SessionData
import io.kotlintest.shouldBe
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

    }

}