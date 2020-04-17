package cloud.bernardinello.kobot.spikes

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotlintest.TestCase
import io.kotlintest.matchers.instanceOf
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class JacksonNativeSerializationTest : StringSpec() {

    class FooBarException(mex: String) : Exception(mex)

    @JsonIgnoreProperties(ignoreUnknown = false)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(
        value = [
            JsonSubTypes.Type(value = Foo::class, name = "foo"),
            JsonSubTypes.Type(value = Bar::class, name = "bar")
        ]
    )
    abstract class FooBar(val id: String, val type: String) {
        init {
            if (id == "")
                throw FooBarException(
                    "a foobar id can't be ''"
                )
        }
    }

    class Foo(id: String, val foo: String, val x: Int) : FooBar(id, type = "foo") {
        init {
            if (foo == "bar")
                throw FooBarException(
                    "a foo can't hold bar"
                )
        }
    }

    class Bar(id: String, val bar: Int, val z: String) : FooBar(id, type = "bar")

    companion object {
        val log: Logger = LoggerFactory.getLogger(JacksonNativeSerializationTest::class.java)
    }

    override fun beforeTest(testCase: TestCase) {
        log.info("Running test: ${testCase.name}")
    }

    init {
        "PHT test" {
            val foo: FooBar = jacksonObjectMapper().readValue(
                """{
                    | "type": "foo",
                    | "id": "a foobar",
                    | "foo": "a foo",
                    | "x": 3
                |}""".trimMargin()
            )

            foo.type shouldBe "foo"
            foo.id shouldBe "a foobar"
            foo shouldBe instanceOf(Foo::class)
            foo as Foo
            foo.foo shouldBe "a foo"
            foo.x shouldBe 3

            val bar: FooBar = jacksonObjectMapper().readValue(
                """{
                |"id": "bar1",
                |"type": "bar",
                |"bar": 1,
                |"z": "zbar"
                |}""".trimMargin()
            )
            bar.id shouldBe "bar1"
            bar.type shouldBe "bar"
            bar shouldBe instanceOf(Bar::class)
            bar as Bar
            bar.bar shouldBe 1
            bar.z shouldBe "zbar"
        }

        "A foobar id should not be empty" {
            shouldThrow<ValueInstantiationException> {
                jacksonObjectMapper().readValue(
                    """{
                    | "id": "",
                    | "type": "foo",
                    | "foo": "a foo",
                    | "x": 3
                    |}""".trimMargin()
                ) as FooBar
            }.cause shouldBe instanceOf(FooBarException::class)
        }

        "a asd type should not be parsed" {
            val e = shouldThrow<InvalidTypeIdException> {
                jacksonObjectMapper().readValue(
                    """{
                    | "id": "",
                    | "type": "asd",
                    | "foo": "a foo",
                    | "x": 3
                    |}""".trimMargin()
                ) as Foo
            }
            log.debug(e.message)
            log.debug(e.typeId)
            log.debug("{}", e.baseType)
            log.debug("{}", e.cause)
        }

        "A foo can't hold bar" {
            val e = shouldThrow<ValueInstantiationException> {
                jacksonObjectMapper().readValue(
                    """{
                | "id": "id",
                | "type": "foo",
                | "foo": "bar",
                | "x": 3
                |}""".trimMargin()
                ) as FooBar
            }
            log.debug(e.message)
            log.debug(e.localizedMessage)
            log.debug("{}", e.location)
            log.debug("{}", e.type)
            log.debug("{}", e.cause)
        }
    }
}
