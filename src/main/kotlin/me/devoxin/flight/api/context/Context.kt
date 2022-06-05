package me.devoxin.flight.api.context

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.api.entities.Attachment
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
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

    fun respond(embed: EmbedBuilder.() -> Unit): CompletableFuture<*> {
        val sendable = slashSendBuilder({ setEmbeds(EmbedBuilder().apply(embed).build()) })
            ?: sendBuilder({ setEmbeds(EmbedBuilder().apply(embed).build()) })

        return sendable.submit()
    }


    fun defaultSend(content: Message): CompletableFuture<*> {
        return messageChannel.sendMessage(content).submit()
    }

    fun defaultSend(content: String): CompletableFuture<*> {
        return messageChannel.sendMessage(content).submit()
    }

    fun sendBuilder(messageOpts: (MessageBuilder.() -> Unit)? = null, vararg files: Attachment): RestAction<Message> {
        if (messageOpts == null && files.isEmpty()) {
            throw IllegalArgumentException("Cannot send a message with no options or attachments!")
        }

        val builtMessage = messageOpts?.let(MessageBuilder()::apply)?.build()
            ?: MessageBuilder().build()

        return messageChannel.sendMessage(builtMessage).also {
            if (files.isNotEmpty()) {
                for (file in files) {
                    it.addFile(file.stream, file.filename, *file.attachmentOptions)
                }
            }
        }
    }

    fun slashSendBuilder(messageOpts: (MessageBuilder.() -> Unit)? = null, vararg files: Attachment): ReplyCallbackAction? {
        if (messageOpts == null && files.isEmpty()) {
            throw IllegalArgumentException("Cannot send a message with no options or attachments!")
        }

        val builtMessage = messageOpts?.let(MessageBuilder()::apply)?.build()
            ?: MessageBuilder().build()

        return asSlashContext?.event?.reply(builtMessage).also {
            if(files.isNotEmpty()) {
                for(file in files) {
                    it?.addFile(file.stream, file.filename, *file.attachmentOptions)
                }
            }
        }
    }
}
