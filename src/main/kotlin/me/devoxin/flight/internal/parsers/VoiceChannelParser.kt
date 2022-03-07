package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.MessageContext
import net.dv8tion.jda.api.entities.VoiceChannel
import java.util.*

class VoiceChannelParser : Parser<VoiceChannel> {

    override fun parse(ctx: MessageContext, param: String): Optional<VoiceChannel> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val channel: VoiceChannel? = if (snowflake.isPresent) {
            ctx.guild?.getVoiceChannelById(snowflake.get().resolved)
        } else {
            ctx.guild?.voiceChannels?.firstOrNull { it.name == param }
        }

        return Optional.ofNullable(channel)
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}
