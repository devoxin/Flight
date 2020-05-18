package me.devoxin.flight.api.annotations

import me.devoxin.flight.api.entities.BucketType
import java.util.concurrent.TimeUnit

/**
 * Sets a cooldown on the command.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Cooldown(
    /** How long the cool-down lasts. */
    val duration: Long,
    /** The time unit of the duration. */
    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    /** The bucket this cool-down applies to. */
    val bucket: BucketType
)
