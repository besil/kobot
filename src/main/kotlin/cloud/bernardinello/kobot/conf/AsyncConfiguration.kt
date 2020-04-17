package cloud.bernardinello.kobot.conf

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor


@Configuration
@EnableAsync(proxyTargetClass = true)
//@EnableCaching(proxyTargetClass = true)
class AsyncConfiguration {
    @Bean("threadPoolTaskExecutor")
    fun threadPoolTaskExecutor(): Executor {
        return ThreadPoolTaskExecutor()
    }
}