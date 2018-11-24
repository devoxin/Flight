package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import java.util.*
import java.util.regex.Pattern

class SnowflakeParser : Parser<Long> {

    override fun parse(ctx: Context, param: String): Optional<Long> {
        val match = snowflakeMatch.matcher(param)

        if (match.find()) { // todo use .matches() for single args
            return Optional.of(match.group().toLong())
        }

        return Optional.empty()
    }

    companion object {
        private val snowflakeMatch: Pattern = Pattern.compile("[0-9]{17,20}")
    }

}