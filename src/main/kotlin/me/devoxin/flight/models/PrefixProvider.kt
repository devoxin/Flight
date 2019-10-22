package me.devoxin.flight.models

import net.dv8tion.jda.api.entities.Message

interface PrefixProvider {

    // @todo: Get rid of specific types
    fun provide(message: Message): List<String>

}
