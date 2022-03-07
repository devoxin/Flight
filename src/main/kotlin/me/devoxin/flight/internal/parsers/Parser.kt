package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.MessageContext
import java.util.*

interface Parser<T> {
    fun parse(ctx: MessageContext, param: String): Optional<T>
}
