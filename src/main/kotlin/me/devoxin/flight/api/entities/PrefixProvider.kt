package me.devoxin.flight.api.entities

import net.dv8tion.jda.api.entities.Message

interface PrefixProvider {

    fun provide(message: Message): List<String>

}
