package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import java.util.*

class StringParser : Parser<String> {

    override fun parse(ctx: Context, param: String): Optional<String> {
        if (param.isEmpty() || param.isBlank()) {
            return Optional.empty()
        }

        return Optional.of(param)
    }

}