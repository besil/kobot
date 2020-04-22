package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.instanceOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.slf4j.LoggerFactory

class BotStateTest : StringSpec() {
    companion object {
        val log = LoggerFactory.getLogger(BotStateTest::class.java)
    }

    init {
        "A bot state must always have id and type fields" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {}
                """.trimIndent()
                ) as BotState
            }//.message.shouldContain("State mandatory fields are missing: [id, type]")

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {}
                """.trimIndent()
                ) as StartState
            }//.message.shouldContain("State mandatory fields are missing: [id, type]")

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {}
                """.trimIndent()
                ) as SendMexState
            }//.message.shouldContain("State mandatory fields are missing: [id, message, type]")
        }

        "A bot state id can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {"type": "start"}
                """.trimIndent()
                ) as StartState
            }//.message.shouldContain("State mandatory fields are missing: [id]")

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {"id": "", "type":"start"}
                """.trimIndent()
                ) as StartState
            }//.message.shouldContain("State field 'id' can't be ''")

            shouldThrow<BotConfigException> {
                StartState(id = "")
            }//.message.shouldContain("State field 'id' can't be ''")
        }

        "A bot state type can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {"id": "foo", "type" : ""}
                """.trimIndent()
                ) as BotState
            }//.message.shouldContain("State field type '' is not allowed. Available types are: ${BotState.availableTypes}")
        }

        "A bot state mandatory fields should be" {
            val state: StartState = KobotParser.parse(
                """
                { "id": "foo", "type":"start" }
            """.trimIndent()
            )

            state.id shouldBe "foo"
            state.type shouldBe "start"
            state shouldBe instanceOf(StartState::class)

            val s = StartState(id = "foo")
            s.id shouldBe "foo"
            s.type shouldBe "start"
        }

        "A start state id can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {"id": "", "type": "start"}
                """.trimIndent()
                ) as StartState
            }//.message.shouldContain("State field 'id' can't be ''")

            shouldThrow<BotConfigException> {
                StartState(id = "")
            }//.message.shouldContain("State field 'id' can't be ''")
        }

        "A send-mex state must have message field" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {"id":"send", "type":"send-mex"}
                """.trimIndent()
                ) as SendMexState
            }//.message.shouldContain("State mandatory fields are missing: [message]")
        }

        "A send-mex state message field can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """
                    {"id":"send", "type":"send-mex", "message": ""}
                """.trimIndent()
                ) as SendMexState
            }.message.shouldContain("State field 'message' can't be empty")

            shouldThrow<BotConfigException> {
                SendMexState(id = "send", message = "")
            }.message.shouldContain("State field 'message' can't be empty")


        }

        "A send-mex mandatory fields should be" {
            val state: SendMexState = KobotParser.parse(
                """
                {"id": "foo", "type":"send-mex", "message": "foo"}
            """.trimIndent()
            )

            state.id shouldBe "foo"
            state.type shouldBe "send-mex"
            state.message shouldBe "foo"

            val s = StartState(id = "foo")
            s.id shouldBe "foo"
            s.type shouldBe "start"

            val sm =
                SendMexState(id = "send", message = "foo")
            sm.id shouldBe "send"
            sm.type shouldBe "send-mex"
            sm.message shouldBe "foo"
        }

        "State parser should detect inconsistent types" {
            shouldThrow<BotConfigException> {
                val botState: BotState = KobotParser.parse(
                    """
                    {"id": "start", "type": "start", "message": ""}
                """.trimIndent()
                )

                botState shouldBe instanceOf(StartState::class)
                botState shouldNotBe instanceOf(SendMexState::class)

                botState as SendMexState
            }
        }

        "A expected-value state can have predefined keys" {
            // static, type, any
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                |   "type" : "foo"
                |}""".trimMargin()
                ) as StaticExpectedValues
            }
        }

        "A expected-value type can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                |   "type" : ""
                |}""".trimMargin()
                ) as StaticExpectedValues
            }
        }

        "A static expected-value must fail on missing keys" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                |   "type" : "static"
                |}""".trimMargin()
                ) as StaticExpectedValues
            }
        }

        "A static expected-value values can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                |   "type" : "static",
                |   "values": []
                |}""".trimMargin()
                ) as StaticExpectedValues
            }
        }

        "A any expected-value can't have other keys" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"type": "any",
                    |"foo": "bar"
                    |}""".trimMargin()
                ) as AnyExpectedValues
            }
        }

        "A session expected-value can't have empty keys" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"type": "session",
                    |"key": ""
                    |}""".trimMargin()
                ) as SessionExpectedValues
            }

            val sessionExpectedValues: SessionExpectedValues =
                KobotParser.parse(
                    """{
                |"type": "session",
                |"key": "session-field"
                |}""".trimMargin()
                )
            sessionExpectedValues.key shouldBe "session-field"
//            sessionExpectedValues.onMismatch shouldBe "error"
        }

        "A wait-for-input state" {
            val waitForInput: WaitForInputState = KobotParser.parse(
                """{
                |"id": "get-confirm",
                |"type": "wait-for-input",
                |"expected-type": "string",
                |"on-mismatch": "Input not matched. Please use one of the following:",
                |"expected-values": {
                |   "type": "static",
                |   "values": [ "Yes", "No" ]
                |}
                |}""".trimMargin()
            )

            waitForInput.id shouldBe "get-confirm"
            waitForInput.type shouldBe "wait-for-input"
            waitForInput.expectedType shouldBe "string"
            waitForInput.onMismatch shouldBe "Input not matched. Please use one of the following:"
            waitForInput.expectedValues shouldBe instanceOf(ExpectedValues::class)
            waitForInput.expectedValues.type shouldBe "static"
            waitForInput.expectedValues shouldBe instanceOf(StaticExpectedValues::class)
            waitForInput.expectedValues as StaticExpectedValues
            (waitForInput.expectedValues as StaticExpectedValues).values shouldBe listOf("Yes", "No")
        }

        "A wait-for-input state can have or not a session-field set" {
            val wfi: WaitForInputState = KobotParser.parse(
                """{
                |"id": "get-confirm",
                |"type": "wait-for-input",
                |"expected-type": "number",
                |"on-mismatch": "error",
                |"expected-values": {
                |   "type": "any"
                |},
                |"session-field": "sf"
                |}""".trimMargin()
            )

            wfi.sessionField shouldBe "sf"
            wfi.expectedValues shouldBe instanceOf(AnyExpectedValues::class)

            val wfi2: WaitForInputState = KobotParser.parse(
                """{
                |"id": "get-confirm",
                |"type": "wait-for-input",
                |"expected-type": "number",
                |"on-mismatch": "error",
                |"expected-values": {
                |   "type": "any"
                |}
                |}""".trimMargin()
            )

            wfi2.sessionField shouldBe ""
        }

        "A wait-for-input state can have limited expected-type" {
            val availableTypes = listOf("string", "number")
            availableTypes.forEach { type ->
                val wfi: WaitForInputState = KobotParser.parse(
                    """{
                        "id": "get-confirm",
                        "type": "wait-for-input",
                        "expected-type": "$type",
                        "on-mismatch": "error!",
                        "expected-values": {
                           "type": "any"
                        }
                        }"""
                )
                wfi.expectedType shouldBe type
            }

            val wrongTypes = listOf("", "foo", "double", "varchar")
            wrongTypes.forEach { type ->
                shouldThrow<BotConfigException> {
                    KobotParser.parse(
                        """{
                        "id": "get-confirm",
                        "type": "wait-for-input",
                        "expected-type": "$type",
                        "expected-values": {
                           "type": "any"
                        }
                        }"""
                    ) as WaitForInputState
                }
            }
        }
    }
}