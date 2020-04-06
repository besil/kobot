package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.BotConfigParserException
import cloud.bernardinello.kobot.utils.KobotParser
import cloud.bernardinello.kobot.utils.LogUtils
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class KobotParserTest : StringSpec() {

    init {
        LogUtils.setLogLevel(default = "info")

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