package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.slf4j.LoggerFactory

class JdbcWriteStateTest : StringSpec() {
    companion object {
        val log = LoggerFactory.getLogger(JdbcWriteStateTest::class.java)
    }

    init {
        "A jdbc-write state can't have empty query" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "write",
                | "type": "jdbc-write",
                | "query": ""
                |}""".trimMargin()
                ) as JdbcWriteState
            }.message shouldContain "Invalid query: '' provided for state: 'write'"
        }

        "A jdbc-write state can't have bad-written query" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "write",
                | "type": "jdbc-write",
                | "query": "insertinto foo values('foo', 'bar')"
                |}""".trimMargin()
                ) as JdbcWriteState
            }.message shouldContain "Invalid query: 'insertinto foo values('foo', 'bar')' provided for state: 'write'"
        }

        "A jdbc-write state sql must be update or insert" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "write",
                | "type": "jdbc-write",
                | "query": "select * from foo"
                |}""".trimMargin()
                ) as JdbcWriteState
            }.message shouldContain "Invalid query: 'select * from foo' provided for state: 'write' is not an insert or update"

            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "read",
                | "type": "jdbc-write",
                | "query": "select * from foo group by asd"
                |}""".trimMargin()
                ) as JdbcWriteState
            }.message shouldContain "Invalid query: 'select * from foo group by asd' provided for state: 'read' is not an insert or update"
        }

        "a jdbc-write state can use session parameters" {
            val write: JdbcWriteState = KobotParser.parse(
                """{
                | "id": "write",
                | "type": "jdbc-write",
                | "query": "insert into foobar(foo, bar) values (!{foo}, !{bar})"
                |}""".trimMargin()
                    .trimMargin()
            )

            write.query shouldBe "insert into foobar(foo, bar) values (!{foo}, !{bar})"
        }
    }

}