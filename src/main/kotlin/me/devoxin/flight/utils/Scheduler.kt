package me.devoxin.flight.utils

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object Scheduler {
    private val schd = Executors.newSingleThreadScheduledExecutor()

    fun every(milliseconds: Long, block: () -> Unit): ScheduledFuture<*> {
        return schd.scheduleAtFixedRate(block, milliseconds, milliseconds, TimeUnit.MILLISECONDS)
    }
}