package me.devoxin.flight.models

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Invite

class Invite(private val jda: JDA,
             public val url: String,
             public val code: String) {

    public fun resolve(success: (Invite) -> Unit, failure: (Throwable) -> Unit) {
        Invite.resolve(jda, code).queue(success, failure)
    }

}