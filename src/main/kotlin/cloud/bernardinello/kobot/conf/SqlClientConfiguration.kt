package cloud.bernardinello.kobot.conf

import cloud.bernardinello.kobot.services.database.KobotSQLClient
import cloud.bernardinello.kobot.services.database.MockedSQLClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource


@Configuration
class SqlClientConfiguration {
    companion object {
        val log = LoggerFactory.getLogger(SqlClientConfiguration::class.java)
    }

    @Bean("kobot-datasource")
    @ConditionalOnProperty(
        name = [
            "kobot.database.url", "kobot.database.username",
            "kobot.database.password", "kobot.database.driverClassName"
        ],
        matchIfMissing = false
    )
    fun getKobotDataSource(
        @Value("\${kobot.database.url}") url: String,
        @Value("\${kobot.database.username}") username: String,
        @Value("\${kobot.database.password}") password: String,
        @Value("\${kobot.database.driverClassName}") driverClassName: String
    ): DataSource {
        log.debug("Preparing kobot-datasource")
        val dataSourceBuilder = DataSourceBuilder.create()
        dataSourceBuilder.driverClassName(driverClassName)
        dataSourceBuilder.url(url)
        dataSourceBuilder.username(username)
        dataSourceBuilder.password(password)
        return dataSourceBuilder.build()
    }

    @Bean("kobot-jdbc-template")
    @ConditionalOnBean(name = ["kobot-datasource"])
    fun jdbcTemplate(@Autowired @Qualifier("kobot-datasource") dataSource: DataSource): JdbcTemplate {
        return JdbcTemplate(dataSource)
    }

    @Bean
    @ConditionalOnBean(name = ["kobot-jdbc-template"])
    fun kobotSQLClient(@Autowired @Qualifier("kobot-jdbc-template") jdbcTemplate: JdbcTemplate): KobotSQLClient {
        log.info("Kobot SQL Client enabled")
        return KobotSQLClient(jdbcTemplate)
    }

    @Bean("kobot-datasource")
    @ConditionalOnProperty(
        name = [
            "kobot.database.url", "kobot.database.username",
            "kobot.database.password", "kobot.database..driverClassName"
        ],
        matchIfMissing = true
    )
    fun mockedSQLClient(): MockedSQLClient {
        log.info("Kobot SQL Client disabled")
        return MockedSQLClient()
    }

}