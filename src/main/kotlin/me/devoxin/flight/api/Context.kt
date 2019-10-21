package me.devoxin.flight.api

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.devoxin.flight.models.Attachment
import me.devoxin.flight.utils.Scheduler

class Context(
        val commandClient: CommandClient,
        val message: Message,
        val trigger: String
) {

    val catnip: Catnip = message.catnip()

    val guild: Guild? = message.guild()

    val author: User = message.author()
    val member: Member? = message.member()

    val messageChannel: MessageChannel = message.channel()
    val textChannel: TextChannel? = messageChannel as? TextChannel

    fun send(content: String, callback: ((Message) -> Unit)? = null) {
        if (callback != null) {
            messageChannel.sendMessage(content).thenAccept(callback)
        } else {
            messageChannel.sendMessage(content)
        }
    }

    fun upload(attachment: Attachment) {
        messageChannel.sendMessage(MessageOptions().addFile(attachment.filename, attachment.stream))
    }

    fun embed(block: EmbedBuilder.() -> Unit) {
        messageChannel.sendMessage(EmbedBuilder().apply(block).build())
    }

    fun dm(content: String) {
        author.createDM().thenAccept {
            it.sendMessage(content).thenRun { it.delete() }
        }
    }

    suspend fun sendAsync(content: String): Message {
        return messageChannel.sendMessage(content).await()
    }

    suspend fun embedAsync(block: EmbedBuilder.() -> Unit): Message {
        return messageChannel.sendMessage(EmbedBuilder().apply(block).build()).await()
    }

    /**
     * Sends a typing status within the channel until the provided function is exited.
     *
     * @param block
     *        The code that should be executed while the typing status is active.
     */
    fun typing(block: () -> Unit) {
        messageChannel.type()
        messageChannel.triggerTypingIndicator().thenAccept {
            val task = Scheduler.every(5000) {
                messageChannel.triggerTypingIndicator()
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
    fun typingAsync(block: suspend CoroutineScope.() -> Unit) {
        messageChannel.triggerTypingIndicator().thenAccept {
            val task = Scheduler.every(5000) {
                messageChannel.triggerTypingIndicator()
            }

            GlobalScope.launch(block = block)
                    .invokeOnCompletion {
                        task.cancel(true)
                    }
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
     */
    suspend fun <T : EventType<T>> waitFor(event: EventType<T>, predicate: (T) -> Boolean, timeout: Long): T? {
        val r = commandClient.waitFor(event, predicate, timeout)
        return r.await()
    }
    // TODO: Method to clean a string.
}
