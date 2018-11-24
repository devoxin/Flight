package me.devoxin.flight

import kotlinx.coroutines.future.await
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Context(
        public val commandClient: CommandClient,
        private val event: MessageReceivedEvent,
        public val trigger: String
) {

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

    public suspend fun sendAsync(content: String): Message {
        return messageChannel.sendMessage(content).submit().await()
    }

    public fun embed(block: EmbedBuilder.() -> Unit) {
        messageChannel.sendMessage(EmbedBuilder().apply(block).build()).queue()
    }

    public suspend fun embedAsync(block: EmbedBuilder.() -> Unit): Message {
        return messageChannel.sendMessage(EmbedBuilder().apply(block).build()).submit().await()
    }

    public fun dm(content: String) {
        author.openPrivateChannel().queue { channel ->
            channel.sendMessage(content)
                    .submit()
                    .handle { _, _ -> channel.close().queue() }
        }
    }

    // TODO: Method to clean a string.

}