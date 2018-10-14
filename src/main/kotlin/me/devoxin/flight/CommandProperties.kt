package me.devoxin.flight

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandProperties(
        val aliases: Array<String> = [],
        val description: String = "No description available",
        val developerOnly: Boolean = false
)
