package me.devoxin.flight.api.context

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.CommandClient
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.CompletableFuture

interface Context {
    val contextType: ContextType

    /**
     * The current Context instance as a MessageContext instance,
     * if contextType is ContextType.MESSAGE, otherwise null.
     */
    val asMessageContext: MessageContext?
        get() = this as? MessageContext
    /**
     * The current Context instance as a SlashContext instance,
     * if contextType is ContextType.SLASH, otherwise null.
     */
    val asSlashContext: SlashContext?
        get() = this as? SlashContext

    val commandClient: CommandClient
    val guild: Guild?
    val author: User
    val messageChannel: MessageChannel

    fun respond(content: String): CompletableFuture<*> {
        val sendable = asSlashContext?.event?.reply(content)
            ?: messageChannel.sendMessage(content)

        return sendable.submit()
    }

    fun defaultSend(content: String): CompletableFuture<*> {
        return messageChannel.sendMessage(content).submit()
    }
}
