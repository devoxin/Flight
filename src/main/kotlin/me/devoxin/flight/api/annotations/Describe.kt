package me.devoxin.flight.api.annotations

/**
 * Describes an argument for a command.
 * This is only used by [me.devoxin.flight.internal.entities.CommandRegistry.toDiscordCommands].
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Describe(
    val value: String = ""
)
