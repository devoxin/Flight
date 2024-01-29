package me.devoxin.flight.api.annotations

/**
 * Marks a function as subcommand.
 *
 * Subcommands cannot co-exist with multiple parent commands (marked with @Command).
 * If a cog contains multiple parent commands, and any subcommands, an exception will be thrown.
 *
 * Ideally, commands that have subcommands should be separated into their own cogs.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class SubCommand(
    val aliases: Array<String> = [],
    val description: String = "No description available",
    val guildOnly: Boolean = false
)
