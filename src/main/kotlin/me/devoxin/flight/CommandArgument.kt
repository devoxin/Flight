package me.devoxin.flight

@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class CommandArgument(
        val name: String,
        val type: Arguments.ArgType,
        val greedy: Boolean = false,
        val required: Boolean = true
)
