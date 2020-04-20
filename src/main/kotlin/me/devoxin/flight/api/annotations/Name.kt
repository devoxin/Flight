package me.devoxin.flight.api.annotations

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Name(
    val name: String = ""
)
