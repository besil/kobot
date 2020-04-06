package cloud.bernardinello.kobot.conversation

import com.fasterxml.jackson.annotation.JsonProperty

class BotStateRelationship(
    val from: String,
    val to: String,
    @JsonProperty("on-input") val onInput: List<String> = listOf()
)
