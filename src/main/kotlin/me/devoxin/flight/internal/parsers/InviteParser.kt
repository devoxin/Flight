package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.arguments.types.Invite
import java.util.*

class InviteParser : Parser<Invite> {
    override fun parse(ctx: MessageContext, param: String): Invite? {
        val match = INVITE_PATTERN.matcher(param)

        if (match.find()) {
            val code = match.group(1)
            return Invite(ctx.jda, match.group(), code)
        }

        return null
    }

    companion object {
        val INVITE_PATTERN = "discord(?:(?:app)?\\.com/invite|\\.gg)/([a-zA-Z\\d]{1,16})".toPattern()
    }
}
