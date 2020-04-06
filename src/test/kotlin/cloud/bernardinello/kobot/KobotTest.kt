package cloud.bernardinello.kobot

import akka.testkit.javadsl.TestKit
import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.layers.*
import cloud.bernardinello.kobot.layers.memory.MemoryData
import cloud.bernardinello.kobot.layers.memory.NoMemoryData
import cloud.bernardinello.kobot.layers.memory.SessionData
import cloud.bernardinello.kobot.layers.transport.MyTelegramBot
import cloud.bernardinello.kobot.utils.KobotParser
import cloud.bernardinello.kobot.utils.LogUtils
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

class KobotTest : StringSpec() {
    lateinit var kobot: Kobot

    companion object {
        val log: Logger = LoggerFactory.getLogger(KobotTest::class.java)
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
        kobot = Kobot(config)
    }

    init {
        LogUtils.setLogLevel(default = "info")

        "simple behaviour test" {
            kobot.transportLayer.tell(AddMemoryLayer, kobot.memoryLayer)
            kobot.memoryLayer.tell(AddConversationLayer, kobot.conversationLayer.first())
            kobot.memoryLayer.tell(AddTransportLayer, kobot.transportLayer)

            val update = mockk<Update>()
            every { update.message.chatId } returns 1L
            every { update.message.text } returns "Hello"

            val myBot = mockk<MyTelegramBot>()
            val mex = SendMessage(1, "hello")
            mex.replyMarkup = ReplyKeyboardRemove()
            every { myBot.execute(mex) } answers { nothing }

            kobot.transportLayer.tell(myBot, kobot.transportLayer)

            kobot.transportLayer.tell(update, kobot.transportLayer)
            verify(timeout = 3000) { myBot.execute(mex) }
        }

        "A memory layer should save the current state" {
            val input = InputKobotMessage(1L, "ciao")
            kobot.memoryLayer.tell(input, kobot.transportLayer)
            val probe = TestKit(kobot.system)
            log.debug("Probing...")
            kobot.memoryLayer.tell(GetMemory(1L), probe.ref)
            probe.expectMsg(MemoryData(kobot.config.startState, SessionData()))
        }

        "A memory layer should delete a state when END state is met" {
            val input = InputKobotMessage(1L, "ciao")
            kobot.memoryLayer.tell(input, kobot.transportLayer)

            val output =
                OutputConversationMessage(1L, OutputKobotMessage(1L, listOf()), MemoryData(kobot.config.endState))
            kobot.memoryLayer.tell(output, kobot.conversationLayer.first())

            val probe = TestKit(kobot.system)
            log.debug("Probing...")
            kobot.memoryLayer.tell(GetMemory(1L), probe.ref)
            probe.expectMsg(NoMemoryData(1L))
        }
    }
}