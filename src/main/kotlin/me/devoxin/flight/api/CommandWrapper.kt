package me.devoxin.flight.api

import me.devoxin.flight.annotations.Command
import me.devoxin.flight.api.CommandError
import me.devoxin.flight.api.Context
import me.devoxin.flight.arguments.Argument
import me.devoxin.flight.models.Cog
import java.lang.reflect.Method
import kotlin.coroutines.suspendCoroutine

class CommandWrapper(
        val name: String,
        val arguments: List<Argument>,
        val category: String,
        val properties: Command,
        val async: Boolean,
        val method: Method,
        val cog: Cog
) {

    /**
     * Calls the related method with the given args.
     */
    fun execute(ctx: Context, vararg additionalArgs: Any?, complete: (Boolean, CommandError?) -> Unit) {
        try {
            method.invoke(cog, ctx, *additionalArgs)
            complete(true, null)
        } catch (e: Throwable) {
            complete(false, CommandError(e.cause ?: e, this))
        }
    }

    /**
     * Calls the related method with the given args, except in an async manner.
     */
    suspend fun executeAsync(ctx: Context, vararg additionalArgs: Any?, complete: (Boolean, CommandError?) -> Unit) {
        suspendCoroutine<Unit> {
            try {
                method.invoke(cog, ctx, *additionalArgs, it)
                complete(true, null)
            } catch (e: Throwable) {
                complete(false, CommandError(e.cause ?: e, this))
            }
        }
    }

    /**
     * The arguments for this command, if specified.
     *
     * @return A list of command arguments.
     */
//    fun commandArguments(): List<Argument> {
//        val arguments = mutableListOf<Argument>()
//
//        for (p in method.parameters) {
//            if (p.type == Context::class.java || p.type == Continuation::class.java) {
//                continue
//            }
//
//            val name = p.getAnnotation(Name::class.java)?.name ?: p.name
//            val type = p.type
//            val greedy = p.isAnnotationPresent(Greedy::class.java)
//            val required = !p.isAnnotationPresent(Optional::class.java)
//
//            arguments.add(Argument(name, type, greedy, required))
//        }
//
//        return arguments
//    }

}