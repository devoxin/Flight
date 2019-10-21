package me.devoxin.flight.api

class CommandError(
        val original: Throwable,
        val command: CommandWrapper
)
