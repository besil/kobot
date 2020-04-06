package cloud.bernardinello.kobot.conf

import com.fasterxml.jackson.annotation.JsonProperty

class TelegramConfig(@JsonProperty("name") val name: String, @JsonProperty("token") val token: String)