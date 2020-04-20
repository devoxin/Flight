package me.devoxin.flight.api.annotations

import me.devoxin.flight.api.entities.BucketType
import java.util.concurrent.TimeUnit

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Cooldown(
    val duration: Long,
    val timeUnit: TimeUnit = TimeUnit.MILLISECONDS,
    val bucket: BucketType
)
