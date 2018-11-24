package me.devoxin.flight.arguments

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class Name(
    val name: String = ""
)
