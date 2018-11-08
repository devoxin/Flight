package me.devoxin.flight

import net.dv8tion.jda.core.Permission

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandProperties(
        val aliases: Array<String> = [],
        val description: String = "No description available",
        val category: String = "No Category",
        val developerOnly: Boolean = false,
        val userPermissions: Array<Permission> = [],
        val botPermissions: Array<Permission> = [],
        val nsfw: Boolean = false,
        val guildOnly: Boolean = false
)
