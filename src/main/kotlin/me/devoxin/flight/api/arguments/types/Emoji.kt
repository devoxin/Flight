package me.devoxin.flight.api.arguments.types

import net.dv8tion.jda.api.utils.DiscordAssets
import net.dv8tion.jda.api.utils.ImageFormat

class Emoji(val name: String, val id: Long, val animated: Boolean) {
    val url: String
        get() {
            val format = if (animated) ImageFormat.ANIMATED_WEBP else ImageFormat.STATIC_WEBP
            return DiscordAssets.customEmoji(format, id.toString()).url
        }
}
