package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import java.util.*

class DoubleParser : Parser<Double> {

    override fun parse(ctx: Context, param: String): Optional<Double> {
        return Optional.ofNullable(param.toDoubleOrNull())
    }

}
