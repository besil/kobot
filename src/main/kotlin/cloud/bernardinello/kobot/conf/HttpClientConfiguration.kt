package cloud.bernardinello.kobot.conf

import cloud.bernardinello.kobot.services.http.HttpClientService
import cloud.bernardinello.kobot.services.http.KobotHTTPClient
import cloud.bernardinello.kobot.services.http.MockedHTTPClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

@Configuration
class HttpClientConfiguration {
    companion object {
        val log = LoggerFactory.getLogger(HttpClientConfiguration::class.java)
    }

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    @ConditionalOnProperty(
        name = ["kobot.http.client.enabled"],
        havingValue = "true",
        matchIfMissing = false
    )
    fun kobotHttpClient(@Autowired restTemplate: RestTemplate): HttpClientService {
        log.info("Kobot HTTP Client enabled")
        return KobotHTTPClient(restTemplate)
    }

    @Bean
    @ConditionalOnProperty(
        name = ["kobot.http.client.enabled"],
        havingValue = "false",
        matchIfMissing = true
    )
    fun mockedHTTPClient(): HttpClientService {
        log.info("Kobot HTTP Client disabled")
        return MockedHTTPClient()
    }
}