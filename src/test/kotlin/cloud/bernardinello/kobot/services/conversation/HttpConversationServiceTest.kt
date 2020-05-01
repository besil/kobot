package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.HttpRequestDetails
import cloud.bernardinello.kobot.services.database.SQLClientService
import cloud.bernardinello.kobot.services.http.HttpClientService
import cloud.bernardinello.kobot.services.http.KobotHTTPClient
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
class HttpConversationServiceTestController {
    @GetMapping("/api/random")
    fun random(): Map<String, Any> {
        return mapOf("hello" to "world", "list" to listOf(1, 2, 3))
    }
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
class HttpConversationServiceTest @Autowired constructor(val http: HttpClientService) {
    companion object {
        val log = LoggerFactory.getLogger(HttpConversationServiceTest::class.java)
    }

    @LocalServerPort
    var port: Int = -1

    @Test
    fun `test http request`(@Autowired config: BotConfig) {
        val url = "http://localhost:$port/api/random"

        val reqDetails = HttpRequestDetails(
            method = "get",
            url = url
        )

        val execute: ResponseEntity<Map<String, Any>> = http.execute(reqDetails)
        assertThat(execute.statusCodeValue).isEqualTo(200)


    }

}
