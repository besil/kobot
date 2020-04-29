package cloud.bernardinello.kobot.conversation

import cloud.bernardinello.kobot.utils.KobotParser
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEqualIgnoringCase
import io.kotest.matchers.string.shouldContain

class HttpStateTest : StringSpec() {
    init {
        "extraction-key can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "http://www.example.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "",
                | "session-field": "bar"
                |}""".trimMargin()
                ) as HttpState
            }.message shouldContain "extraction-key can't be empty"

            val state: HttpState = KobotParser.parse(
                """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "http://www.example.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo.bar",
                | "session-field": "bar"
                |}""".trimMargin()
            ) as HttpState
            state.extractionKey shouldBe "foo.bar"
        }

        "session-field can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "http://www.example.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": ""
                |}""".trimMargin()
                ) as HttpState
            }.message shouldContain "session-field can't be empty"
        }

        "method can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "",
                |   "url": "http:/www.google.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
                ) as HttpState
            }.message shouldContain "http method can't be empty"
        }

        "method can be get, post put or delete" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "foo",
                |   "url": "http:/www.google.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
                ) as HttpState
            }.message shouldContain "'foo' is not a valid http method. Supported methods are: [get, post, put, delete]"
        }

        "url field can't be empty" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
                ) as HttpState
            }.message shouldContain "url can't be empty"
        }

        "url field must be a valid url" {
            shouldThrow<BotConfigException> {
                KobotParser.parse(
                    """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "ht:/www.google.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
                ) as HttpState
            }.message shouldContain "url 'ht:/www.google.com' is not a valid url"

//            shouldThrow<BotConfigException> {
//                KobotParser.parse(
//                    """{
//                | "id": "http",
//                | "type": "http",
//                | "request": {
//                |   "method": "get",
//                |   "url": "http:www.google.com",
//                |   "query-params": [],
//                |   "body-params": [],
//                |   "headers": {}
//                | },
//                | "extraction-key": "foo",
//                | "session-field": "bar"
//                |}""".trimMargin()
//                ) as HttpState
//            }.message shouldContain "url 'http:www.google.com' is not a valid url"

//            shouldThrow<BotConfigException> {
//                KobotParser.parse(
//                    """{
//                | "id": "http",
//                | "type": "http",
//                | "request": {
//                |   "method": "get",
//                |   "url": "https:/www.google.com",
//                |   "query-params": [],
//                |   "body-params": [],
//                |   "headers": {}
//                | },
//                | "extraction-key": "foo",
//                | "session-field": "bar"
//                |}""".trimMargin()
//                ) as HttpState
//            }.message shouldContain "url 'https:/www.google.com' is not a valid url"


            val s1: HttpState = KobotParser.parse(
                """{
            | "id": "http",
            | "type": "http",
            | "request": {
            |   "method": "get",
            |   "url": "http://www.example.com",
            |   "query-params": [],
            |   "body-params": [],
            |   "headers": {}
            | },
            | "extraction-key": "foo",
            | "session-field": "bar"
            |}""".trimMargin()
            )
            s1.request.url shouldBeEqualIgnoringCase "http://www.example.com"

            val s2: HttpState = KobotParser.parse(
                """{
            | "id": "http",
            | "type": "http",
            | "request": {
            |   "method": "get",
            |   "url": "https://www.google.com",
            |   "query-params": [],
            |   "body-params": [],
            |   "headers": {}
            | },
            | "extraction-key": "foo",
            | "session-field": "bar"
            |}""".trimMargin()
            )
            s2.request.url shouldBeEqualIgnoringCase "https://www.google.com"

            val s3: HttpState = KobotParser.parse(
                """{
            | "id": "http",
            | "type": "http",
            | "request": {
            |   "method": "get",
            |   "url": "http://www.example.com:8080",
            |   "query-params": [],
            |   "body-params": [],
            |   "headers": {}
            | },
            | "extraction-key": "foo",
            | "session-field": "bar"
            |}""".trimMargin()
            )
            s3.request.url shouldBeEqualIgnoringCase "http://www.example.com:8080"
        }

        "query params can be" {
            val state: HttpState = KobotParser.parse(
                """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "http://www.example.com",
                |   "query-params": [
                |       {"key": "foo", "value": "bar"}
                |   ],
                |   "body-params": [],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
            )
            state.request.queryParams.size shouldBe 1
            state.request.queryParams[0].key shouldBe "foo"
            state.request.queryParams[0].value shouldBe "bar"
        }

        "body params can be" {
            val state: HttpState = KobotParser.parse(
                """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "http://www.example.com",
                |   "query-params": [
                |   ],
                |   "body-params": [
                |       {"key": "foo", "value": "bar"}
                |   ],
                |   "headers": {}
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
            )
            state.request.bodyParams.size shouldBe 1
            state.request.bodyParams[0].key shouldBe "foo"
            state.request.bodyParams[0].value shouldBe "bar"
        }

        "headers params can be" {
            val state: HttpState = KobotParser.parse(
                """{
                | "id": "http",
                | "type": "http",
                | "request": {
                |   "method": "get",
                |   "url": "http://www.example.com",
                |   "query-params": [],
                |   "body-params": [],
                |   "headers": {
                |       "content-type": "application/json",
                |       "accept": "application/json"
                |   }
                | },
                | "extraction-key": "foo",
                | "session-field": "bar"
                |}""".trimMargin()
            )
            state.request.headers.contentType == "application/json"
            state.request.headers.accept == "application/json"
        }
    }
}