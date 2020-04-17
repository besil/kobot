package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class JdbcReadStateTest : StringSpec() {

    init {
        "A jdbc-read state can't have empty query" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "",
                | "session-field": "foos"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query provided for state: 'read'"
        }

        "A jdbc-read state must have a not-empty session-field" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select * from foo",
                | "session-field": ""
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid session-field: '' provided for state: 'read'"
        }


        "A jdbc-read state should have a valid sql query" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select * from",
                | "session-field": ""
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select * from' provided for state: 'read'"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select * from foo group by asd",
                | "session-field": ""
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select * from foo group by asd' provided for state: 'read'"
        }
    }

}