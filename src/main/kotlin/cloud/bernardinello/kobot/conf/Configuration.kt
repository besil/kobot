package cloud.bernardinello.kobot.conf

data class TelegramConfig(val name: String, val token: String)

data class DatabaseConfig(val username: String, val password: String, val url: String)