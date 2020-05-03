package cloud.bernardinello.kobot.services.http

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.HttpRequestDetails
import cloud.bernardinello.kobot.conversation.HttpRequestParam
import cloud.bernardinello.kobot.services.database.SQLClientService
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate

@RestController
class HttpConversationServiceTestController {
    @GetMapping("/api/random")
    @ResponseBody
    fun getRandom(@RequestParam id: String): Map<String, Any> = mapOf("get" to "get id object: $id")

    @PostMapping("/api/random")
    @ResponseBody
    fun postRandom(@RequestBody body: Map<String, Any>): Map<String, Any> =
        body.map { it.key.toUpperCase() to it.value.toString().toUpperCase() }.toMap()

}

@Configuration
@EnableAutoConfiguration
@ComponentScan(
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = [CommandLineRunner::class]
    ),
        ComponentScan.Filter(
            type = FilterType.ANNOTATION,
            value = [Configuration::class]
        )
    ]
)
class HttpConversationServiceTestConfiguration() {
    @Bean
    fun botConfig(): BotConfig = mockk()

    @Bean
    fun sqlClientService(): SQLClientService = mockk()

    @Bean
    fun httpClientService(): HttpClientService = KobotHTTPClient(RestTemplate())
}

@SpringBootTest(
    classes = [HttpConversationServiceTestController::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Import(HttpConversationServiceTestConfiguration::class)
class HttpClientServiceTest @Autowired constructor(val http: HttpClientService) {
    companion object {
        val log = LoggerFactory.getLogger(HttpClientServiceTest::class.java)
    }

    @LocalServerPort
    var port: Int = -1

    @Test
    fun `test http get request`() {
        val url = "http://localhost:$port/api/random"

        val reqDetails = HttpRequestDetails(
            method = "get",
            url = url,
            queryParams = listOf(HttpRequestParam("id", "1"))
        )
        val execute: ResponseEntity<Map<String, Any>> = http.execute(reqDetails)
        assertThat(execute.statusCodeValue).isEqualTo(200)
        assertThat(execute.body).containsKey("get")
        assertThat(execute.body!!["get"]).isEqualTo("get id object: 1")
    }

    @Test
    fun `test http post request`() {
        val url = "http://localhost:$port/api/random"

        val reqDetails = HttpRequestDetails(
            method = "post",
            url = url,
            bodyParams = listOf(HttpRequestParam("foo", "bar"))
        )
        val execute: ResponseEntity<Map<String, Any>> = http.execute(reqDetails)
        assertThat(execute.statusCodeValue).isEqualTo(200)
        println("POST result is: ${execute.body}")
        log.trace("POST result is: {}", execute.body)
        assertThat(execute.body).containsKey("FOO")
        assertThat(execute.body).doesNotContainKey("foo")
        assertThat(execute.body!!["FOO"]).isEqualTo("BAR")
    }

}
