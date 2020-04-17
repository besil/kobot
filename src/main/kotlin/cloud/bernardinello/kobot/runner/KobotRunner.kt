package cloud.bernardinello.kobot.runner

import cloud.bernardinello.kobot.conf.TelegramConfig
import cloud.bernardinello.kobot.services.KobotActorService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner

//@Component
class KobotRunner(
    @Autowired val kobotActorService: KobotActorService,
    @Autowired val telegramConfig: TelegramConfig
) : CommandLineRunner {
    companion object {
        val log = LoggerFactory.getLogger(KobotRunner::class.java)
    }

    override fun run(vararg args: String) {
        log.info("ARGS: ${args.toList()}")
        log.debug("Config: {}", kobotActorService.config)
        kobotActorService.start()
        kobotActorService.startTelegram(telegramConfig)
    }
}