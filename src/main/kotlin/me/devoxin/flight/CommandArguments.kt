package me.devoxin.flight

@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandArguments(vararg val arguments: Argument)
