package me.devoxin.flight

import net.dv8tion.jda.core.Permission

public interface CommandClientAdapter {

    /**
     * Invoked when an invalid argument is passed.
     */
    public fun onBadArgument(ctx: Context, error: BadArgument)

    /**
     * Invoked when the parser encounters an internal error.
     */
    public fun onParseError(ctx: Context, error: Throwable)

    /**
     * Invoked before a command is executed. Useful for logging command usage etc.
     *
     * @return True, if the command should still be executed
     */
    public fun onCommandPreInvoke(ctx: Context, command: Command): Boolean

    /**
     * Invoked after a command has executed, regardless of whether the command execution encountered an error
     */
    public fun onCommandPostInvoke(ctx: Context, command: Command)

    /**
     * Invoked when a command encounters an error during execution.
     */
    public fun onCommandError(ctx: Context, error: CommandError)

    /**
     * Invoked when a user lacks permissions to execute a command
     */
    public fun onUserMissingPermissions(ctx: Context, command: Command, permissions: Array<Permission>)

    /**
     * Invoked when the bot lacks permissions to execute a command
     */
    public fun onBotMissingPermissions(ctx: Context, command: Command, permissions: Array<Permission>)

}
