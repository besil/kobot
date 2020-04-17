package cloud.bernardinello.kobot

import akka.testkit.javadsl.TestKit
import cloud.bernardinello.kobot.conf.DatabaseConfig
import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.layers.*
import cloud.bernardinello.kobot.layers.memory.MemoryData
import cloud.bernardinello.kobot.layers.memory.NoMemoryData
import cloud.bernardinello.kobot.layers.memory.SessionData
import cloud.bernardinello.kobot.layers.transport.MyTelegramBot
import cloud.bernardinello.kobot.services.KobotActorService
import cloud.bernardinello.kobot.utils.KobotParser
import io.kotlintest.TestCase
import io.kotlintest.specs.StringSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove

class KobotActorServiceTest : StringSpec() {
    lateinit var kobotActorService: KobotActorService

    companion object {
        val log: Logger = LoggerFactory.getLogger(KobotActorServiceTest::class.java)
    }

    override fun beforeTest(testCase: TestCase) {
        super.beforeTest(testCase)
        val config: BotConfig = KobotParser.parse(
            """{
            |"states" : [
            |{"id": "start", "type": "start"},
            |{"id": "send-mex", "type": "send-mex", "message": "hello"},
            |{"id": "end", "type": "end"}
            |],
            |"relationships": [
            |{"from": "start", "to": "send-mex"},
            |{"from": "send-mex", "to": "end"}
            |]
            |}""".trimMargin()
        )
        kobotActorService = KobotActorService(config, DatabaseConfig("", "", ""))
    }

    init {
        "simple behaviour test" {
            kobotActorService.transportLayer.tell(AddMemoryLayer, kobotActorService.memoryLayer)
            kobotActorService.memoryLayer.tell(AddConversationLayer, kobotActorService.conversationLayer.first())
            kobotActorService.memoryLayer.tell(AddTransportLayer, kobotActorService.transportLayer)

            val update = mockk<Update>()
            every { update.message.chatId } returns 1L
            every { update.message.text } returns "Hello"

            val myBot = mockk<MyTelegramBot>()
            val mex = SendMessage(1, "hello")
            mex.replyMarkup = ReplyKeyboardRemove()
            every { myBot.execute(mex) } answers { nothing }

            kobotActorService.transportLayer.tell(myBot, kobotActorService.transportLayer)

            kobotActorService.transportLayer.tell(update, kobotActorService.transportLayer)
            verify(timeout = 3000) { myBot.execute(mex) }
        }

        "A memory layer should save the current state" {
            val input = InputKobotMessage(1L, "ciao")
            kobotActorService.memoryLayer.tell(input, kobotActorService.transportLayer)
            val probe = TestKit(kobotActorService.system)
            log.debug("Probing...")
            kobotActorService.memoryLayer.tell(GetMemory(1L), probe.ref)
            probe.expectMsg(MemoryData(kobotActorService.config.startState, SessionData()))
        }

        "A memory layer should delete a state when END state is met" {
            val input = InputKobotMessage(1L, "ciao")
            kobotActorService.memoryLayer.tell(input, kobotActorService.transportLayer)

            val output =
                OutputConversationMessage(
                    1L,
                    OutputKobotMessage(1L, listOf()),
                    MemoryData(kobotActorService.config.endState)
                )
            kobotActorService.memoryLayer.tell(output, kobotActorService.conversationLayer.first())

            val probe = TestKit(kobotActorService.system)
            log.debug("Probing...")
            kobotActorService.memoryLayer.tell(GetMemory(1L), probe.ref)
            probe.expectMsg(NoMemoryData(1L))
        }
    }
}