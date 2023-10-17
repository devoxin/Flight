package me.devoxin.flight.api.arguments.types

import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Invite

class Invite(
    private val jda: JDA,
    val url: String,
    val code: String
) {
    fun resolve(withCounts: Boolean = false) = Invite.resolve(jda, code, withCounts)

    suspend fun resolveAsync(withCounts: Boolean = false) = Invite.resolve(jda, code, withCounts).submit().await()
}
