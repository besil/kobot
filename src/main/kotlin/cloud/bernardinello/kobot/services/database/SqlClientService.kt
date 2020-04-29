package cloud.bernardinello.kobot.services.database

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate

interface SQLClientService {
    companion object {
        val log = LoggerFactory.getLogger(SQLClientService::class.java)
    }

    fun queryForList(sql: String): List<Map<String, Any>> {
        log.error("SQL Client not configured")
        throw RuntimeException("SQL Client not configured")
    }

    fun update(sql: String): Int {
        log.error("SQL Client not configured")
        throw RuntimeException("SQL Client not configured")
    }
}

class KobotSQLClient(val jdbcTemplate: JdbcTemplate) : SQLClientService {
    override fun queryForList(sql: String): List<Map<String, Any>> {
        return jdbcTemplate.queryForList(sql)
    }

    override fun update(sql: String): Int {
        return jdbcTemplate.update(sql)
    }
}

class MockedSQLClient : SQLClientService