package me.devoxin.flight.api

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Cooldown
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.entities.Executable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.instanceParameter

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

    val subcommands = hashMapOf<String, SubCommandFunction>()

    init {
        for (sc in subCmds) {
            val triggers = listOf(sc.name, *sc.properties.aliases)

            for (trigger in triggers) {
                if (subcommands.containsKey(trigger)) {
                    throw IllegalStateException("The sub-command trigger $trigger already exists!")
                }

                subcommands[trigger] = sc
            }
        }
    }

}
