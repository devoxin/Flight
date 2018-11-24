package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import net.dv8tion.jda.core.entities.TextChannel
import java.util.*

class TextChannelParser : Parser<TextChannel> {

    override fun parse(ctx: Context, param: String): Optional<TextChannel> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val channel: TextChannel? = if (snowflake.isPresent) {
            ctx.guild?.getTextChannelById(snowflake.get())
        } else {
            ctx.guild?.textChannels?.firstOrNull { it.name == param }
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