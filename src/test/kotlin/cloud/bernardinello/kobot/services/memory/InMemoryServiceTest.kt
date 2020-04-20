package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.EndState
import cloud.bernardinello.kobot.conversation.StartState
import cloud.bernardinello.kobot.services.conversation.ConversationService
import cloud.bernardinello.kobot.services.transport.TransportService
import cloud.bernardinello.kobot.utils.InputKobotMessage
import cloud.bernardinello.kobot.utils.OutputConversationMessage
import cloud.bernardinello.kobot.utils.OutputKobotMessage
import io.kotest.core.spec.style.AnnotationSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class InMemoryServiceTest : StringSpec() {
    lateinit var transportService: TransportService
    lateinit var conversationService: ConversationService
    lateinit var botConfig: BotConfig

    @AnnotationSpec.BeforeEach
    fun setUp() {
        transportService = mockk(relaxUnitFun = true)
        conversationService = mockk(relaxUnitFun = true)
        botConfig = mockk()
        every { botConfig.startState } returns StartState("start")
    }

    init {
        setUp()

        "memory service should add chatId as default key in session data" {
            val memoryService = InMemoryService(transportService, conversationService, botConfig)
            memoryService.handle(InputKobotMessage(1, "ciao"))
            memoryService.cache.containsKey(1) shouldBe true

            val memoryData = memoryService.cache.get(1)!!
            memoryData.sessionData.contains("chatId") shouldBe true
        }

        "A memory service should delete cache when a END state is met" {
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