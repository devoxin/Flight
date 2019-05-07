package me.devoxin.flight

class CommandError(
        error: Throwable,
        val command: CommandWrapper
) : Throwable(error)
