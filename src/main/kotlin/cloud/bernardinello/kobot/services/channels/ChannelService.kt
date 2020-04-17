package cloud.bernardinello.kobot.services.channels

import cloud.bernardinello.kobot.utils.OutputKobotMessage

interface ChannelService {
    fun send(message: OutputKobotMessage)
}