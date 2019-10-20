package me.devoxin.flight.api

import me.devoxin.flight.internal.CommandWrapper

class CommandError(
        val original: Throwable,
        val command: CommandWrapper
)
