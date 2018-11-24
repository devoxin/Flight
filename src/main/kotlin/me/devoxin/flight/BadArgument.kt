package me.devoxin.flight

import me.devoxin.flight.arguments.Argument

class BadArgument(
        public val argument: Argument,
        public val providedArgument: String
) : Exception("`${argument.name}` must be a `${argument.type.simpleName}`")
