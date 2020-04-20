package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import java.util.*

class IntParser : Parser<Int> {

    override fun parse(ctx: Context, param: String): Optional<Int> {
        return Optional.ofNullable(param.toIntOrNull())
    }

}
