package me.devoxin.flight.api.context

enum class ContextType {
    MESSAGE,
    SLASH,
    // This is not used in Context, but rather within
    // @Command to denote what contexts the command should execute in.
    MESSAGE_OR_SLASH
}
