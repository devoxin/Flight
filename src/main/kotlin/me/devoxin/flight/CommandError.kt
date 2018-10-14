package me.devoxin.flight

class CommandError(
        error: Throwable,
        public val command: Command
) : Throwable(error)
