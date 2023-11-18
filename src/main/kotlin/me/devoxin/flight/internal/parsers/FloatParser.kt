package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class FloatParser : Parser<Float> {
    override fun parse(ctx: MessageContext, param: String): Float? {
        return param.toFloatOrNull()
    }
}
