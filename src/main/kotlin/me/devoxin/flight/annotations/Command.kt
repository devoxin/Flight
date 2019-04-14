package me.devoxin.flight.annotations

import com.mewna.catnip.entity.util.Permission

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(
        val aliases: Array<String> = [],
        val description: String = "No description available",
        val developerOnly: Boolean = false,
        val userPermissions: Array<Permission> = [],
        val botPermissions: Array<Permission> = [],
        val nsfw: Boolean = false,
        val guildOnly: Boolean = false
)
