package cloud.bernardinello.kobot.monitoring

import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.layers.KobotActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object StartMonitoring

class MonitorActor(val botConfig: BotConfig) : KobotActor() {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(MonitorActor::class.java)
    }

    fun onReceive(start: StartMonitoring) {
        log.debug("Starting monitoring api...")
    }
}