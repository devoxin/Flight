package me.devoxin.flight.parsers

import me.devoxin.flight.api.Context
import java.util.*
import java.util.regex.Pattern

class SnowflakeParser : Parser<Long> {

    override fun parse(ctx: Context, param: String): Optional<Long> {
        val match = snowflakeMatch.matcher(param)

        if (match.matches()) {
            val id = match.group("sid") ?: match.group("id")
            return Optional.of(id.toLong())
        }

        return Optional.empty()
    }

    companion object {
        private val snowflakeMatch = Pattern.compile("^(?:<(?:@!?|@&|#)(?<sid>[0-9]{17,21})>|(?<id>[0-9]{17,21}))$")
    }

}
