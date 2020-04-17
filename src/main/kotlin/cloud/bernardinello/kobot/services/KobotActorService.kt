package cloud.bernardinello.kobot.services

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import cloud.bernardinello.kobot.conf.DatabaseConfig
import cloud.bernardinello.kobot.conf.TelegramConfig
import cloud.bernardinello.kobot.conversation.BotConfig
import cloud.bernardinello.kobot.layers.AddConversationLayer
import cloud.bernardinello.kobot.layers.AddMemoryLayer
import cloud.bernardinello.kobot.layers.AddTransportLayer
import cloud.bernardinello.kobot.layers.conversation.ConversationEngine
import cloud.bernardinello.kobot.layers.memory.InMemoryLayer
import cloud.bernardinello.kobot.layers.transport.MyTelegramBot
import cloud.bernardinello.kobot.layers.transport.TelegramTransportLayer
import cloud.bernardinello.kobot.monitoring.MonitorActor
import cloud.bernardinello.kobot.monitoring.StartMonitoring
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@Deprecated("Use services")
class KobotActorService(
    @Autowired val config: BotConfig,
    @Autowired val dbConfig: DatabaseConfig
) {
    companion object {
        val log: Logger = LoggerFactory.getLogger(KobotActorService::class.java)
    }

    val system: ActorSystem
    val transportLayer: ActorRef
    val memoryLayer: ActorRef
    val conversationLayer: List<ActorRef>
    val monitoringActor: ActorRef

    init {
        log.debug("Starting actor system...")
        system = ActorSystem.create("kobot")
        // layers
        transportLayer = system.actorOf(Props.create(TelegramTransportLayer::class.java), "transportLayer")
        memoryLayer = system.actorOf(Props.create(InMemoryLayer::class.java, config), "memoryLayer")
        conversationLayer =
            (0..3).map { system.actorOf(Props.create(ConversationEngine::class.java, config, dbConfig)) }.toList()
        // monitoring
        monitoringActor = system.actorOf(Props.create(MonitorActor::class.java, config), "monitoring")
    }

    fun start() {
        log.trace("Init layers")
        transportLayer.tell(AddMemoryLayer, memoryLayer)
        conversationLayer.forEach { memoryLayer.tell(AddConversationLayer, it) }
        memoryLayer.tell(AddTransportLayer, transportLayer)
        log.trace("Init monitoring")
        monitoringActor.tell(StartMonitoring, monitoringActor)
        log.info("Kobot is up & running")
    }

    fun stop() {
        log.debug("Stopping actors...")
        system.stop(transportLayer)
        system.stop(memoryLayer)
        conversationLayer.forEach { system.stop(it) }
    }

    fun startTelegram(telegramConfig: TelegramConfig) {
        log.trace("Starting telegram bot")
//        ApiContextInitializer.init()
        val telegramBot = MyTelegramBot(telegramConfig, transportLayer)
        transportLayer.tell(telegramBot, transportLayer)
    }
}


//class KobotCommand : CliktCommand() {
//    val loggingLevel: String by option(envvar = "LOGGING_LEVEL", help = "Set the logging level")
//        .choice("trace", "TRACE", "debug", "DEBUG", "info", "INFO", "warn", "WARN", "error", "ERROR")
//        .default("INFO")
//    val conversationConfigPath: Path by option(
//        "--conversation-config",
//        "-config",
//        help = "Conversation json path"
//    ).convert { Paths.get(it) }.required()
//    val telegramConfigPath: Path? by option(
//        "--telegram-config",
//        "-telegram",
//        help = "Telegram credentials json path"
//    ).convert { Paths.get(it) }
//
//    override fun run() {
//        LogUtils.setLogLevel(loggingLevel)
//
//        val config: BotConfig = KobotParser.parse(conversationConfigPath)
//        val kobot = KobotService(config)
//
//        telegramConfigPath?.let {
//            val telegramConfig: TelegramConfig = KobotParser.parse(it)
//            kobot.startTelegram(telegramConfig)
//        }
//
//        kobot.start()
//    }
//}


//fun main(args: Array<String>) {
//    KobotCommand().main(args)
//}