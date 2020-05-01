package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.*
import cloud.bernardinello.kobot.services.database.SQLClientService
import cloud.bernardinello.kobot.services.http.HttpClientService
import cloud.bernardinello.kobot.services.memory.MemoryService
import cloud.bernardinello.kobot.services.memory.SessionData
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.slf4j.LoggerFactory

class ConversationServiceTest : StringSpec() {

    companion object {
        val log = LoggerFactory.getLogger(ConversationServiceTest::class.java)
    }

    init {
        val config = mockk<BotConfig>(relaxUnitFun = true)
        val memoryService = mockk<MemoryService>(relaxUnitFun = true)
        val sqlClient = mockk<SQLClientService>(relaxUnitFun = true)
        val httpClient = mockk<HttpClientService>(relaxUnitFun = true)

        "conversation service has utilities for extracting session keys from a string" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val keys = conversationService.extractSessionKeys("!{foo} must not be !{bar}. Except a !{foo-bar}!")
            keys shouldBe listOf("foo", "bar", "foo-bar")
        }

        "checkInput should return on-mismatch message when invalid input is provided" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val context = SessionData()

            val wfi = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                onMismatch = "Error!",
                expectedValues = StaticExpectedValues(listOf("ciao", "mondo"))
            )
            val inputCheck: InputCheck = conversationService.checkInput(wfi, context, "hello")
            inputCheck.valid shouldBe false
            inputCheck.message shouldBe "Error!"
            inputCheck.choices shouldBe listOf("ciao", "mondo")

            val inputCheck2 = conversationService.checkInput(wfi, context, "ciao")
            inputCheck2.valid shouldBe true
            inputCheck2.message shouldBe ""
            inputCheck2.choices shouldBe listOf()
        }

        "checkInput on session expected values should throw exception if key not present" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)

            val wfi = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                expectedValues = SessionExpectedValues(key = "foo"),
                onMismatch = "error"
            )
            val context = SessionData()

            shouldThrow<ConversationServiceException> {
                conversationService.checkInput(wfi, context, "hello")
            }.message shouldContain "Session keys ['foo'] not found in current context"
        }

        "checkInput on session expected values should throw exception if key is not a list of elements" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)

            val wfi = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                expectedValues = SessionExpectedValues(key = "foo"),
                onMismatch = "Error!"
            )

            val context = SessionData()
            context["foo"] = "bar"

            shouldThrow<ConversationServiceException> {
                conversationService.checkInput(wfi, context, "hello")
            }.message shouldContain "Session key 'foo' doesn't contain a List: 'bar' found"

            context["foo"] = 1
            shouldThrow<ConversationServiceException> {
                conversationService.checkInput(wfi, context, "hello")
            }.message shouldContain "Session key 'foo' doesn't contain a List: '1' found"

            context["foo"] = listOf(1)
            val inputCheck: InputCheck = conversationService.checkInput(wfi, context, "hello")
            inputCheck.valid shouldBe false
            inputCheck.message shouldBe "Error!"
            inputCheck.choices shouldBe listOf("1")
        }

        "update context should save variables in session fields" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val wfi = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                onMismatch = "Error!",
                expectedValues = StaticExpectedValues(listOf("ciao", "mondo")),
                sessionField = "foo"
            )

            val context = SessionData()
            conversationService.updateContext(wfi, context, "ciao")
            context.contains("foo") shouldBe true
            context["foo"] shouldBe "ciao"
        }

        "update context on start-state should do nothing" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val context = SessionData()
            conversationService.updateContext(StartState("start"), context, "foo")
            context.data.size shouldBe 0
        }

        "visiting a start state should not change the accumulator" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val context = SessionData()
            val acc = conversationService.visit(StartState("start"), Accumulator(context))
            acc shouldBe Accumulator(context)
        }

        "visiting a end state should not change the accumulator" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val context = SessionData()
            val acc = conversationService.visit(EndState("end"), Accumulator(context))
            acc shouldBe Accumulator(context)
        }

        "visiting a send state" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val states = listOf(
                SendMexState("send", "hello world"), SendMexState("2", "ciao mondo")
            )
            val context = SessionData()
            val acc = conversationService.visit(states, context)
            acc.outputMessages.toList() shouldBe listOf("hello world", "ciao mondo")
            acc.choices shouldBe listOf()
            acc.context.data.size shouldBe 0
        }

        "if send-mex references a non existing session element, an exception should be thrown" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val state = SendMexState("send", "hello !{world}")

            val context = SessionData()
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session keys [world] not found in current context"

            context["world"] = "foo"
            val acc = conversationService.visit(state, Accumulator(context))

            acc.outputMessages shouldBe listOf("hello foo")
            acc.choices shouldBe listOf()
            acc.context shouldBe context
        }

        "send-mex could reference multiple session keys" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val state = SendMexState("send", "!{greet} !{someone}")

            val context = SessionData()
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session keys [greet, someone] not found in current context"

            context["greet"] = "hello"
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session keys [someone] not found in current context"

            context["someone"] = "world"
            val acc = conversationService.visit(state, Accumulator(context))
            acc.context shouldBe context
            acc.choices shouldBe listOf()
            acc.outputMessages.toList() shouldBe listOf("hello world")
        }

        "visiting a static wait-for-input state with no session" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val states = listOf(
                WaitForInputState(
                    id = "wfi",
                    expectedType = "string",
                    expectedValues = StaticExpectedValues(
                        values = listOf("yes", "no")
                    ),
                    onMismatch = "error",
                    sessionField = "foo"
                )
            )
            val context = SessionData()
            val acc = conversationService.visit(states, context)
            acc.outputMessages.toList() shouldBe listOf()
            acc.choices shouldBe listOf("yes", "no")
            acc.context.data.size shouldBe 0
        }

        "visiting a session wait-for-input state with no key session should throw exception" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val state = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                onMismatch = "error",
                expectedValues = SessionExpectedValues(
                    key = "foo"
                )
            )
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(SessionData()))
            }.message shouldContain "Session keys ['foo'] not found in current context"
        }

        "a session wait-for-input expected values must be a collection" {
            val conversationService = ConversationService(config, memoryService, sqlClient, httpClient)
            val state = WaitForInputState(
                id = "wfi",
                expectedType = "string",
                onMismatch = "error",
                expectedValues = SessionExpectedValues(
                    key = "foo"
                )
            )

            val context = SessionData()
            context["foo"] = 1
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session key 'foo' doesn't contain a List: '1' found"

            context["foo"] = "ciao"
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(context))
            }.message shouldContain "Session key 'foo' doesn't contain a List: 'ciao' found"

            context["foo"] = listOf("ciao")
            val acc = conversationService.visit(state, Accumulator(context))
            acc.choices shouldBe listOf("ciao")
        }

        "visiting a jdbc-read state" {
            val sqlService = mockk<SQLClientService>()
            val conversationService = ConversationService(config, memoryService, sqlService, httpClient)
            val state = JdbcReadState(
                id = "read",
                query = "select a from foo",
                sessionField = "result"
            )

            val rows: List<Map<String, Any>> = listOf(mapOf("a" to 1), mapOf("a" to 2), mapOf("a" to 3))
            every { sqlService.queryForList(state.query) } returns rows

            val acc = conversationService.visit(state, Accumulator(SessionData()))
            acc.context["result"] shouldBe listOf(1, 2, 3)
        }

        "jdbc-read should be able to use session-data" {
            val sqlService = mockk<SQLClientService>()
            val conversationService = ConversationService(config, memoryService, sqlService, httpClient)
            val state = JdbcReadState(
                id = "read",
                query = "select a from foo where chatid=!{chatId}",
                sessionField = "result"
            )

            val rows: List<Map<String, Any>> = listOf(mapOf("a" to 1), mapOf("a" to 2), mapOf("a" to 3))
            every { sqlService.queryForList("select a from foo where chatid=5") } returns rows

            val sd = SessionData()
            sd["chatId"] = 5
            val acc = conversationService.visit(state, Accumulator(sd))
            acc.context["result"] shouldBe listOf(1, 2, 3)

            verify {
                sqlService.queryForList("select a from foo where chatid=5")
            }
        }

        "jdbc-read should set single value if one single element is retrieved" {
            val sqlService = mockk<SQLClientService>()
            val conversationService = ConversationService(config, memoryService, sqlService, httpClient)
            val state = JdbcReadState(
                id = "read",
                query = "select a from foo where chatid=!{chatId}",
                sessionField = "result"
            )

            val rows: List<Map<String, Any>> = listOf(mapOf("a" to 1))
            every { sqlService.queryForList("select a from foo where chatid=5") } returns rows

            val sd = SessionData()
            sd["chatId"] = 5
            val acc = conversationService.visit(state, Accumulator(sd))
            acc.context["result"] shouldBe 1

            verify {
                sqlService.queryForList("select a from foo where chatid=5")
            }
        }

        "jdbc-read should handle empty results" {
            val sqlService = mockk<SQLClientService>()
            val conversationService = ConversationService(config, memoryService, sqlService, httpClient)
            val state = JdbcReadState(
                id = "read",
                query = "select a from foo where chatid=!{chatId}",
                sessionField = "result"
            )

            val rows: List<Map<String, Any>> = listOf()
            every { sqlService.queryForList("select a from foo where chatid=5") } returns rows

            val sd = SessionData()
            sd["chatId"] = 5
            val acc = conversationService.visit(state, Accumulator(sd))
            acc.context["result"] shouldBe listOf<Any>()

            verify {
                sqlService.queryForList("select a from foo where chatid=5")
            }
        }

        "jdbc-read should throw exception if session-key is not found" {
            val sqlService = mockk<SQLClientService>()
            val conversationService = ConversationService(config, memoryService, sqlService, httpClient)
            val state = JdbcReadState(
                id = "read",
                query = "select a from foo where chatid=!{foobar} and foo=!{bar}",
                sessionField = "result"
            )
            val sd = SessionData()
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(sd))
            }.message shouldContain "Session keys [bar, foobar] not found in current context"
        }

        "jdbc-write should throw exception if session-key is not found" {
            val sqlService = mockk<SQLClientService>()
            val conversationService = ConversationService(config, memoryService, sqlService, httpClient)
            val state = JdbcWriteState(
                id = "write",
                query = "insert into foobar values(!{foo}, '!{bar}')"
            )
            val sd = SessionData()
            shouldThrow<ConversationServiceException> {
                conversationService.visit(state, Accumulator(sd))
            }.message shouldContain "Session keys [bar, foo] not found in current context"

            sd["foo"] = 1
            sd["bar"] = "bar"
            every { sqlService.update("insert into foobar values(1, 'bar')") } returns 1
            conversationService.visit(state, Accumulator(sd))

            verify {
                sqlService.update("insert into foobar values(1, 'bar')")
            }
        }


    }
}