package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.EndState
import cloud.bernardinello.kobot.conversation.StartState
import cloud.bernardinello.kobot.services.conversation.ConversationService
import cloud.bernardinello.kobot.services.transport.TransportService
import cloud.bernardinello.kobot.utils.InputKobotMessage
import cloud.bernardinello.kobot.utils.OutputConversationMessage
import cloud.bernardinello.kobot.utils.OutputKobotMessage
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class InMemoryServiceTest : StringSpec() {

    init {
        "A memory service should delete cache when a END state is met" {
            val transportService = mockk<TransportService>(relaxUnitFun = true)
            val conversationService = mockk<ConversationService>(relaxUnitFun = true)
            val botConfig = mockk<BotConfig>()
            every { botConfig.startState } returns StartState("start")

            val memoryService = InMemoryService(transportService, conversationService, botConfig)

            memoryService.handle(InputKobotMessage(1, "ciao"))
            memoryService.cache.containsKey(1) shouldBe true

            val output = OutputConversationMessage(
                1,
                OutputKobotMessage(1, listOf()),
                MemoryData(EndState("end"))
            )
            memoryService.handle(output)

            memoryService.cache.containsKey(1) shouldBe false
        }
    }

}