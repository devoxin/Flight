package me.devoxin.flight.api.hooks

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.exceptions.BadArgument
import net.dv8tion.jda.api.Permission

abstract class DefaultCommandEventAdapter : CommandEventAdapter {

    override fun onBadArgument(ctx: Context, command: CommandFunction, error: BadArgument) {}

    override fun onCommandError(ctx: Context, command: CommandFunction, error: Throwable) {}

    override fun onCommandPostInvoke(ctx: Context, command: CommandFunction, failed: Boolean) {}

    override fun onCommandPreInvoke(ctx: Context, command: CommandFunction) = true

    override fun onParseError(ctx: Context, command: CommandFunction, error: Throwable) {}

    override fun onBotMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) {}

    override fun onUserMissingPermissions(ctx: Context, command: CommandFunction, permissions: List<Permission>) {}

}
