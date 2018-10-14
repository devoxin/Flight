package me.devoxin.flight

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

}
