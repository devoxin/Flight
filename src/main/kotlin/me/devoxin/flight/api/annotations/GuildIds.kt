package me.devoxin.flight.api.annotations

/**
 * The GuildIds that this command may be run within.
 * For slash commands, this will assign the command as a guild command.
 * For message commands, this will be enforced via a check before a command is executed.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class GuildIds(
    val value: LongArray
)
