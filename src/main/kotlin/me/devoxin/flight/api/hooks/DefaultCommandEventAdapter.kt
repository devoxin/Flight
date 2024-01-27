package me.devoxin.flight.api.hooks

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.entities.CheckType
import me.devoxin.flight.api.exceptions.BadArgument
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

open class DefaultCommandEventAdapter : CommandEventAdapter {
    override fun onCheckFailed(ctx: Context, command: CommandFunction, checkType: CheckType) = Unit

    override fun onBadArgument(ctx: Context, command: CommandFunction, error: BadArgument) {
        error.printStackTrace()
    }

    override fun onCommandError(ctx: Context, command: CommandFunction, error: Throwable) {
        error.printStackTrace()
    }

    override fun onCommandPostInvoke(ctx: Context, command: CommandFunction, failed: Boolean) = Unit

    override fun onCommandPreInvoke(ctx: Context, command: CommandFunction) = true

    override fun onParseError(ctx: Context, command: CommandFunction, error: Throwable) {
        error.printStackTrace()
    }

    override fun onInternalError(error: Throwable) {
        error.printStackTrace()
    }

    override fun onCommandCooldown(ctx: Context, command: CommandFunction, cooldown: Long) = Unit

    override fun onAutocompleteError(event: CommandAutoCompleteInteractionEvent, error: Throwable) {
        error.printStackTrace()
    }

    override fun onBotMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) = Unit

    override fun onUserMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) = Unit

    override fun onUnknownCommand(event: MessageReceivedEvent, command: String, args: List<String>) = Unit
}
