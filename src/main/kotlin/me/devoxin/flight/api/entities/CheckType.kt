package me.devoxin.flight.api.entities

enum class CheckType {
    // Emitted when a command wasn't used in the right context (e.g. Slash command as a Message command).
    EXECUTION_CONTEXT,
    // Emitted when a command is marked guildOnly, but isn't used in a guild.
    GUILD_CHECK,
    // Emitted when a command is marked NSFW, but isn't used in an NSFW channel.
    NSFW_CHECK,
    // Emitted when a command is marked developerOnly, but isn't used by a developer.
    DEVELOPER_CHECK
}
