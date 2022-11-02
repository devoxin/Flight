package me.devoxin.flight.api.annotations

/**
 * The required range for the argument.
 * Fill in the required type as needed. You may not specify both.
 * Example:
 *   @Range(double = [0.0, 5.0])
 * The first number represents the MINIMUM range. The second represents the MAXIMUM range.
 * If a maximum range is not needed, specify [ number ]
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Range(
    val long: LongArray = [], // e.g. 0, 5
    val double: DoubleArray = [], // e.g. 0.0, 5.0
    val string: IntArray = []
)
