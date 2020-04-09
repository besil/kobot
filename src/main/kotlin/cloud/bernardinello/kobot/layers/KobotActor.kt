package cloud.bernardinello.kobot.layers

import akka.actor.UntypedAbstractActor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

abstract class KobotActor : UntypedAbstractActor() {
    companion object {
        val log: Logger = LoggerFactory.getLogger(KobotActor::class.java)
    }

    final override fun onReceive(message: Any) {
        try {
            val m: Method = this.javaClass.getMethod("onReceive", message::class.java)
            m.invoke(this, message)
        } catch (e: Exception) {
            log.warn("{}", e)
            log.info("{} error processing message: {}", this::class.simpleName, message)
        }
    }
}