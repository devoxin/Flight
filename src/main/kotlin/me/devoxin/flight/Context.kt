package me.devoxin.flight

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


    public fun send(content: String) {
        messageChannel.sendMessage(content).queue()
    }

    public fun dm(content: String) {
        author.openPrivateChannel().queue { channel ->
            channel.sendMessage(content)
                    .submit()
                    .handle { _, _ -> channel.close().queue() }
        }
    }

}