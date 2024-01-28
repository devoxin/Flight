package me.devoxin.flight.internal.entities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.arguments.Argument
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.concurrent.ExecutorService
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

abstract class Executable(
    val name: String,
    val method: KFunction<*>,
    val cog: Cog,
    val arguments: List<Argument>,
    val contextParameter: KParameter
) {
    fun resolveArguments(options: List<OptionMapping>): HashMap<KParameter, Any?> {
        val mapping = hashMapOf<KParameter, Any?>()

        for (argument in arguments) {
            val option = options.firstOrNull { it.name == argument.slashFriendlyName }

            if (option == null) {
                if (argument.isNullable && !argument.optional) {
                    mapping += argument.parameter to null
                    continue
                }

                if (argument.optional) {
                    continue
                }

                throw IllegalStateException("Missing option for argument ${argument.name}")
            }

            mapping += if (option.type == OptionType.INTEGER && (argument.type == Integer::class.java || argument.type == java.lang.Integer::class.java)) {
                val (param, value) = argument.getEntityFromOptionMapping(option)
                @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
                param to (value as java.lang.Long).toInt()
            } else {
                argument.getEntityFromOptionMapping(option)
            }
        }

        return mapping
    }

    open fun execute(ctx: Context, args: HashMap<KParameter, Any?>, complete: (Boolean, Throwable?) -> Unit, executor: ExecutorService?) {
        method.instanceParameter?.let { args[it] = cog }
        args[contextParameter] = ctx

        if (method.isSuspend) {
            DEFAULT_DISPATCHER.launch {
                executeAsync(args, complete)
            }
        } else {
            executor?.execute { executeSync(args, complete) }
                ?: executeSync(args, complete)
        }
    }

    /**
     * Calls the related method with the given args.
     */
    private fun executeSync(args: HashMap<KParameter, Any?>, complete: (Boolean, Throwable?) -> Unit) {
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
    private suspend fun executeAsync(args: HashMap<KParameter, Any?>, complete: (Boolean, Throwable?) -> Unit) {
        try {
            method.callSuspendBy(args)
            complete(true, null)
        } catch (e: Throwable) {
            complete(false, e.cause ?: e)
        }
    }

    companion object {
        private val DEFAULT_DISPATCHER = CoroutineScope(Dispatchers.Default)
    }
}
