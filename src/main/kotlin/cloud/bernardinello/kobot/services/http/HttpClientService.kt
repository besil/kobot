package cloud.bernardinello.kobot.services.http

import cloud.bernardinello.kobot.conversation.HttpRequestDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder

@Service
interface HttpClientService {
    fun execute(request: HttpRequestDetails): ResponseEntity<Map<String, Any>>
}

class HttpClientServiceException(val msg: String) : Exception(msg)

class KobotHTTPClient(@Autowired val client: RestTemplate) : HttpClientService {
    companion object {
        val log = LoggerFactory.getLogger(KobotHTTPClient::class.java)
    }


    override fun execute(request: HttpRequestDetails): ResponseEntity<Map<String, Any>> {
        val headers: MultiValueMap<String, String> = LinkedMultiValueMap()
        headers[HttpHeaders.CONTENT_TYPE] = request.headers.contentType
        headers[HttpHeaders.ACCEPT] = request.headers.accept

        val body: Map<String, Any> = request.bodyParams.map { it.key to it.value }.toMap()

        val entity: HttpEntity<Map<String, Any>> = HttpEntity(body, headers)

        var builder: UriComponentsBuilder = UriComponentsBuilder.fromHttpUrl(request.url)
        request.queryParams
            .filter { it.value.isNotBlank() }
            .forEach { builder = builder.queryParam(it.key, it.value) }
        request.bodyParams
        val url = builder.toUriString()
        val method = HttpMethod.valueOf(request.method.toUpperCase())

        val answer: ResponseEntity<Map<String, Any>> = this.makeRequest(url, method, entity)
        return answer
    }

    private inline fun <reified T, reified K> makeRequest(
        url: String,
        method: HttpMethod,
        entity: HttpEntity<Map<T, K>>
    ): ResponseEntity<Map<T, K>> {
        return client.exchange(
            url,
            method,
            entity,
            Map::class
        )
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