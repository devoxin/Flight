package me.devoxin.flight

@Repeatable
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class Argument(
        val name: String,
        val type: ArgType,
        val greedy: Boolean = false,
        val required: Boolean = true
)
