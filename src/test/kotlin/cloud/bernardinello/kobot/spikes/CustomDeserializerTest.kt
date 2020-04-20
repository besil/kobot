package cloud.bernardinello.kobot.spikes

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.instanceOf
import io.kotest.matchers.shouldBe
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure


class CustomDeserializerTest : StringSpec() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(CustomDeserializerTest::class.java)
    }

//    override fun beforeTest(testCase: TestCase) {
//        log.info("Running test: ${testCase.name}")
//    }

    val om: ObjectMapper = jacksonObjectMapper()

    @JsonDeserialize(using = FooBarDeserializer::class)
    abstract class FooBar(val type: String)

    class Foo(val foo: String) : FooBar(type = "foo")
    class Bar(val bar: Int, val z: Double) : FooBar(type = "bar")

    class FooBarDeserializerException(msg: String) : Exception(msg)

    class FooBarDeserializer @JvmOverloads constructor(vc: Class<*>? = null) : StdDeserializer<FooBar>(vc) {
        override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): FooBar {
            log.debug("Starting deserialization")
            val jsonNode: JsonNode = jp.readValueAsTree()

            val type = jsonNode.get("type").asText()
            log.debug("Found type: $type")

            try {
                when (type) {
                    "bar" -> {
                        val constructor: KFunction<Bar> = Bar::class.primaryConstructor!!
                        log.debug("Primary constructor parameters: {}", constructor.parameters)

                        val invokeParameters: Map<KParameter, Any?> =
                            constructor.parameters.map { param: KParameter ->
                                val traverse = jsonNode.get(param.name).traverse()
                                traverse.nextToken()
                                val jsonValue: Any = ctxt.readValue(traverse, param.type.jvmErasure.javaObjectType)
                                param to jsonValue
                            }.toMap()

                        log.debug("Invoking Bar({})", invokeParameters.map { "${it.key.name}=${it.value}" })
                        val callBy: Bar = constructor.callBy(invokeParameters)
                        return callBy
                    }
                    "foo" -> {
                        val foo: String = jsonNode.get("foo").toString()
                        log.debug("Returning Foo value: $foo")
                        return Foo(foo)
                    }
                    else -> throw FooBarDeserializerException(
                        "Type $type doesn't exist"
                    )
                }
            } catch (e: Exception) {
                throw FooBarDeserializerException(
                    e.message!!
                )
            }
        }
    }

    init {
        "a bar deserialization" {
            val bar: FooBar = om
                .readValue(
                    """{
                |   "type": "bar",
                |   "bar": 2,
                |   "z": 0.3
                |}""".trimMargin()
                )

            bar shouldBe instanceOf(Bar::class)
            (bar as Bar).bar shouldBe 2
            bar.z shouldBe 0.3
        }

        "an invalid type should throw exception" {
            shouldThrow<FooBarDeserializerException> {
                om.readValue(
                    """{
                |"type": "asd"
                |}""".trimMargin()
                ) as Foo
            }
        }

        "a malformed json" {
            shouldThrow<JsonParseException> {
                om.readValue(
                    """{
                |"type": "bar",
                |}""".trimMargin()
                ) as FooBar
            }
        }

        "a mismatch config" {
            shouldThrow<FooBarDeserializerException> {
                om
                    .readValue(
                        """{
                        |"type": "bar",
                        |"bar": "ciao"
                        |}""".trimMargin()
                    ) as FooBar
            }
        }

    }
}