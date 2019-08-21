package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import me.devoxin.flight.models.Invite
import java.util.*
import java.util.regex.Pattern

class InviteParser : Parser<Invite> {

    override fun parse(ctx: Context, param: String): Optional<Invite> {
        val match = INVITE_REGEX.matcher(param)

        if (match.find()) {
            val code = match.group(1)
            return Optional.of(Invite(ctx.jda, match.group(), code))
        }

        return Optional.empty()
    }

    companion object {
        val INVITE_REGEX = Pattern.compile("discord(?:(?:app)?\\.com/invite|\\.gg)/([a-zA-Z0-9]{1,16})")!!
    }

}