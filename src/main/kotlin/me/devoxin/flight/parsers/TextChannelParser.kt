package me.devoxin.flight.parsers

import com.mewna.catnip.entity.channel.TextChannel
import me.devoxin.flight.Context
import java.util.*

class TextChannelParser : Parser<TextChannel> {

    override fun parse(ctx: Context, param: String): Optional<TextChannel> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val channel: TextChannel? = if (snowflake.isPresent) {
            ctx.guild?.channel(snowflake.get())?.asTextChannel()
        } else {
            ctx.guild?.channels()?.filter { it.isText }?.firstOrNull { it.name() == param }?.asTextChannel()
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
