package me.devoxin.flight

import me.devoxin.flight.arguments.Argument

class BadArgument(
        val argument: Argument,
        val providedArgument: String
) : Exception("`${argument.name}` must be a `${argument.type.simpleName}`")
