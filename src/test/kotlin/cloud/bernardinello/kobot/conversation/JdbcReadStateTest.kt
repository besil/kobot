package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.slf4j.LoggerFactory

class JdbcReadStateTest : StringSpec() {

    companion object {
        val log = LoggerFactory.getLogger(JdbcReadStateTest::class.java)
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

        "a single column result should be expressed in the query" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select * from foo",
                | "session-field": "foo"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select * from foo' provided for state: 'read' must have a single column return"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select a, b from foo",
                | "session-field": "foo"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select a, b from foo' provided for state: 'read' must have a single column return"
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
            }.message shouldContain "Invalid query: 'select * from' provided for state: 'read' must have a single column return"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select * from foo groupby asd",
                | "session-field": "foos"
                |}""".trimMargin()
                ) as JdbcReadState
            }.message shouldContain "Invalid query: 'select * from foo groupby asd' provided for state: 'read' must have a single column return"
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

        "extractSelectNames should return only column names" {
            val state = JdbcReadState(
                "read",
                query = "select a from foo",
                sessionField = "key"
            )
            state.extractSelectNames("select a from foo") shouldBe listOf("a")
            state.extractSelectNames("select a from foo where chatId=5") shouldBe listOf("a")
            state.extractSelectNames("select a from foo where chatId=:chatId") shouldBe listOf("a")
            state.extractSelectNames("select a from foo where chatId=!{chatId}") shouldBe listOf("a")

            state.extractSelectNames("select a,b from foo") shouldBe listOf("a", "b")
            state.extractSelectNames("select a,b from foo where chatId=5") shouldBe listOf("a", "b")
            state.extractSelectNames("select a,b from foo where chatId=?") shouldBe listOf("a", "b")
            state.extractSelectNames("select a,b from foo where chatId=:chatId") shouldBe listOf("a", "b")
        }

        "jdbc-read should be able to use session parameters" {
            val state: JdbcReadState = KobotParser.parse(
                """{
                | "id": "read",
                | "type": "jdbc-read",
                | "query": "select a from foobar where foo<>!{user-key} and bar=!{chatId}",
                | "session-field": "foos"
                |}""".trimMargin()
            )

            state.query shouldBe "select a from foobar where foo<>!{user-key} and bar=!{chatId}"
        }
    }
}