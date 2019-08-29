package me.devoxin.flight

import me.devoxin.flight.annotations.Command
import me.devoxin.flight.arguments.Argument
import me.devoxin.flight.arguments.Greedy
import me.devoxin.flight.arguments.Name
import me.devoxin.flight.arguments.Optional
import me.devoxin.flight.models.Cog
import java.lang.reflect.Method
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction

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
            complete(false, CommandError(e, this))
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
                //The elvis operator is necessary here as it returns the original error as thrown by the command itself.
                // Without it, CommandError#original would return an InvocationTargetException, as part of reflections
                // which is NOT what we want. In the event that somehow, cause is not available, it will fall back to the
                // error as it was thrown.
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