package me.devoxin.flight.api.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Autocomplete(
    /**
     * AUTOCOMPLETE HANDLERS MUST HAVE THE PARAMETER:
     * event: CommandAutoCompleteInteractionEvent
     */

    // The name of the method (in the same cog) that will handle autocomplete requests
    // for this parameter.
    val method: String
)
