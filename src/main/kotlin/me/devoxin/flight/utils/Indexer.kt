package me.devoxin.flight.utils

import me.devoxin.flight.CommandWrapper
import me.devoxin.flight.Context
import me.devoxin.flight.annotations.Async
import me.devoxin.flight.annotations.Command
import me.devoxin.flight.arguments.Argument
import me.devoxin.flight.arguments.Greedy
import me.devoxin.flight.arguments.Name
import me.devoxin.flight.arguments.Optional
import me.devoxin.flight.models.Cog
import org.reflections.Reflections
import org.reflections.scanners.MethodParameterNamesScanner
import org.reflections.scanners.SubTypesScanner
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.coroutines.Continuation

class Indexer(private val pkg: String) {

    private val reflections = Reflections(pkg, MethodParameterNamesScanner(), SubTypesScanner())

    public fun getCogs(): List<Class<out Cog>> {
        logger.debug("Scanning $pkg for cogs...")
        val cogs = reflections.getSubTypesOf(Cog::class.java)
        logger.debug("Found ${cogs.size} cogs")

        return cogs
                .filter { !Modifier.isAbstract(it.modifiers) && !it.isInterface && Cog::class.java.isAssignableFrom(it) }
                .toList()
    }

    public fun getCommands(cog: Cog): List<Method> {
        logger.debug("Scanning ${cog.name()} for commands...")
        val commands = cog::class.java.methods.filter { it.isAnnotationPresent(Command::class.java) }
        logger.debug("Found ${commands.size} commands in cog ${cog.name()}")

        return commands.toList()
    }

    public fun loadCommand(meth: Method, cog: Cog): CommandWrapper {
        if (meth.declaringClass != cog::class.java) {
            throw IllegalArgumentException("${meth.name} is not from ${cog.name()}")
        }

        if (!meth.isAnnotationPresent(Command::class.java)) {
            throw IllegalArgumentException("${meth.name} is not annotated with Command!")
        }

        val category = cog.name()
        val name = meth.name
        val properties = meth.getAnnotation(Command::class.java)
        val async = meth.isAnnotationPresent(Async::class.java)

        val allParamNames = getParamNames(meth)
        val paramNames = allParamNames.drop(allParamNames.indexOf("this") + 1)

        if (paramNames.size != meth.parameters.size) {
            throw IllegalArgumentException(
                    "Parameter count mismatch in command ${meth.name}, expected: ${meth.parameters.size}, got: ${paramNames.size}\n" +
                            "${paramNames.joinToString(", ")}\n" +
                            "${meth.parameters.map { it.type.simpleName }}"
            )
        }

        val arguments = mutableListOf<Argument>()

        for (a in meth.parameters.withIndex()) {
            val i = a.index
            val p = a.value

            if (p.type == Context::class.java || p.type == Continuation::class.java) {
                continue
            }

            val pName = paramNames[i] // p.getAnnotation(Name::class.java)?.name
            val type = p.type
            val greedy = p.isAnnotationPresent(Greedy::class.java)
            val required = !p.isAnnotationPresent(Optional::class.java)

            arguments.add(Argument(pName, type, greedy, required))
        }

        return CommandWrapper(name, arguments.toList(), category, properties, async, meth, cog)
    }

    public fun getParamNames(meth: Method): List<String> {
        return reflections.getMethodParamNames(meth)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Indexer::class.java)
    }

}
