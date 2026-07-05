package me.devoxin.flight.internal.utils

object ExceptionUtils {
    fun suppressed(block: () -> Unit) {
        runCatching { block() }
    }
}
