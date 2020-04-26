package cloud.bernardinello.kobot.services.memory

import cloud.bernardinello.kobot.conversation.BotState

data class MemoryData(val state: BotState, val sessionData: SessionData = SessionData())
data class NoMemoryData(val chatId: Long)

class SessionData(val data: MutableMap<String, Any> = mutableMapOf()) {

//    constructor(m: Map<String, Any>) {
//        data = m
//    }

//    val data: MutableMap<String, Any> = mutableMapOf()

    operator fun contains(key: String): Boolean {
        return key in data
    }

    operator fun get(key: String): Any {
        return data[key] ?: error("Session key not found: $key")
    }

    operator fun set(key: String, value: Any) {
        data[key] = value
    }

    override fun toString(): String {
        return data.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is SessionData)
            return this.data.equals(other.data)
        return false
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }
}
