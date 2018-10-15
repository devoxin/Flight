package me.devoxin.flight

import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.async

abstract class AsyncCommand : Command {

    abstract suspend fun executeAsync(ctx: Context, args: Map<String, Any?>)

    final override fun execute(ctx: Context, args: Map<String, Any?>) {
        GlobalScope.async {
            try {
                executeAsync(ctx, args)
            } catch (e: Throwable) {
                val commandError = CommandError(e, this@AsyncCommand)
                ctx.commandClient.eventListeners.forEach { it.onCommandError(ctx, commandError) }
            }
        }
    }

}