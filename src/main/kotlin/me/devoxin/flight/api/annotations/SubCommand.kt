package me.devoxin.flight.api.annotations

annotation class SubCommand(
    val aliases: Array<String> = [],
    val description: String = "No description available"
)
