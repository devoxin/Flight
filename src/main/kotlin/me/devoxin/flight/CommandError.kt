package me.devoxin.flight

class CommandError(
        val error: Throwable,
        val command: CommandWrapper
) : Throwable(error)
