package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class DoubleParser : Parser<Double> {
    override fun parse(ctx: MessageContext, param: String): Double? {
        return param.toDoubleOrNull()
    }
}
