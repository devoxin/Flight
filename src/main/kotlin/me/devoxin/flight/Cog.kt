package me.devoxin.flight

public interface Cog {

    /**
     * Invoked when an error occurs during command execution.
     * This is local to this command, allowing for per-command error handling.
     *
     * @return Whether the error was handled or not. If it wasn't,
     *         the error will be passed back to the registered
     *         CommandClientAdapter for handling.
     */
    fun onCommandError(ctx: Context, error: CommandError): Boolean = false

}