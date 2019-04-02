package me.devoxin.flight.models

import com.mewna.catnip.entity.message.Message

interface PrefixProvider {

    fun provide(message: Message): List<String>

}
