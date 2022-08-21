package me.devoxin.flight.api.context

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.internal.entities.Executable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.components.Modal
import java.util.concurrent.CompletableFuture

class SlashContext(
    override val commandClient: CommandClient,
    val event: SlashCommandInteractionEvent,
    override val invokedCommand: Executable
) : Context {
    override val contextType = ContextType.SLASH
    override val jda: JDA = event.jda
    override val author = event.user
    override val guild = event.guild
    override val member = event.member
    override val messageChannel = event.channel

    var replied = false
        private set
    var deferred = false
        private set

    fun defer(ephemeral: Boolean = false) {
        if (!deferred) { // Idempotency handling
            event.deferReply(ephemeral).queue { deferred = true }
        }
    }

    suspend fun deferAsync(ephemeral: Boolean): InteractionHook {
        if (!deferred) { // Idempotency handling
            return event.deferReply(ephemeral).submit()
                .thenApply { deferred = true; it }
                .await()
        }

        return event.hook
    }

    fun reply(content: String, ephemeral: Boolean = false) {
        respond(MessageBuilder(content).build(), ephemeral)
    }

    fun reply(modal: Modal) {
        event.replyModal(modal).queue { replied = true }
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply], with no
     * special handling to account for acknowledged events.
     */
    fun reply(message: Message, ephemeral: Boolean = false) {
        event.reply(message).setEphemeral(ephemeral).queue { replied = true }
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply], with no
     * special handling to account for acknowledged events.
     */
    suspend fun replyAsync(message: Message, ephemeral: Boolean = false): InteractionHook {
        return event.reply(message).setEphemeral(ephemeral).submit().thenApply { replied = true; it }.await()
    }

    /**
     * This will only call [InteractionHook.sendMessage] with no special handling.
     */
    fun send(message: Message, ephemeral: Boolean = false) {
        event.hook.sendMessage(message).setEphemeral(ephemeral).queue()
    }

    /**
     * This will only call [InteractionHook.sendMessage] with no special handling.
     */
    suspend fun sendAsync(message: Message, ephemeral: Boolean = false): Message {
        return event.hook.sendMessage(message).setEphemeral(ephemeral).submit().await()
    }

    /**
     * Convenience method which handles replying the correct way for you.
     *
     * The [ephemeral] setting is ignored if the interaction is deferred.
     * Instead, the ephemeral setting when deferring is used. This is a Discord limitation.
     */
    fun respond(message: Message, ephemeral: Boolean = false) {
        respond0(message, ephemeral)
    }

    internal fun respond0(message: Message, ephemeral: Boolean = false): CompletableFuture<*> {
        return when {
            replied -> event.hook.sendMessage(message).setEphemeral(ephemeral).submit()
            deferred -> event.hook.editOriginal(message).submit().thenApply { replied = true }
            else -> event.reply(message).setEphemeral(ephemeral).submit().thenApply { replied = true }
        }
    }
}
