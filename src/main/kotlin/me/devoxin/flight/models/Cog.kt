package me.devoxin.flight.models

import me.devoxin.flight.CommandError
import me.devoxin.flight.Context

public interface Cog {

    /**
     * Invoked when an error occurs during command execution.
     * This is local to the cog, allowing for per-cog error handling.
     *
     * @return Whether the error was handled or not. If it wasn't,
     *         the error will be passed back to the registered
     *         CommandClientAdapter for handling.
     */
    fun onCommandError(ctx: Context, error: CommandError): Boolean = false

    fun name(): String = this::class.java.simpleName

}