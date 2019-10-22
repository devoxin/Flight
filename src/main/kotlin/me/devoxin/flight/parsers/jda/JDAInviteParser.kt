package me.devoxin.flight.parsers.jda

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.jda.JDAContext
import me.devoxin.flight.models.JDAInvite
import me.devoxin.flight.parsers.Parser
import net.dv8tion.jda.api.JDA
import java.util.*
import java.util.regex.Pattern

class JDAInviteParser : Parser<JDAInvite> {

    override fun parse(ctx: Context<*>, param: String): Optional<JDAInvite> {
        require(ctx is JDAContext) { "Wrong context type" }
        val match = INVITE_REGEX.matcher(param)

        if (match.find()) {
            val code = match.group(1)
            return Optional.of(JDAInvite(ctx.client, match.group(), code))
        }

        return Optional.empty()
    }

    companion object {
        val INVITE_REGEX = Pattern.compile("discord(?:(?:app)?\\.com/invite|\\.gg)/([a-zA-Z0-9]{1,16})")!!
    }

}
