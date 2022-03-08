package me.devoxin.flight.api

interface Context {
    enum class ContextType {
        MESSAGE,
        SLASH
    }

    val contextType: ContextType
}
