package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.internal.arguments.types.Snowflake
import java.util.*
import java.util.regex.Pattern

class SnowflakeParser : Parser<Snowflake> {
    override fun parse(ctx: MessageContext, param: String): Optional<Snowflake> {
        val match = snowflakeMatch.matcher(param)

        if (match.matches()) {
            val id = match.group("sid") ?: match.group("id")
            return Optional.of(Snowflake(id.toLong()))
        }

        return Optional.empty()
    }

    companion object {
        val snowflakeMatch = "^(?:<(?:@!?|@&|#)(?<sid>\\d{17,21})>|(?<id>\\d{17,21}))$".toPattern()
    }
}
