package me.devoxin.flight.models

import me.devoxin.flight.BadArgument
import me.devoxin.flight.CommandError
import me.devoxin.flight.CommandWrapper
import me.devoxin.flight.Context
import net.dv8tion.jda.api.Permission

interface CommandClientAdapter {

    /**
     * Invoked when an invalid argument is passed.
     */
    fun onBadArgument(ctx: Context, error: BadArgument)

    /**
     * Invoked when the parser encounters an internal error.
     */
    fun onParseError(ctx: Context, error: Throwable)

    /**
     * Invoked before a command is executed. Useful for logging command usage etc.
     *
     * @return True, if the command should still be executed
     */
    fun onCommandPreInvoke(ctx: Context, command: CommandWrapper): Boolean

    /**
     * Invoked after a command has executed, regardless of whether the command execution encountered an error
     *
     * @param ctx The command context.
     * @param command The command that finished processing.
     * @param failed Whether the command encountered an error or not.
     */
    fun onCommandPostInvoke(ctx: Context, command: CommandWrapper, failed: Boolean)

    /**
     * Invoked when a command encounters an error during execution.
     */
    fun onCommandError(ctx: Context, error: CommandError)

    /**
     * Invoked when a user lacks permissions to execute a command
     */
    fun onUserMissingPermissions(ctx: Context, command: CommandWrapper, permissions: Array<Permission>)

    /**
     * Invoked when the bot lacks permissions to execute a command
     */
    fun onBotMissingPermissions(ctx: Context, command: CommandWrapper, permissions: Array<Permission>)

}
