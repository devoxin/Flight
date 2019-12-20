package me.devoxin.flight.models

import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.api.Context

interface Cog {

    /**
     * Invoked when an error occurs during command execution.
     * This is local to the cog, allowing for per-cog error handling.
     *
     * @return Whether the error was handled or not. If it wasn't,
     *         the error will be passed back to the registered
     *         CommandClientAdapter for handling.
     */
    fun onCommandError(ctx: Context, command: CommandWrapper, error: Throwable): Boolean = false

    /**
     * Invoked before a command is executed. This check is local to
     * all commands inside the cog.
     *
     * @return Whether the command execution should continue or not.
     */
    fun localCheck(ctx: Context, command: CommandWrapper): Boolean = true

    /**
     * Used to determine the cog's name.
     *
     * @return The cog's name
     */
    fun name(): String = this::class.java.simpleName

}