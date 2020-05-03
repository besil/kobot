package cloud.bernardinello.kobot.services.conversation

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.conversation.EndState
import cloud.bernardinello.kobot.services.database.SQLClientService
import cloud.bernardinello.kobot.services.http.HttpClientService
import cloud.bernardinello.kobot.services.memory.MemoryService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.getForObject
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SpringConversationServiceTestController {
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
class SpringConversationServiceTestConfig() {
    @Bean
    fun botConfig(): BotConfig = mockk()

    @Bean
    fun memoryService(): MemoryService = mockk()

    @Bean
    fun sqlClientService(): SQLClientService = mockk()

    @Bean
    fun httpClientService(): HttpClientService = mockk()
}

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [SpringConversationServiceTestConfig::class, SpringConversationServiceTestController::class]
)
class SpringConversationServiceTest @Autowired constructor(val conversationService: ConversationService) {
    companion object {
        val log = LoggerFactory.getLogger(SpringConversationServiceTest::class.java)
    }

    @LocalServerPort
    var port: Int = -1

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `conversationService is not null`(@Autowired config: BotConfig) {
        assertThat(conversationService).isNotNull
        log.info("conversationService is: {}", conversationService::class.simpleName)
        log.info("conversationService is: {}", conversationService.httpClient::class.simpleName)

        log.info("Config: {}", conversationService.config)
        log.info("Config: {}", conversationService.config == config)
        every { config.endState } returns EndState("foo")

        assertThat(config.endState.id).isEqualTo("foo")
    }

    @Test
    fun `test http request`() {
        val result = restTemplate.getForObject<Map<String, Any>>("http://localhost:$port/api/random")
        assertThat(result!!["hello"]).isEqualTo("world")
    }

}
