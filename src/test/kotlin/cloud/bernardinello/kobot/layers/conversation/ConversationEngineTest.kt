package cloud.bernardinello.kobot.layers.conversation

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class ConversationEngineTest : StringSpec() {

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
    }

}