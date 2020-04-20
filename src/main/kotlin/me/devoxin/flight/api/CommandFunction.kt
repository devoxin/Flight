package me.devoxin.flight.api

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Cooldown
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.api.entities.Cog
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter

class CommandFunction(
    val name: String,
    val arguments: List<Argument>,
    val category: String,
    val properties: Command,
    val cooldown: Cooldown?,
    val async: Boolean,
    val method: KFunction<*>,
    val cog: Cog,
    val jar: Jar?,
    private val contextParameter: KParameter
) {

    /**
     * Calls the related method with the given args.
     */
    fun execute(ctx: Context, args: HashMap<KParameter, Any?>, complete: (Boolean, Throwable?) -> Unit) {
        method.instanceParameter?.let { args[it] = cog }
        args[contextParameter] = ctx

        try {
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
        method.instanceParameter?.let { args[it] = cog }
        args[contextParameter] = ctx

        try {
            method.callSuspendBy(args)
            complete(true, null)
        } catch (e: Throwable) {
            complete(false, e.cause ?: e)
        }
    }



}