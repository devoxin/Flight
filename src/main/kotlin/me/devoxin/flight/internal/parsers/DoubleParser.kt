package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.MessageContext
import java.util.*

class DoubleParser : Parser<Double> {

    override fun parse(ctx: MessageContext, param: String): Optional<Double> {
        return Optional.ofNullable(param.toDoubleOrNull())
    }

}
