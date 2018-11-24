package me.devoxin.flight

import me.devoxin.flight.models.CommandClientAdapter
import net.dv8tion.jda.core.Permission

public abstract class DefaultCommandClientAdapter : CommandClientAdapter {

    override fun onBadArgument(ctx: Context, error: BadArgument) {}

    override fun onCommandError(ctx: Context, error: CommandError) {}

    override fun onCommandPostInvoke(ctx: Context, command: CommandWrapper, failed: Boolean) {}

    override fun onCommandPreInvoke(ctx: Context, command: CommandWrapper) = true

    override fun onParseError(ctx: Context, error: Throwable) {}

    override fun onBotMissingPermissions(ctx: Context, command: CommandWrapper, permissions: Array<Permission>) {}

    override fun onUserMissingPermissions(ctx: Context, command: CommandWrapper, permissions: Array<Permission>) {}

}
