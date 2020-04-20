package me.devoxin.flight.api.entities

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Invite

class Invite(
    private val jda: JDA,
    val url: String,
    val code: String
) {
    fun resolve() = Invite.resolve(jda, code)
}
