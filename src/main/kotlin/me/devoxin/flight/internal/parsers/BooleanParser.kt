package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class BooleanParser : Parser<Boolean> {
    override fun parse(ctx: MessageContext, param: String): Optional<Boolean> {
        return when (param) {
            in trueExpr -> Optional.of(true)
            in falseExpr -> Optional.of(false)
            else -> Optional.empty()
        }
    }

    companion object {
        val trueExpr = listOf("yes", "y", "true", "t", "1", "enable", "on")
        val falseExpr = listOf("no", "n", "false", "f", "0", "disable", "off")
    }
}
