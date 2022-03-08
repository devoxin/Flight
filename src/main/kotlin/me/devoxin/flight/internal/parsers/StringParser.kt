package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class StringParser : Parser<String> {

    override fun parse(ctx: MessageContext, param: String): Optional<String> {
        if (param.isEmpty() || param.isBlank()) {
            return Optional.empty()
        }

        return Optional.of(param)
    }

}
