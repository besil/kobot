package cloud.bernardinello.kobot.spikes

import cloud.bernardinello.kobot.services.conversation.ConversationServiceTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory

class RegexpTest : StringSpec() {

    companion object {
        val log = LoggerFactory.getLogger(ConversationServiceTest::class.java)
    }

//    override fun beforeTest(testCase: TestCase) {
//        log.info(
//            "\n-----------------------------\n" +
//                    "Running test: ${testCase.name}\n" +
//                    "-----------------------------".trimIndent()
//        )
//    }

    init {
        "Kotlin regexp" {
            //            val s = "hello $[whof] world"
            val s = "hello world"
            val regexp = "(.*)world".toRegex()

            regexp.matches(s) shouldBe true

            val find: MatchResult = regexp.find(s)!!

            println(find.groups)
            println(find.groupValues)

            find.groups.first()!!.value shouldBe "hello world"
            find.groups.get(1)!!.value shouldBe "hello "
        }

        "Kotlin match word between ! and !" {
            val s = "hello !beautiful! world"
            val regexp = "hello !(.*)! world".toRegex()

            regexp.matches(s) shouldBe true

            val find: MatchResult = regexp.find(s)!!

            println(find.groups)
            find.groupValues[1] shouldBe "beautiful"
        }

        "Kotlin regexp with $ and [] characters" {
            val s = "hello !{whof} world"
            val regexp = ".*!\\{(.*)}.*".toRegex()

            regexp.matches(s) shouldBe true

            val find: MatchResult = regexp.find(s)!!

            println(find.groups)
            find.groupValues[1] shouldBe "whof"
        }

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