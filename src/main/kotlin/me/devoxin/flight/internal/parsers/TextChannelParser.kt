package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.entities.TextChannel
import java.util.*

class TextChannelParser : Parser<TextChannel> {

    override fun parse(ctx: Context, param: String): Optional<TextChannel> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val channel: TextChannel? = if (snowflake.isPresent) {
            ctx.guild?.getTextChannelById(snowflake.get().resolved)
        } else {
            ctx.guild?.textChannels?.firstOrNull { it.name == param }
        }

        return Optional.ofNullable(channel)
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}
