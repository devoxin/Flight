package me.devoxin.flight.arguments

import kotlin.reflect.KParameter

class Argument(
    val name: String,
    val type: Class<*>,
    val greedy: Boolean,
    val required: Boolean,
    internal val kparam: KParameter
)
