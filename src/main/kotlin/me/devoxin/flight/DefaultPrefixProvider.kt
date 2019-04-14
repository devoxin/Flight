package me.devoxin.flight

import com.mewna.catnip.entity.message.Message
import me.devoxin.flight.models.PrefixProvider

class DefaultPrefixProvider(
        private val prefixes: List<String>,
        private val allowMentionPrefix: Boolean
) : PrefixProvider {
    override fun provide(message: Message): List<String> {
        val prefixes = mutableListOf<String>()

        if (allowMentionPrefix) {
            val selfUserId = message.catnip().selfUser()!!.id()
            prefixes.add("<@$selfUserId> ")
            prefixes.add("<@!$selfUserId> ")
        }

        prefixes.addAll(this.prefixes)
        return prefixes.toList()
    }
}
