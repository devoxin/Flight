package me.devoxin.flight.api.context

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.internal.entities.Executable
import me.devoxin.flight.internal.utils.Scheduler
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.FileUpload
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import java.util.regex.Pattern

class MessageContext(
    override val commandClient: CommandClient,
    event: MessageReceivedEvent,
    val trigger: String,
    override val invokedCommand: Executable
) : Context {
    override val contextType = ContextType.MESSAGE
    override val jda: JDA = event.jda
    override val author = event.author
    override val guild = event.takeIf { it.isFromGuild }?.guild
    override val member = event.member
    override val messageChannel = event.channel
    override val guildChannel = event.takeIf { it.isFromGuild }?.guildChannel
    override val isFromGuild = event.isFromGuild

    val message: Message = event.message

    val textChannel: TextChannel? = messageChannel as? TextChannel
    val privateChannel: PrivateChannel? = messageChannel as? PrivateChannel

    /**
     * Sends a message embed to the channel the Context was created from.
     *
     * @param content
     *        The content of the message.
     */
    override fun send(content: String) {
        send0({ setContent(content) }).submit()
    }

    /**
     * Sends a file to the channel the Context was created from.
     *
     * @param attachment
     *        The attachment to send.
     */
    fun send(attachment: FileUpload) {
        send0(null, attachment).submit()
    }

    /**
     * Sends a message embed to the channel the Context was created from.
     *
     * @param embed
     *        Options to apply to the message embed.
     */
    fun send(embed: EmbedBuilder.() -> Unit) {
        send0({ setEmbeds(EmbedBuilder().apply(embed).build()) }).submit()
    }

    /**
     * Sends a message embed to the channel the Context was created from.
     *
     * @param content
     *        The content of the message.
     *
     * @return The created message.
     */
    suspend fun sendAsync(content: String): Message {
        return send0({ setContent(content) }).submit().await()
    }

    /**
     * Sends a file to the channel the Context was created from.
     *
     * @param attachment
     *        The attachment to send.
     *
     * @return The created message.
     */
    suspend fun sendAsync(attachment: FileUpload): Message {
        return send0(null, attachment).submit().await()
    }

    /**
     * Sends a message embed to the channel the Context was created from.
     *
     * @param embed
     *        Options to apply to the message embed.
     *
     * @return The created message.
     */
    suspend fun sendAsync(embed: EmbedBuilder.() -> Unit): Message {
        return send0({ setEmbeds(EmbedBuilder().apply(embed).build()) }).submit().await()
    }

    /**
     * Sends the message author a direct message.
     *
     * @param content
     *        The content of the message.
     */
    fun sendPrivate(content: String) {
        author.openPrivateChannel().submit()
            .thenAccept {
                it.sendMessage(content).submit()
                    .handle { _, _ -> it.delete().submit() }
            }
    }

    private fun send0(messageOpts: (MessageCreateBuilder.() -> Unit)? = null, vararg files: FileUpload): RestAction<Message> {
        if (messageOpts == null && files.isEmpty()) {
            throw IllegalArgumentException("Cannot send a message with no options or attachments!")
        }

        val builder = MessageCreateBuilder()
        messageOpts?.let(builder::apply)

        files.takeIf { it.isNotEmpty() }?.let {
            builder.addFiles(*it)
        }

        return messageChannel.sendMessage(builder.build())
    }

    /**
     * Sends a typing status within the channel until the provided function is exited.
     *
     * @param block
     *        The code that should be executed while the typing status is active.
     */
    fun typing(block: () -> Unit) {
        messageChannel.sendTyping().queue {
            val task = Scheduler.every(5000) {
                messageChannel.sendTyping().queue()
            }
            block()
            task.cancel(true)
        }
    }

    /**
     * Sends a typing status within the channel until the provided function is exited.
     *
     * @param block
     *        The code that should be executed while the typing status is active.
     */
    suspend fun typingAsync(block: suspend () -> Unit) {
        messageChannel.sendTyping().submit().await()
        val task = Scheduler.every(5000) { messageChannel.sendTyping().queue() }

        try {
            block()
        } finally {
            task.cancel(true)
        }
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
    suspend fun <T: Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): T {
        val r = commandClient.waitFor(event, predicate, timeout)
        return r.await()
    }

    /**
     * Cleans a string, sanitizing all forms of mentions (role, channel and user), replacing them with
     * their display-equivalent where possible (For example, <@123456789123456789> becomes @User).
     *
     * For cases where the mentioned entity is not cached by the bot, the mention will be replaced
     * with @invalid-<entity type>.
     *
     * It's recommended that you use this only for sending responses back to a user.
     *
     * @param str
     *        The string to clean.
     *
     * @returns The sanitized string.
     */
    fun cleanContent(str: String): String {
        var content = str.replace("e", "е")
        // We use a Cyrillic "e" instead of \u200b as it keeps character count the same.
        val matcher = mentionPattern.matcher(str)

        while (matcher.find()) {
            val entityType = matcher.group("type")
            val entityId = matcher.group("id").toLong()
            val fullEntity = matcher.group("mention")

            when (entityType) {
                "@", "@!" -> {
                    val entity = guild?.getMemberById(entityId)?.effectiveName
                        ?: jda.getUserById(entityId)?.name
                        ?: "invalid-user"
                    content = content.replace(fullEntity, "@$entity")
                }
                "@&" -> {
                    val entity = jda.getRoleById(entityId)?.name ?: "invalid-role"
                    content = content.replace(fullEntity, "@$entity")
                }
                "#" -> {
                    val entity = jda.getTextChannelById(entityId)?.name ?: "invalid-channel"
                    content = content.replace(fullEntity, "#$entity")
                }
            }
        }

        for (emote in message.mentions.customEmojis) {
            content = content.replace(emote.asMention, ":${emote.name}:")
        }

        return content
    }

    companion object {
        private val mentionPattern = Pattern.compile("(?<mention><(?<type>@!?|@&|#)(?<id>[0-9]{17,21})>)")
    }
}
