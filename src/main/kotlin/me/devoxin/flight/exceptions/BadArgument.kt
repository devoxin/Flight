package me.devoxin.flight.exceptions

import me.devoxin.flight.arguments.Argument

class BadArgument(
        val argument: Argument,
        val providedArgument: String,
        val original: Throwable? = null
) : Throwable("`${argument.name}` must be a `${argument.type.simpleName}`")
