package me.devoxin.flight.arguments

import kotlin.reflect.KParameter

class Argument(
    val name: String,
    val type: Class<*>,
    val greedy: Boolean,
    val optional: Boolean, // Denotes that a parameter has a default value.
    val valueRequired: Boolean, // Denotes whether it's marked nullable, thus always requires a value.
    internal val kparam: KParameter
)
