package me.devoxin.flight.api.entities

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder

class DSLMessageCreateBuilder : MessageCreateBuilder() {
    fun embed(builder: EmbedBuilder.() -> Unit) {
        addEmbeds(EmbedBuilder().apply(builder).build())
    }
}
