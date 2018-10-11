package me.devoxin.flight

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE)
annotation class CommandArgument(
        val name: String,
        val type: Arguments.ArgType,
        val greedy: Boolean = false,
        val required: Boolean = true
)
