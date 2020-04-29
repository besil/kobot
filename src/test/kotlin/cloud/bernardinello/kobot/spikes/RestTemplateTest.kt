package cloud.bernardinello.kobot.spikes

import cloud.bernardinello.kobot.conf.HttpClientConfiguration
import cloud.bernardinello.kobot.services.http.HttpClientService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.getForEntity

@RestController
class TestController {
    @GetMapping("/api/random")
    fun random(): Map<String, Any> {
        return mapOf("hello" to "world", "list" to listOf(1, 2, 3))
    }
}

@Configuration
@ComponentScan(
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = [CommandLineRunner::class]
    )]
)
@EnableAutoConfiguration
class RestTestConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [RestTestConfiguration::class]
)
@Import(HttpClientConfiguration::class)
class RestTemplateTest @Autowired constructor(val bot: HttpClientService) {
    companion object {
        val log = LoggerFactory.getLogger(RestTemplateTest::class.java)
    }

    @Test
    fun `bot is not null`() {
        Assertions.assertThat(bot).isNotNull
    }

    @LocalServerPort
    var port: Int = -1

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `a http request should be decoded as Map String Any`() {
        val http = restTemplate.restTemplate
        val resp: ResponseEntity<Map<String, Any>> = http.getForEntity("http://localhost:$port/api/random")

        log.info("Response: {}", resp)
        val json = JSONObject(resp.body)
        log.info("Body: {}", json)
//        log.info("Quote is: {}", json.getJSONObject("contents").getJSONArray("quotes").first())

        assertThat(resp.statusCodeValue).isEqualTo(200)
        log.info("{}", json.getJSONArray("list")[0])
    }

}


