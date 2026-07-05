package me.devoxin.flight.api.context

import kotlinx.coroutines.future.await
import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.api.entities.DSLMessageCreateBuilder
import me.devoxin.flight.internal.entities.Executable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.modals.Modal
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import net.dv8tion.jda.api.utils.messages.MessageEditData
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
    override val guildChannel = event.takeIf { it.isFromGuild }?.guildChannel
    override val isFromGuild = event.isFromGuild

    var replied = false
        private set
    var deferred = false
        private set

    fun defer(ephemeral: Boolean = false) {
        defer0(ephemeral)
    }

    suspend fun deferAsync(ephemeral: Boolean = false): InteractionHook {
        return defer0(ephemeral).await()
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply] with no special handling.
     * Use [respond] or [respondAsync] to handle things such as deferral or already acknowledged events.
     */
    fun reply(content: String, ephemeral: Boolean = false) {
        event.reply(content).setEphemeral(ephemeral).queue { replied = true }
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply] with no special handling.
     * Use [respond] or [respondAsync] to handle things such as deferral or already acknowledged events.
     */
    fun reply(modal: Modal) {
        if (replied) {
            throw IllegalStateException("Cannot respond with a Modal to an acknowledged interaction!")
        }

        event.replyModal(modal).queue { replied = true }
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply] with no special handling.
     * Use [respond] or [respondAsync] to handle things such as deferral or already acknowledged events.
     */
    fun reply(message: MessageCreateData, ephemeral: Boolean = false) {
        event.reply(message).setEphemeral(ephemeral).queue { replied = true }
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply] with no special handling.
     * Use [respond] or [respondAsync] to handle things such as deferral or already acknowledged events.
     */
    suspend fun replyAsync(content: String, ephemeral: Boolean = false): InteractionHook {
        return event.reply(content).setEphemeral(ephemeral).submit().thenApply { replied = true; it }.await()
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply] with no special handling.
     * Use [respond] or [respondAsync] to handle things such as deferral or already acknowledged events.
     */
    suspend fun replyAsync(modal: Modal) {
        if (replied) {
            throw IllegalStateException("Cannot respond with a Modal to an acknowledged interaction!")
        }

        event.replyModal(modal).submit().thenAccept { replied = true }.await()
    }

    /**
     * This will only call [SlashCommandInteractionEvent.reply] with no special handling.
     * Use [respond] or [respondAsync] to handle things such as deferral or already acknowledged events.
     */
    suspend fun replyAsync(message: MessageCreateData, ephemeral: Boolean = false): InteractionHook {
        return event.reply(message).setEphemeral(ephemeral).submit().thenApply { replied = true; it }.await()
    }

    /**
     * This will only call [InteractionHook.sendMessage] with no special handling.
     */
    fun send(message: MessageCreateData, ephemeral: Boolean = false) {
        event.hook.sendMessage(message).setEphemeral(ephemeral).queue()
    }

    /**
     * This will only call [InteractionHook.sendMessage] with no special handling.
     */
    suspend fun sendAsync(message: MessageCreateData, ephemeral: Boolean = false): Message {
        return event.hook.sendMessage(message).setEphemeral(ephemeral).submit().await()
    }

    /**
     * Convenience method which handles replying the correct way for you.
     *
     * The [ephemeral] setting is ignored if the interaction is deferred.
     * Instead, the ephemeral setting when deferring is used. This is a Discord limitation.
     */
    fun respond(message: MessageCreateData, ephemeral: Boolean = false) {
        respond0(message, ephemeral)
    }

    /**
     * Convenience method which handles replying the correct way for you.
     *
     * The [ephemeral] setting is ignored if the interaction is deferred.
     * Instead, the ephemeral setting when deferring is used. This is a Discord limitation.
     */
    fun respond(builder: DSLMessageCreateBuilder.() -> Unit, ephemeral: Boolean = false) {
        val built = DSLMessageCreateBuilder().apply(builder).build()
        respond0(built, ephemeral)
    }

    /**
     * Convenience method which handles replying the correct way for you.
     *
     * The [ephemeral] setting is ignored if the interaction is deferred.
     * Instead, the ephemeral setting when deferring is used. This is a Discord limitation.
     */
    suspend fun respondAsync(message: MessageCreateData, ephemeral: Boolean = false) {
        respond0(message, ephemeral).await()
    }

    /**
     * Convenience method which handles replying the correct way for you.
     *
     * The [ephemeral] setting is ignored if the interaction is deferred.
     * Instead, the ephemeral setting when deferring is used. This is a Discord limitation.
     */
    suspend fun respondAsync(builder: DSLMessageCreateBuilder.() -> Unit, ephemeral: Boolean = false) {
        val built = DSLMessageCreateBuilder().apply(builder).build()
        respond0(built, ephemeral).await()
    }

    internal fun defer0(ephemeral: Boolean): CompletableFuture<InteractionHook> {
        if (!deferred) { // Idempotency handling
            return event.deferReply(ephemeral).submit()
                .thenApply { deferred = true; it }
        }

        return CompletableFuture.completedFuture(event.hook)
    }

    internal fun respond0(message: MessageCreateData, ephemeral: Boolean = false): CompletableFuture<*> {
        return when {
            replied -> event.hook.sendMessage(message).setEphemeral(ephemeral).submit()
            deferred -> event.hook.editOriginal(MessageEditData.fromCreateData(message)).submit().thenApply { replied = true; it }
            else -> event.reply(message).setEphemeral(ephemeral).submit().thenApply { replied = true; it }
        }
    }
}
