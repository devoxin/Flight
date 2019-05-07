package me.devoxin.flight.arguments

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Name(
    val name: String = ""
)
