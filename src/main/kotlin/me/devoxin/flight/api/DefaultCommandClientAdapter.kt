package me.devoxin.flight.api

import me.devoxin.flight.exceptions.BadArgument
import me.devoxin.flight.models.CommandClientAdapter
import net.dv8tion.jda.api.Permission

abstract class DefaultCommandClientAdapter<C> : CommandClientAdapter<C> {

    override fun onBadArgument(ctx: Context<C>, error: BadArgument) {}

    override fun onCommandError(ctx: Context<C>, error: CommandError) {}

    override fun onCommandPostInvoke(ctx: Context<C>, command: CommandWrapper, failed: Boolean) {}

    override fun onCommandPreInvoke(ctx: Context<C>, command: CommandWrapper) = true

    override fun onParseError(ctx: Context<C>, error: Throwable) {}

    // @todo: Get rid of specific types
    override fun onBotMissingPermissions(ctx: Context<C>, command: CommandWrapper, permissions: List<Permission>) {}

    // @todo: Get rid of specific types
    override fun onUserMissingPermissions(ctx: Context<C>, command: CommandWrapper, permissions: List<Permission>) {}

}
