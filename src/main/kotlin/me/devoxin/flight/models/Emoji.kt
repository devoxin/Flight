package me.devoxin.flight.models

class Emoji(public val name: String,
            public val id: Long,
            public val animated: Boolean) {

    val url: String
            get() {
                val extension = if (animated) "gif" else "png"
                return "https://cdn.discordapp.com/emojis/$id.$extension"
            }

}
