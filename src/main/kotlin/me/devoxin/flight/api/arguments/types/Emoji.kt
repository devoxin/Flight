package me.devoxin.flight.api.arguments.types

import net.dv8tion.jda.api.entities.emoji.CustomEmoji

class Emoji(val name: String, val id: Long, val animated: Boolean) {
    val url: String
        get() {
            val extension = if (animated) "gif" else "png"
            return CustomEmoji.ICON_URL.format(id, extension)
        }
}
