package cloud.bernardinello.kobot.conf

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.utils.KobotParser
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import java.nio.file.Paths
import javax.sql.DataSource


@Configuration
class KobotConfiguration {
    companion object {
        val log = LoggerFactory.getLogger(KobotConfiguration::class.java)
    }

    @Bean
    fun botConfig(@Value("\${conversation.path}") conversationPath: String): BotConfig {
        log.info("Creating a bot config from conversation file: {}", conversationPath)
        return KobotParser.parse(Paths.get(conversationPath))
    }

    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

}