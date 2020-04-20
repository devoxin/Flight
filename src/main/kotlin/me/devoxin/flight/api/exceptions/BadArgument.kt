package me.devoxin.flight.api.exceptions

import me.devoxin.flight.internal.arguments.Argument

class BadArgument(
    val argument: Argument,
    val providedArgument: String,
    val original: Throwable? = null
) : Throwable("`${argument.name}` must be a `${argument.type.simpleName}`")
