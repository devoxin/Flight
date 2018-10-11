package me.devoxin.flight

import net.dv8tion.jda.core.entities.Message

class DefaultPrefixProvider(
        private val prefixes: List<String>,
        private val allowMentionPrefix: Boolean
) : PrefixProvider {

    override fun provide(message: Message): List<String> {
        val prefixes = mutableListOf<String>()

        if (allowMentionPrefix) {
            val selfUserId = message.jda.selfUser.id
            prefixes.add("<@$selfUserId> ")
            prefixes.add("<@!$selfUserId> ")
        }

        prefixes.addAll(this.prefixes)

        return prefixes.toList()
    }

}
