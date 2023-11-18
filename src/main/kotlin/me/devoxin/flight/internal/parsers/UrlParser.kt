package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import java.net.URL
import java.util.*

class UrlParser : Parser<URL> {
    override fun parse(ctx: MessageContext, param: String): URL? {
        return try {
            URL(param)
        } catch (e: Throwable) {
            null
        }
    }
}
