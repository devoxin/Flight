package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

class BooleanParser : Parser<Boolean> {
    override fun parse(ctx: MessageContext, param: String): Boolean? {
        return when (param) {
            in trueExpr -> true
            in falseExpr -> false
            else -> null
        }
    }

    companion object {
        val trueExpr = listOf("yes", "y", "true", "t", "1", "enable", "on")
        val falseExpr = listOf("no", "n", "false", "f", "0", "disable", "off")
    }
}
