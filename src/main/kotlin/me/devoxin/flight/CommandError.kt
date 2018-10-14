package me.devoxin.flight

class CommandError(
        public val error: Throwable,
        public val command: Command,
        public val ctx: Context,
        public val message: String
)
