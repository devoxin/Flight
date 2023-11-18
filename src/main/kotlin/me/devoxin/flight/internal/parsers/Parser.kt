package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.util.*

interface Parser<T> {
    fun parse(ctx: MessageContext, param: String): T?
}
