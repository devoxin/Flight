package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import me.devoxin.flight.internal.arguments.types.Snowflake
import java.util.*
import java.util.regex.Pattern

class SnowflakeParser : Parser<Snowflake> {

    override fun parse(ctx: Context, param: String): Optional<Snowflake> {
        val match = snowflakeMatch.matcher(param)

        if (match.matches()) {
            val id = match.group("sid") ?: match.group("id")
            return Optional.of(Snowflake(id.toLong()))
        }

        return Optional.empty()
    }

    companion object {
        private val snowflakeMatch = Pattern.compile("^(?:<(?:@!?|@&|#)(?<sid>[0-9]{17,21})>|(?<id>[0-9]{17,21}))$")
    }

}
