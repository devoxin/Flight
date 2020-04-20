package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import java.util.*

class LongParser : Parser<Long> {
    override fun parse(ctx: Context, param: String): Optional<Long> {
        return Optional.ofNullable(param.toLongOrNull())
    }
}
