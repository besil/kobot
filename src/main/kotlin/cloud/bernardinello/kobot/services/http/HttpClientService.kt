package cloud.bernardinello.kobot.services.http

import cloud.bernardinello.kobot.conversation.HttpRequestDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
interface HttpClientService {
    fun execute(request: HttpRequestDetails)
}


class KobotHTTPClient(@Autowired val client: RestTemplate) : HttpClientService {
    override fun execute(request: HttpRequestDetails) {

    }
}


class MockedHTTPClient : HttpClientService {
    companion object {
        val log = LoggerFactory.getLogger(MockedHTTPClient::class.java)
    }

    override fun execute(request: HttpRequestDetails) {
        log.error("SQL Client not configured")
        throw RuntimeException("SQL Client not configured")
    }
}