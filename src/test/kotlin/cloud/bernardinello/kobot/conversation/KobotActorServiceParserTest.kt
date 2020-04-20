package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.BotConfigParserException
import cloud.bernardinello.kobot.utils.KobotParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.string.shouldContain

class KobotActorServiceParserTest : StringSpec() {

    init {
        "A malformed state json should throw exception" {
            shouldThrow<BotConfigParserException> {
                KobotParser.parse(
                    """
                        {"id"="foo", "type":"start"}
                    """.trimIndent()
                ) as BotConfig
            }.message.shouldContain("Json {\"id\"=\"foo\", \"type\":\"start\"} is malformed")
        }

        "A state parameter error should throw a ConfigException, not ParserException" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"id": "", "type": "start"
                    |}""".trimMargin()
                ) as BotState
            }
        }
    }

}