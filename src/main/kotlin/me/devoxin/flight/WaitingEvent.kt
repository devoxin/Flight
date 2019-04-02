package me.devoxin.flight

import com.mewna.catnip.shard.event.EventType
import java.util.concurrent.CompletableFuture

@Suppress("UNCHECKED_CAST")
class WaitingEvent<T: EventType<T>>(
        private val eventClass: Class<*>,
        private val predicate: (T) -> Boolean,
        private val future: CompletableFuture<T?>
) {

    fun check(event: EventType<T>) = eventClass.isAssignableFrom(event::class.java) && predicate(event as T)

    fun accept(event: EventType<T>?) = future.complete(event as T)

}
