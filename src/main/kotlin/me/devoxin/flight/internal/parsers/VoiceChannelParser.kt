package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import java.util.*

class VoiceChannelParser : Parser<VoiceChannel> {
    override fun parse(ctx: MessageContext, param: String): Optional<VoiceChannel> {
        val snowflake = SnowflakeParser.INSTANCE.parse(ctx, param).takeIf { it.isPresent }?.get()?.resolved

        return when {
            snowflake != null -> Optional.ofNullable(ctx.guild?.getVoiceChannelById(snowflake))
            else -> Optional.ofNullable(ctx.guild?.voiceChannelCache?.firstOrNull { it.name == param })
        }
    }
}
