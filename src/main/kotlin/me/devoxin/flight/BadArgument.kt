package me.devoxin.flight

class BadArgument(
        public val argumentName: String,
        public val type: ArgType,
        public val providedArgument: String
) : Exception("`$argumentName` must be a `$type`")
