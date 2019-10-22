package me.devoxin.flight.api.jda

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import me.devoxin.flight.api.Context
import me.devoxin.flight.models.Attachment
import me.devoxin.flight.utils.Scheduler
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class JDAContext(
        override val commandClient: JDACommandClient,
        event: MessageReceivedEvent,
        override val trigger: String
) : Context<JDA> {
    override val client: JDA = event.jda
    val message: Message = event.message

    val author: User = event.author

    val guild: Guild? = if (event.isFromGuild) event.guild else null
    val member: Member? = event.member

    val textChannel: TextChannel? = if (event.isFromType(ChannelType.TEXT)) event.textChannel else null
    val privateChannel: PrivateChannel? = if (event.isFromType(ChannelType.PRIVATE)) event.privateChannel else null
    val messageChannel: MessageChannel = event.channel


    override fun send(content: String, callback: ((Message) -> Unit)?) {
        messageChannel.sendMessage(content).queue(callback)
    }

    override fun upload(attachment: Attachment) {
        messageChannel.sendFile(attachment.stream, attachment.filename).queue()
    }

    override fun embed(block: EmbedBuilder.() -> Unit) {
        messageChannel.sendMessage(EmbedBuilder().apply(block).build()).queue()
    }

    override fun dm(content: String) {
        author.openPrivateChannel().queue { channel ->
            channel.sendMessage(content)
                    .submit()
                    .handle { _, _ -> channel.close().queue() }
        }
    }

    override suspend fun sendAsync(content: String): Message {
        return messageChannel.sendMessage(content).submit().await()
    }

    override suspend fun embedAsync(block: EmbedBuilder.() -> Unit): Message {
        return messageChannel.sendMessage(EmbedBuilder().apply(block).build()).submit().await()
    }

    override fun typing(block: () -> Unit) {
        messageChannel.sendTyping().queue {
            val task = Scheduler.every(5000) {
                messageChannel.sendTyping().queue()
            }
            block()
            task.cancel(true)
        }
    }

    override fun typingAsync(block: suspend CoroutineScope.() -> Unit) {
        messageChannel.sendTyping().queue {
            val task = Scheduler.every(5000) {
                messageChannel.sendTyping().queue()
            }

            GlobalScope.launch(block = block)
                .invokeOnCompletion {
                    task.cancel(true)
                }
        }
    }

    // @todo: Move this to an external class
    override suspend fun <T: Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): T? {
        val r = commandClient.waitFor(event, predicate, timeout)
        return r.await()
    }

    // TODO: Method to clean a string.
}
