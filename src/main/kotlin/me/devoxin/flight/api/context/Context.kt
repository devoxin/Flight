package me.devoxin.flight.api.context

import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.internal.entities.Executable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildMessageChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import java.util.concurrent.CompletableFuture

interface Context {
    val invokedCommand: Executable
    val contextType: ContextType

    /**
     * The current Context instance as a MessageContext instance,
     * if contextType is [ContextType.MESSAGE], otherwise null.
     */
    val asMessageContext: MessageContext?
        get() = this as? MessageContext
    /**
     * The current Context instance as a SlashContext instance,
     * if contextType is [ContextType.SLASH], otherwise null.
     */
    val asSlashContext: SlashContext?
        get() = this as? SlashContext

    val commandClient: CommandClient
    val jda: JDA
    val author: User
    val guild: Guild?
    val member: Member?
    val messageChannel: MessageChannel
    val guildChannel: GuildMessageChannel?
    val isFromGuild: Boolean

    fun respond(content: String): CompletableFuture<*> {
        return asSlashContext?.respond0(MessageCreateData.fromContent(content))
            ?: messageChannel.sendMessage(content).submit()
    }

    fun send(content: String) {
        messageChannel.sendMessage(content).submit()
    }
}
