package cloud.bernardinello.kobot.spikes

import cloud.bernardinello.kobot.conf.HttpClientConfiguration
import cloud.bernardinello.kobot.services.http.HttpClientService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import


@Configuration
@ComponentScan(
    excludeFilters = [ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        value = [CommandLineRunner::class]
    )]
)
@EnableAutoConfiguration
class TestApplicationConfiguration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestApplicationConfiguration::class]
)
@Import(HttpClientConfiguration::class)
class SpringBootTest @Autowired constructor(val service: HttpClientService) {

    @Test
    fun `service is not null`() {
        assertThat(service).isNotNull
    }

}