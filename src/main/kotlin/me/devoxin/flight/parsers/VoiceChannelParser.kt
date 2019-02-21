package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import net.dv8tion.jda.api.entities.VoiceChannel
import java.util.*

class VoiceChannelParser : Parser<VoiceChannel> {

    override fun parse(ctx: Context, param: String): Optional<VoiceChannel> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val channel: VoiceChannel? = if (snowflake.isPresent) {
            ctx.guild?.getVoiceChannelById(snowflake.get())
        } else {
            ctx.guild?.voiceChannels?.firstOrNull { it.name == param }
        }

        if (channel != null) {
            return Optional.of(channel)
        }

        return Optional.empty()
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}