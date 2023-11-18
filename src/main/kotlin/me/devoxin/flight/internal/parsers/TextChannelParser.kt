package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import java.util.*

class TextChannelParser : Parser<TextChannel> {
    override fun parse(ctx: MessageContext, param: String): TextChannel? {
        val snowflake = SnowflakeParser.INSTANCE.parse(ctx, param)?.resolved

        return when {
            snowflake != null -> ctx.guild?.getTextChannelById(snowflake)
            else -> ctx.guild?.textChannelCache?.firstOrNull { it.name == param }
        }
    }
}
