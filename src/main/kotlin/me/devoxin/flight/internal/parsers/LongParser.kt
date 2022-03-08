package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class LongParser : Parser<Long> {
    override fun parse(ctx: MessageContext, param: String): Optional<Long> {
        return Optional.ofNullable(param.toLongOrNull())
    }
}
