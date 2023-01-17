package me.devoxin.flight.api.entities

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.context.MessageContext

interface Cog {

    fun name(): String? = null

    /**
     * Invoked when an error occurs during command execution.
     * This is local to the cog, allowing for per-cog error handling.
     *
     * @return Whether the error was handled or not. If it wasn't,
     *         the error will be passed back to the registered
     *         CommandClientAdapter for handling.
     */
    fun onCommandError(ctx: Context, command: CommandFunction, error: Throwable): Boolean = false

    /**
     * Invoked before a command is executed. This check is local to
     * all commands inside the cog.
     *
     * @return Whether the command execution should continue or not.
     */
    fun localCheck(ctx: Context, command: CommandFunction): Boolean = true


    /**
     * Invoked when this Cog gets unloaded, usually through [CommandRegistry.unload].
     * This can be used as a last-ditch attempt to clean up, or shut down any resources.
     */
    fun unload(): Unit = Unit

}
