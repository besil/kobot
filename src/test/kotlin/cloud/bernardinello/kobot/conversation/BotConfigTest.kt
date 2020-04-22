package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.instanceOf
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BotConfigTest : StringSpec() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(BotConfigTest::class.java)
    }

    init {
        "A bot configuration should be" {
            val config: BotConfig = KobotParser.parse(
                """{
                "states" : [
                    { "id" : "start", "type" : "start"},
                    {"id" : "end","type" : "end"},
                    {"id": "send","type":"send-mex","message": "foo"}
                ],
                "relationships" : [
                    {"from" : "start","to" : "send"},
                    {"from" : "send","to" : "end"}
                ]
            }""".trimIndent()
            ) as BotConfig

            config.states.size shouldBe 3
            config.relationships.size shouldBe 2

            config.states.forEach { println(it::class) }

            config.states[0] shouldBe instanceOf(StartState::class)
            config.states[1] shouldBe instanceOf(EndState::class)
            config.states[2] shouldBe instanceOf(SendMexState::class)
            config.states[2] shouldNotBe instanceOf(StartState::class)
        }

        "Parsing invalid states in a bot config should rise state exception " {
            val e = shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                        "states" : [{
                            "id" : "start",
                            "type" : "asd"
                        },{
                            "id" : "end",
                            "type" : "end"
                        }],
                        "relationships" : [{
                            "from" : "start",
                            "to" : "end"
                        }]
                    }""".trimIndent()
                ) as BotConfig
            }
            e.message shouldContain "Could not resolve type id 'asd'"
        }

        "A minimum config must have a start and an end state, with a path between them" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [],
                    |"relationships": []
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "A bot configuration must have a start state"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "foo", "type": "start"}
                    |],
                    |"relationships": []
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "A bot configuration must have an end state"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "foo", "type": "start"},
                    |   {"id": "bar", "type": "end"}
                    |],
                    |"relationships": []
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "A path between foo and bar must exists"

            val config: BotConfig =
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "foo", "type": "start"},
                    |   {"id": "bar", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "foo", "to": "bar"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig

            config.states.size shouldBe 2
            config.relationships.size shouldBe 1
        }

        "Bot states must be unique" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    "states": [
                       {"id": "foo", "type": "start"},
                       {"id": "foo", "type": "end"}
                    ],
                    "relationships": [
                       {"from": "foo", "to": "bar"}
                    ]
                    }""".trimMargin()
                ) as BotConfig
            }.message shouldContain "State ids [foo] are not unique"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "foo", "type": "start"},
                    |   {"id": "foo", "type": "end"},
                    |   {"id": "bar", "type": "send-mex", "message": "bar"},
                    |   {"id": "bar", "type": "send-mex", "message": "bar"}
                    |],
                    |"relationships": [
                    |   {"from": "foo", "to": "bar"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "State ids [bar, foo] are not unique"
        }

        "Relationships between states must be unique" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "s1", "type": "start"},
                    |   {"id": "s2", "type": "end"},
                    |   {"id": "s3", "type": "send-mex", "message": "bar"},
                    |   {"id": "s4", "type": "send-mex", "message": "bar"}
                    |],
                    |"relationships": [
                    |   {"from": "s1", "to": "s2"},
                    |   {"from": "s1", "to": "s2"},
                    |   {"from": "s3", "to": "s4"},
                    |   {"from": "s3", "to": "s4"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Relationships [s1-s2, s3-s4] are not unique"
        }

        "State ids should match those in relationships" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "send"},
                    |   {"from": "send1", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Relationships contain state ids [send1] which are not defined state ids"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "send"},
                    |   {"from": "send", "to": "send1"},
                    |   {"from": "send2", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Relationships contain state ids [send1, send2] which are not defined state ids"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "foo", "to": "send"},
                    |   {"from": "send", "to": "send1"},
                    |   {"from": "send2", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Relationships contain state ids [foo, send1, send2] which are not defined state ids"
        }

        "All nodes states must be connected and in a path from start to end state" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send1", "type": "send-mex", "message": "ciao"},
                    |   {"id": "send2", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "send1"},
                    |   {"from": "send1", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "The following states are not connected with start or end state: [send2]"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send1", "type": "send-mex", "message": "ciao"},
                    |   {"id": "send2", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "The following states are not connected with start or end state: [send1, send2]"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "end", "type": "end"},
                    |   {"id": "send1", "type": "send-mex", "message": "ciao"},
                    |   {"id": "send2", "type": "send-mex", "message": "ciao"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "end"},
                    |   {"from": "send1", "to": "send2"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "The following states are not connected with start or end state: [send1, send2]"
        }

        "No state can be before start or after end" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send1", "type": "send-mex", "message": "ciao"},
                    |   {"id": "send2", "type": "send-mex", "message": "ciao2"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "send1", "to": "send2"},
                    |   {"from": "send2", "to": "start"},
                    |   {"from": "start", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Start state is not first state. States [send1, send2] are before"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send1", "type": "send-mex", "message": "ciao"},
                    |   {"id": "send2", "type": "send-mex", "message": "ciao2"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "end"},
                    |   {"from": "end", "to": "send1"},
                    |   {"from": "send1", "to": "send2"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "End state is not last state. States [send1, send2] are after"
        }

        "No state can be linked to itself" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "send"},
                    |   {"from": "send", "to": "send"},
                    |   {"from": "send", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "No state can be linked to itself: [send]"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {"id": "send", "type": "send-mex", "message": "ciao"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "send"},
                    |   {"from": "send", "to": "send"},
                    |   {"from": "start", "to": "start"},
                    |   {"from": "send", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "No state can be linked to itself: [send, start]"
        }

        "A wait-for-input state" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["foo", "bar"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["foo"]},
                    |   {"from": "sendfoo", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "The following states are not connected with start or end state: [sendbar]"

            val config: BotConfig = KobotParser.parse(
                """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["foo", "bar"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["foo"]},
                    |   {"from": "wti", "to": "sendbar", "on-input": ["bar"]},
                    |   {"from": "sendfoo", "to": "end"}
                    |]
                    |}""".trimMargin()
            )
            config.states.size shouldBe 5

            KobotParser.parse(
                """{
                |"states": [
                |   {"id": "start", "type": "start"},
                |   {
                |       "id": "wti", 
                |       "type": "wait-for-input", 
                |       "expected-type": "string",
                |       "on-mismatch": "error on input. choices are:",
                |       "expected-values": {
                |           "type": "static",
                |           "values": ["foo", "bar"]
                |       },
                |       "session-field": "s"
                |   },
                |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito: $[session.s]"},
                |   {"id": "end", "type": "end"}
                |],
                |"relationships": [
                |   {"from": "start", "to": "wti"},
                |   {"from": "wti", "to": "sendfoo", "on-input": ["foo", "bar"]},
                |   {"from": "sendfoo", "to": "end"}
                |]
                |}""".trimMargin()
            ) as BotConfig
        }

        "A static wait-for-input can't declare more expected-values than on-inputs" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["a", "b", "c"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["a"]},
                    |   {"from": "wti", "to": "sendbar", "on-input": ["b"]},
                    |   {"from": "sendfoo", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Static input state 'wti' has no outgoing relationship on input [c]"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["a", "b", "c", "d"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["a"]},
                    |   {"from": "wti", "to": "sendbar", "on-input": ["b"]},
                    |   {"from": "sendfoo", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Static input state 'wti' has no outgoing relationship on input [c, d]"
        }

        "A static wait-for-input can't declare less expected-values than on-inputs" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["a", "b"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["a"]},
                    |   {"from": "wti", "to": "sendbar", "on-input": ["b"]},
                    |   {"from": "wti", "to": "end", "on-input": ["c"]},
                    |   {"from": "sendfoo", "to": "end"},
                    |   {"from": "sendbar", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Static input state 'wti' doesn't declare expected values [c] but relationships from wti declares on-input [c]"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["a", "b"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["a"]},
                    |   {"from": "wti", "to": "sendbar", "on-input": ["b"]},
                    |   {"from": "wti", "to": "end", "on-input": ["c", "d"]},
                    |   {"from": "sendfoo", "to": "end"},
                    |   {"from": "sendbar", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Static input state 'wti' doesn't declare expected values [c, d] but relationships from wti declares on-input [c, d]"
        }

        "A static wait-for-input must declare same expected-values of on-inputs" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                    |"states": [
                    |   {"id": "start", "type": "start"},
                    |   {
                    |       "id": "wti", 
                    |       "type": "wait-for-input", 
                    |       "expected-type": "string",
                    |       "on-mismatch": "error on input. choices are:",
                    |       "expected-values": {
                    |           "type": "static",
                    |           "values": ["a", "b"]
                    |       },
                    |       "session-field": "s"
                    |   },
                    |   {"id": "sendfoo", "type": "send-mex", "message": "Hai inserito un foo: $[session.s]"},
                    |   {"id": "sendbar", "type": "send-mex", "message": "Hai inserito un bar: $[session.s]"},
                    |   {"id": "end", "type": "end"}
                    |],
                    |"relationships": [
                    |   {"from": "start", "to": "wti"},
                    |   {"from": "wti", "to": "sendfoo", "on-input": ["c"]},
                    |   {"from": "wti", "to": "sendbar", "on-input": ["d"]},
                    |   {"from": "sendfoo", "to": "end"},
                    |   {"from": "sendbar", "to": "end"}
                    |]
                    |}""".trimMargin()
                ) as BotConfig
            }.message shouldContain "Static input state 'wti' has no outgoing relationship on input [a, b]"
        }

    }
}