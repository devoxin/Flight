package me.devoxin.flight.parsers

import me.devoxin.flight.api.Context
import java.util.*

class DoubleParser : Parser<Double> {

    override fun parse(ctx: Context, param: String): Optional<Double> {
        val d = param.toDoubleOrNull() ?: return Optional.empty()
        return Optional.of(d)
    }

}