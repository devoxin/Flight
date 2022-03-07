package me.devoxin.flight.api.hooks

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.MessageContext
import me.devoxin.flight.api.exceptions.BadArgument
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

abstract class DefaultCommandEventAdapter : CommandEventAdapter {
    override fun onBadArgument(ctx: MessageContext, command: CommandFunction, error: BadArgument) {
        error.printStackTrace()
    }

    override fun onCommandError(ctx: MessageContext, command: CommandFunction, error: Throwable) {
        error.printStackTrace()
    }

    override fun onCommandPostInvoke(ctx: MessageContext, command: CommandFunction, failed: Boolean) {}

    override fun onCommandPreInvoke(ctx: MessageContext, command: CommandFunction) = true

    override fun onParseError(ctx: MessageContext, command: CommandFunction, error: Throwable) {
        error.printStackTrace()
    }

    override fun onInternalError(error: Throwable) {
        error.printStackTrace()
    }

    override fun onCommandCooldown(ctx: MessageContext, command: CommandFunction, cooldown: Long) {}

    override fun onBotMissingPermissions(ctx: MessageContext, command: CommandFunction, permissions: List<Permission>) {}

    override fun onUserMissingPermissions(ctx: MessageContext, command: CommandFunction, permissions: List<Permission>) {}

    override fun onUnknownCommand(event: MessageReceivedEvent, command: String, args: List<String>) {
    }
}
