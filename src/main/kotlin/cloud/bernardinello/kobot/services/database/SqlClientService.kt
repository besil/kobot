package cloud.bernardinello.kobot.services.database

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
interface SQLClientService {
    companion object {
        val log = LoggerFactory.getLogger(SQLClientService::class.java)
    }

    fun queryForList(sql: String): List<Map<String, Any>>

    fun update(sql: String): Int
}

class KobotSQLClient(val jdbcTemplate: JdbcTemplate) : SQLClientService {
    override fun queryForList(sql: String): List<Map<String, Any>> {
        return jdbcTemplate.queryForList(sql)
    }

    override fun update(sql: String): Int {
        return jdbcTemplate.update(sql)
    }
}

class MockedSQLClient : SQLClientService {
    companion object {
        val log = LoggerFactory.getLogger(MockedSQLClient::class.java)
    }

    override fun queryForList(sql: String): List<Map<String, Any>> {
        log.error("SQL Client not configured")
        throw RuntimeException("SQL Client not configured")
    }

    override fun update(sql: String): Int {
        log.error("SQL Client not configured")
        throw RuntimeException("SQL Client not configured")
    }
}