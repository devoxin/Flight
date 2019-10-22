package me.devoxin.flight.parsers.jda

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.jda.JDAContext
import me.devoxin.flight.parsers.Parser
import me.devoxin.flight.parsers.SnowflakeParser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.VoiceChannel
import java.util.*

class JDAVoiceChannelParser : Parser<VoiceChannel> {

    override fun parse(ctx: Context<*>, param: String): Optional<VoiceChannel> {
        require(ctx is JDAContext) { "Wrong context type" }
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
