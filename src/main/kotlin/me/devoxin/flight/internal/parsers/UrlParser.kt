package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.net.URL
import java.util.*

class UrlParser : Parser<URL> {
    override fun parse(ctx: MessageContext, param: String): Optional<URL> {
        return try {
            Optional.of(URL(param))
        } catch (e: Throwable) {
            Optional.empty()
        }
    }
}
