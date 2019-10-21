package me.devoxin.flight.api

import me.devoxin.flight.exceptions.BadArgument
import me.devoxin.flight.models.CommandClientAdapter
import net.dv8tion.jda.api.Permission

abstract class DefaultCommandClientAdapter : CommandClientAdapter {

    override fun onBadArgument(ctx: Context, error: BadArgument) {}

    override fun onCommandError(ctx: Context, error: CommandError) {}

    override fun onCommandPostInvoke(ctx: Context, command: CommandWrapper, failed: Boolean) {}

    override fun onCommandPreInvoke(ctx: Context, command: CommandWrapper) = true

    override fun onParseError(ctx: Context, error: Throwable) {}

    override fun onBotMissingPermissions(ctx: Context, command: CommandWrapper, permissions: List<Permission>) {}

    override fun onUserMissingPermissions(ctx: Context, command: CommandWrapper, permissions: List<Permission>) {}

}