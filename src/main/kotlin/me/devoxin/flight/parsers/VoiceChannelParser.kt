package me.devoxin.flight.parsers

import com.mewna.catnip.entity.channel.VoiceChannel
import me.devoxin.flight.Context
import java.util.*

class VoiceChannelParser : Parser<VoiceChannel> {

    override fun parse(ctx: Context, param: String): Optional<VoiceChannel> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val channel: VoiceChannel? = if (snowflake.isPresent) {
            ctx.guild?.voiceChannel(snowflake.get())
        } else {
            ctx.guild?.channels()?.filter { it.isVoice }?.firstOrNull { it.name() == param }?.asVoiceChannel()
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
