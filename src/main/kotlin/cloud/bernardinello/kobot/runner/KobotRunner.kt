package cloud.bernardinello.kobot.runner

import cloud.bernardinello.kobot.conf.TelegramConfig
import cloud.bernardinello.kobot.services.KobotService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class KobotRunner(
    @Autowired val kobotService: KobotService,
    @Autowired val telegramConfig: TelegramConfig
) : CommandLineRunner {
    companion object {
        val log = LoggerFactory.getLogger(KobotRunner::class.java)
    }

    override fun run(vararg args: String) {
        log.info("ARGS: ${args.toList()}")
        log.debug("Config: {}", kobotService.config)
        kobotService.start()
        kobotService.startTelegram(telegramConfig)
    }
}