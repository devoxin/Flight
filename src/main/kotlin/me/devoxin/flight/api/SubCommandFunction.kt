package me.devoxin.flight.api

import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Executable
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

class SubCommandFunction(
    val name: String,
    val properties: SubCommand,
    // Executable properties
    method: KFunction<*>,
    cog: Cog,
    contextParameter: KParameter,
    arguments: List<Argument>
) : Executable(method, cog, contextParameter, arguments)
