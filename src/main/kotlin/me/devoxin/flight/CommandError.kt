package me.devoxin.flight

class CommandError(
        val original: Throwable,
        val command: CommandWrapper
)
