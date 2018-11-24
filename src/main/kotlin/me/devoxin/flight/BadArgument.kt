package me.devoxin.flight

import me.devoxin.flight.arguments.ArgType

class BadArgument(
        public val argumentName: String,
        public val type: ArgType,
        public val providedArgument: String
) : Exception("`$argumentName` must be a `$type`")
