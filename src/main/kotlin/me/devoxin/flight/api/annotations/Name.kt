package me.devoxin.flight.api.annotations

/**
 * Sets the name of a command argument.
 * This is redundant as Flight should pick up argument names automatically. This should only
 * be used as a last resort, or if a different name for the argument is needed.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Name(
    val value: String
)
