package cloud.bernardinello.kobot.services.http

import cloud.bernardinello.kobot.conversation.HttpRequestDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
interface HttpClientService {
    fun execute(request: HttpRequestDetails): ResponseEntity<Map<String, Any>>
}


class KobotHTTPClient(@Autowired val client: RestTemplate) : HttpClientService {
    override fun execute(request: HttpRequestDetails): ResponseEntity<Map<String, Any>> {
        return ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR)
    }
}


class MockedHTTPClient : HttpClientService {
    companion object {
        val log = LoggerFactory.getLogger(MockedHTTPClient::class.java)
    }

    override fun execute(request: HttpRequestDetails): ResponseEntity<Map<String, Any>> {
        log.error("SQL Client not configured")
        throw RuntimeException("SQL Client not configured")
    }
}