package me.devoxin.flight.api

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Cooldown
import me.devoxin.flight.api.context.ContextType
import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.context.SlashContext
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.entities.Executable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*

class CommandFunction(
    name: String,
    val category: String,
    val properties: Command,
    val cooldown: Cooldown?,
    val jar: Jar?,

    subCmds: List<SubCommandFunction>,
    // Executable properties
    method: KFunction<*>,
    cog: Cog,
    arguments: List<Argument>,
    contextParameter: KParameter
) : Executable(name, method, cog, arguments, contextParameter) {
    val contextType: ContextType
    val subcommands = hashMapOf<String, SubCommandFunction>()

    @Deprecated("Use #subcommands with a mapping/filter function.")
    val subcommandAliases: Map<String, SubCommandFunction>
        get() = subcommands.values.flatMap { it.properties.aliases.map { a -> a to it } }
            .associateBy({ it.first }) { it.second }

    init {
        val jvmCtx = contextParameter.type

        contextType = when {
            jvmCtx.isSubtypeOf(SlashContext::class.starProjectedType) -> ContextType.SLASH
            jvmCtx.isSubtypeOf(MessageContext::class.starProjectedType) -> ContextType.MESSAGE
            else -> ContextType.MESSAGE_OR_SLASH
        }

        for (sc in subCmds) {
            subcommands[sc.name] = sc

            for (trigger in sc.properties.aliases) {
                val existing = subcommands[trigger]

                if (existing != null) {
                    throw IllegalStateException("The trigger '$trigger' for sub-command '${sc.name}' within command '$name' is already assigned to '${existing.name}'!")
                }

                subcommands[trigger] = sc
            }
        }
    }
}
