package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.MessageContext
import java.util.*

class IntParser : Parser<Int> {

    override fun parse(ctx: MessageContext, param: String): Optional<Int> {
        return Optional.ofNullable(param.toIntOrNull())
    }

}
