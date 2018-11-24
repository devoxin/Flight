package me.devoxin.flight.arguments

class Argument(
        val name: String,
        val type: Class<*>,
        val greedy: Boolean,
        val required: Boolean
)
