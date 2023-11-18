package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.arguments.types.Emoji
import java.util.*

class EmojiParser : Parser<Emoji> {
    // TODO: Support unicode emoji?
    override fun parse(ctx: MessageContext, param: String): Emoji? {
        val match = EMOJI_PATTERN.matcher(param)

        if (match.find()) {
            val isAnimated = match.group(1) != null
            val name = match.group(2)
            val id = match.group(3).toLong()

            return Emoji(name, id, isAnimated)
        }

        return null
    }

    companion object {
        val EMOJI_PATTERN = "<(a)?:(\\w+):(\\d{17,21})".toPattern()
    }
}
