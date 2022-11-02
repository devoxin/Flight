package me.devoxin.flight.api.entities

import net.dv8tion.jda.api.entities.Message

class DefaultPrefixProvider(
        private val prefixes: List<String>,
        private val allowMentionPrefix: Boolean
) : PrefixProvider {

    override fun provide(message: Message): List<String> {
        val prefixes = mutableListOf<String>().apply {
            addAll(this@DefaultPrefixProvider.prefixes)
        }

        if (allowMentionPrefix) {
            val selfUserId = message.jda.selfUser.id
            prefixes.add("<@$selfUserId> ")
            prefixes.add("<@!$selfUserId> ")
        }

        return prefixes.toList()
    }

}
