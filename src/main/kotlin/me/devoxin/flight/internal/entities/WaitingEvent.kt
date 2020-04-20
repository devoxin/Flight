package me.devoxin.flight.internal.entities

import net.dv8tion.jda.api.events.GenericEvent
import java.util.concurrent.CompletableFuture

@Suppress("UNCHECKED_CAST")
class WaitingEvent<T : GenericEvent>(
        private val eventClass: Class<*>,
        private val predicate: (T) -> Boolean,
        private val future: CompletableFuture<T>
) {

    fun check(event: GenericEvent) = eventClass.isAssignableFrom(event::class.java) && predicate(event as T)

    fun accept(event: GenericEvent) = future.complete(event as T)

}
