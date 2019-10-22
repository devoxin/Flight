package me.devoxin.flight.parsers.jda

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.jda.JDAContext
import me.devoxin.flight.parsers.Parser
import me.devoxin.flight.parsers.SnowflakeParser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import java.util.*

class JDATextChannelParser : Parser<TextChannel> {

    override fun parse(ctx: Context<*>, param: String): Optional<TextChannel> {
        require(ctx is JDAContext) { "Wrong context type" }
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
