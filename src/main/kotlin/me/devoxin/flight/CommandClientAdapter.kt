package me.devoxin.flight

public interface CommandClientAdapter {

    /**
     * Invoked when an invalid argument is passed.
     */
    public fun onBadArgument(error: BadArgument)

    /**
     * Invoked when the parser encounters an internal error.
     */
    public fun onParseError(error: Throwable)

    /**
     * Invoked before a command is executed. Useful for logging command usage etc.
     *
     * @return True, if the command should still be executed
     */
    public fun onCommandPreInvoke(command: Command, ctx: Context): Boolean

    /**
     * Invoked after a command has executed, regardless of whether the command execution encountered an error
     */
    public fun onCommandPostInvoke(command: Command, ctx: Context)

    /**
     * Invoked when a command encounters an error during execution.
     */
    public fun onCommandError(error: CommandError)

}
