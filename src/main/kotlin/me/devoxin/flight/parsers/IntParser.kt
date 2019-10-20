package me.devoxin.flight.parsers

import me.devoxin.flight.api.Context
import java.util.*

class IntParser : Parser<Int> {

    override fun parse(ctx: Context, param: String): Optional<Int> {
        val i = param.toIntOrNull() ?: return Optional.empty()
        return Optional.of(i)
    }

}