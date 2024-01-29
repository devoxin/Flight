package me.devoxin.flight.api.annotations

import me.devoxin.flight.api.annotations.choice.DoubleChoice
import me.devoxin.flight.api.annotations.choice.LongChoice
import me.devoxin.flight.api.annotations.choice.StringChoice

/**
 * The choices for the argument.
 * Fill in the required type as needed. All parameters are mutually exclusive,
 * and only one may be specified per argument.
 * Example:
 *   @Choices(string = [StringChoice("Test", "hello world")])
 */
annotation class Choices(
    val long: Array<LongChoice> = [],
    val double: Array<DoubleChoice> = [],
    val string: Array<StringChoice> = []
)
