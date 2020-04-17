package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotlintest.TestCase
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import org.slf4j.LoggerFactory

class JdbcReadStateTest : StringSpec() {

    companion object {
        val log = LoggerFactory.getLogger(JdbcReadStateTest::class.java)
    }

    override fun beforeTest(testCase: TestCase) {
        log.info("Running test: ${testCase.name}")
    }

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
            }.message shouldContain "Invalid query: '' provided for state: 'read'"
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
                | "session-field": "foos"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select * from' provided for state: 'read'"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select * from foo groupby asd",
                | "session-field": "foos"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select * from foo groupby asd' provided for state: 'read'"
        }

        "Only select query should be accepted" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "insert into foo values('foo', 'bar')",
                | "session-field": "foos"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'insert into foo values('foo', 'bar')' provided for state: 'read' is not a select"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "update foo set foo='bar' where userid='!{chatId}'",
                | "session-field": "foos"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'update foo set foo='bar' where userid='!{chatId}'' provided for state: 'read' is not a select"
        }
    }
}