package me.devoxin.flight.api

import me.devoxin.flight.annotations.Command
import me.devoxin.flight.arguments.Argument
import me.devoxin.flight.models.Cog
import java.lang.reflect.Method
import kotlin.coroutines.suspendCoroutine
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy

class CommandWrapper(
        val name: String,
        val arguments: List<Argument>,
        val category: String,
        val properties: Command,
        val async: Boolean,
        val method: KFunction<*>,
        val cog: Cog,
        internal val contextParameter: KParameter
) {

    /**
     * Calls the related method with the given args.
     */
    fun execute(ctx: Context, args: HashMap<KParameter, Any?>, complete: (Boolean, Throwable?) -> Unit) {
        args[contextParameter] = ctx

        try {
            //method.invoke(cog, ctx, *additionalArgs)
            method.callBy(args)
            complete(true, null)
        } catch (e: Throwable) {
            complete(false, e.cause ?: e)
        }
    }

    /**
     * Calls the related method with the given args, except in an async manner.
     */
    suspend fun executeAsync(ctx: Context, args: HashMap<KParameter, Any?>, complete: (Boolean, Throwable?) -> Unit) {
        args[contextParameter] = ctx
        //suspendCoroutine<Unit> {
            try {
                //method.invoke(cog, ctx, *additionalArgs, it)
                method.callSuspendBy(args)
                complete(true, null)
            } catch (e: Throwable) {
                complete(false, e.cause ?: e)
            }
        //}
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