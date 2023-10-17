package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.util.*

class TextChannelParser : Parser<TextChannel> {
    override fun parse(ctx: MessageContext, param: String): Optional<TextChannel> {
        val snowflake = SnowflakeParser.INSTANCE.parse(ctx, param).takeIf { it.isPresent }?.get()?.resolved

        return when {
            snowflake != null -> Optional.ofNullable(ctx.guild?.getTextChannelById(snowflake))
            else -> Optional.ofNullable(ctx.guild?.textChannelCache?.firstOrNull { it.name == param })
        }
    }
}
