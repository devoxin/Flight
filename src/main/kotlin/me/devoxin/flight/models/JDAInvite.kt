package me.devoxin.flight.models

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Invite

class JDAInvite(private val jda: JDA,
                val url: String,
                val code: String) {

    fun resolve(success: (Invite) -> Unit, failure: (Throwable) -> Unit) {
        Invite.resolve(jda, code).queue(success, failure)
    }

}
