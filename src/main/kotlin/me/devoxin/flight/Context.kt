package me.devoxin.flight

import com.mewna.catnip.Catnip
import com.mewna.catnip.entity.builder.EmbedBuilder
import com.mewna.catnip.entity.channel.MessageChannel
import com.mewna.catnip.entity.channel.TextChannel
import com.mewna.catnip.entity.guild.Guild
import com.mewna.catnip.entity.guild.Member
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.entity.message.MessageOptions
import com.mewna.catnip.entity.user.User
import com.mewna.catnip.shard.event.EventType
import kotlinx.coroutines.future.await
import me.devoxin.flight.models.Attachment
import java.util.concurrent.TimeUnit

class Context(
        public val commandClient: CommandClient,
        public val message: Message,
        public val trigger: String) {

    public val catnip: Catnip = message.catnip()

    public val guild: Guild? = message.guild()

    public val author: User = message.author()
    public val member: Member? = message.member()

    public val messageChannel: MessageChannel = message.channel()
    public val textChannel: TextChannel? = messageChannel as? TextChannel

    public fun send(content: String, callback: ((Message) -> Unit)? = null) {
        messageChannel.sendMessage(content).thenAccept(callback)
    }

    public fun upload(attachment: Attachment) {
        messageChannel.sendMessage(MessageOptions().addFile(attachment.filename, attachment.stream))
    }

    public fun embed(block: EmbedBuilder.() -> Unit) {
        messageChannel.sendMessage(EmbedBuilder().apply(block).build())
    }

    public fun dm(content: String) {
        author.createDM().thenAccept {
            it.sendMessage(content).thenRun { it.delete() }
        }
    }

    public suspend fun sendAsync(content: String): Message {
        return messageChannel.sendMessage(content).await()
    }

    public suspend fun embedAsync(block: EmbedBuilder.() -> Unit): Message {
        return messageChannel.sendMessage(EmbedBuilder().apply(block).build()).await()
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
    public suspend fun <T : EventType<T>> waitFor(event: EventType<T>, predicate: (T) -> Boolean, timeout: Long): T? {
        val r = commandClient.waitFor(event, predicate, timeout)
        return r.await()
    }
    // TODO: Method to clean a string.

}
