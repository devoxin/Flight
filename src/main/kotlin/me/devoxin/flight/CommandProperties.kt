package me.devoxin.flight

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class CommandProperties(
        val aliases: Array<String> = []
)