package me.devoxin.flight.arguments

import kotlin.reflect.KParameter

class Argument(
    val name: String,
    val type: Class<*>,
    val greedy: Boolean,
    val optional: Boolean, // Denotes that a parameter has a default value.
    val isNullable: Boolean,
    internal val kparam: KParameter
)
