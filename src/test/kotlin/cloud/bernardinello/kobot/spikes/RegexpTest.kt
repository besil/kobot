package cloud.bernardinello.kobot.spikes

import cloud.bernardinello.kobot.services.conversation.ConversationServiceTest
import io.kotlintest.TestCase
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import org.slf4j.LoggerFactory

class RegexpTest : StringSpec() {

    companion object {
        val log = LoggerFactory.getLogger(ConversationServiceTest::class.java)
    }

    override fun beforeTest(testCase: TestCase) {
        log.info(
            "\n-----------------------------\n" +
                    "Running test: ${testCase.name}\n" +
                    "-----------------------------".trimIndent()
        )
    }

    init {
        "sample regexp" {
            val s = "a !{foo} must not be !{bar}. Except a !{foo-bar} can!"
//            val regex = """!\{([\w\-]+)\}""".toRegex()
            val regex = """!\{(.*?)\}""".toRegex()

            log.debug("Match? {}", regex.containsMatchIn(s))
            regex.containsMatchIn(s) shouldBe true

            val matches: List<MatchResult> = regex.findAll(s).toList()
            log.debug("Groups: {}", matches.map { it.groups })
            log.debug("Matches: {}", matches.map { it.groupValues })

            val keys = matches.map { it.groupValues.last() }
            log.debug("Final: {}", keys)

            keys.size shouldBe 3
            keys shouldBe listOf("foo", "bar", "foo-bar")
        }
    }
}