package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class IntParser : Parser<Int> {
    override fun parse(ctx: MessageContext, param: String): Int? {
        return param.toIntOrNull()
    }
}
