package cloud.bernardinello.kobot.services.http

import org.springframework.stereotype.Service

@Service
interface HttpClientService

class KobotHTTPClient : HttpClientService

class MockedHTTPClient : HttpClientService