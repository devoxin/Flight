package me.devoxin.flight.api.annotations

import net.dv8tion.jda.api.Permission

/**
 * Marks a function as a command. This should be used to annotate methods within a cog
 * as commands, so that the scanner can detect them, and register them.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(
    val argDelimiter: Char = ' ',
    val aliases: Array<String> = [],
    val description: String = "No description available",
    val developerOnly: Boolean = false,
    val userPermissions: Array<Permission> = [],
    val botPermissions: Array<Permission> = [],
    val nsfw: Boolean = false,
    val guildOnly: Boolean = false,
    val hidden: Boolean = false
)
