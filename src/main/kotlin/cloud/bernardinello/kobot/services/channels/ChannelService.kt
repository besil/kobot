package cloud.bernardinello.kobot.services.channels

import cloud.bernardinello.kobot.layers.OutputKobotMessage

interface ChannelService {
    fun send(message: OutputKobotMessage)
}