package me.devoxin.flight.api

import kotlinx.coroutines.CoroutineScope
import me.devoxin.flight.models.Attachment
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.Event

interface Context<C> {
    val client: C

    val commandClient: CommandClient
    val trigger: String

    // @todo: Get rid of specific types
    fun send(content: String, callback: ((Message) -> Unit)? = null)
    fun upload(attachment: Attachment)
    // @todo: Get rid of specific types
    fun embed(block: EmbedBuilder.() -> Unit)
    fun dm(content: String)

    suspend fun sendAsync(content: String): Message
    // @todo: Get rid of specific types
    suspend fun embedAsync(block: EmbedBuilder.() -> Unit): Message

    /**
     * Sends a typing status within the channel until the provided function is exited.
     *
     * @param block
     *        The code that should be executed while the typing status is active.
     */
    fun typing(block: () -> Unit)

    /**
     * Sends a typing status within the channel until the provided function is exited.
     *
     * @param block
     *        The code that should be executed while the typing status is active.
     */
    fun typingAsync(block: suspend CoroutineScope.() -> Unit)

    /**
     * Waits for the given event. Only events that pass the given predicate will be returned.
     * If the timeout is exceeded with no results then null will be returned.
     *
     * @param predicate
     *        A function that must return a boolean denoting whether the event meets the given criteria.
     *
     * @param timeout
     *        How long to wait, in milliseconds, for the given event type before expiring.
     */
    // @todo: Move this somewhere else
    suspend fun <T : Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): T?

    // TODO: Method to clean a string.
}
