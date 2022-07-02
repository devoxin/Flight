package me.devoxin.flight.api.context

import me.devoxin.flight.api.CommandClient
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

class SlashContext(
    override val commandClient: CommandClient,
    val event: SlashCommandInteractionEvent
) : Context {
    override val contextType = ContextType.SLASH
    override val jda: JDA = event.jda
    override val guild = event.guild
    override val author = event.user
    override val messageChannel = event.channel
}
