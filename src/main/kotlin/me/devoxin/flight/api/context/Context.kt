package me.devoxin.flight.api.context

import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.api.entities.DSLMessageCreateBuilder
import me.devoxin.flight.internal.entities.Executable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.utils.FileUpload
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

    /**
     * Sends "Bot is thinking..." for slash commands, or a typing indicator for message commands.
     *
     * @param ephemeral
     *        Whether the response should only be seen by the invoking user.
     *        This only applies to slash commands.
     */
    fun think(ephemeral: Boolean = false): CompletableFuture<*> {
        return asSlashContext?.defer0(ephemeral)
            ?: messageChannel.sendTyping().submit()
    }

    /**
     * Convenience method for replying to either a slash command event, or a message event.
     * This will acknowledge, and correctly respond to slash command events, if applicable.
     *
     * @param content
     *        The response content to send.
     */
    fun respond(content: String): CompletableFuture<*> {
        return asSlashContext?.respond0(MessageCreateData.fromContent(content))
            ?: messageChannel.sendMessage(content).submit()
    }

    /**
     * Convenience method for replying to either a slash command event, or a message event.
     * This will acknowledge, and correctly respond to slash command events, if applicable.
     *
     * @param file
     *        The file to send.
     */
    fun respond(file: FileUpload): CompletableFuture<*> {
        return asSlashContext?.respond0(MessageCreateData.fromFiles(file))
            ?: messageChannel.sendFiles(file).submit()
    }

    /**
     * Convenience method for replying to either a slash command event, or a message event.
     * This will acknowledge, and correctly respond to slash command events, if applicable.
     *
     * @param message
     *        The message data to send.
     */
    fun respond(message: MessageCreateData): CompletableFuture<*> {
        return asSlashContext?.respond0(message)
            ?: messageChannel.sendMessage(message).submit()
    }

    /**
     * Convenience method for replying to either a slash command event, or a message event.
     * This will acknowledge, and correctly respond to slash command events, if applicable.
     *
     * @param messageBuilder
     *        The options to apply when creating a response.
     */
    fun respond(messageBuilder: DSLMessageCreateBuilder.() -> Unit): CompletableFuture<*> {
        val built = DSLMessageCreateBuilder().apply(messageBuilder).build()

        return asSlashContext?.respond0(built)
            ?: messageChannel.sendMessage(built).submit()
    }

    /**
     * Sends a message to the channel. This has no special handling, and could cause
     * problems with slash command events, so use with caution.
     *
     * @param content
     *        The response content to send.
     */
    fun send(content: String) {
        messageChannel.sendMessage(content).submit()
    }

    /**
     * Sends the message author a direct message.
     *
     * @param content
     *        The content of the message.
     */
    fun sendPrivate(content: String): CompletableFuture<*> {
        return author.openPrivateChannel()
            .submit()
            .thenCompose { it.sendMessage(content).submit() }
            .thenCompose { it.channel.asPrivateChannel().delete().submit() }
    }

    /**
     * Sends the message author a direct message.
     * This is intended as a lower level function (compared to the other send methods)
     * to offer more functionality when needed.
     *
     * @param message
     *        The message to send.
     *
     * @return The message that was sent.
     */
    fun sendPrivate(message: MessageCreateData): CompletableFuture<*> {
        return author.openPrivateChannel()
            .submit()
            .thenCompose { it.sendMessage(message).submit() }
            .thenCompose { it.channel.asPrivateChannel().delete().submit() }
    }

    /**
     * Waits for the given event. Only events that pass the given predicate will be returned.
     * If the timeout is exceeded with no results then null will be returned.
     *
     * @param predicate
     *        A function that must return a boolean denoting whether the event meets the given criteria.
     *
     * @param timeout
     *        How long to wait, in milliseconds, for the given event type before expiring.
     *
     * @throws java.util.concurrent.TimeoutException
     */
    fun <T: Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T> {
        return commandClient.waitFor(event, predicate, timeout)
    }
}
