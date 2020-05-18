package me.devoxin.flight.api

import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Executable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class SubCommandFunction(
    name: String,
    val properties: SubCommand,
    // Executable properties
    method: KFunction<*>,
    cog: Cog,
    arguments: List<Argument>,
    contextParameter: KParameter
) : Executable(name, method, cog, arguments, contextParameter)
