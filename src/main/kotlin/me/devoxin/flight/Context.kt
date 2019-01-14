package me.devoxin.flight

import kotlinx.coroutines.future.await
import me.devoxin.flight.models.Attachment
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

class Context(
        public val commandClient: CommandClient,
        private val event: MessageReceivedEvent,
        public val trigger: String) {

    public val jda: JDA = event.jda
    public val message: Message = event.message

    public val author: User = event.author
    public val privateChannel: PrivateChannel? = event.privateChannel

    public val guild: Guild? = event.guild
    public val member: Member? = event.member
    public val textChannel: TextChannel? = event.textChannel

    public val messageChannel: MessageChannel = event.channel


    public fun send(content: String, callback: ((Message) -> Unit)? = null) {
        messageChannel.sendMessage(content).queue(callback)
    }

    public fun upload(attachment: Attachment) {
        messageChannel.sendFile(attachment.stream, attachment.filename).queue()
    }

    public fun embed(block: EmbedBuilder.() -> Unit) {
        messageChannel.sendMessage(EmbedBuilder().apply(block).build()).queue()
    }

    public fun dm(content: String) {
        author.openPrivateChannel().queue { channel ->
            channel.sendMessage(content)
                    .submit()
                    .handle { _, _ -> channel.close().queue() }
        }
    }

    public suspend fun sendAsync(content: String): Message {
        return messageChannel.sendMessage(content).submit().await()
    }

    public suspend fun embedAsync(block: EmbedBuilder.() -> Unit): Message {
        return messageChannel.sendMessage(EmbedBuilder().apply(block).build()).submit().await()
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
     */
    public suspend fun <T: Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): T? {
        val r = commandClient.waitFor(event, predicate, timeout)
        return r.await()
    }

    // TODO: Method to clean a string.

}