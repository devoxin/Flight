package me.devoxin.flight.internal.utils

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.SubCommandFunction
import me.devoxin.flight.api.annotations.*
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.api.entities.Cog
import org.reflections.Reflections
import org.reflections.scanners.MethodParameterNamesScanner
import org.reflections.scanners.Scanners
import org.reflections.scanners.SubTypesScanner
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmErasure

class Indexer {

    private val jar: Jar?
    private val packageName: String
    private val reflections: Reflections
    private val classLoader: URLClassLoader?

    constructor(packageName: String) {
        this.packageName = packageName
        this.classLoader = null
        this.jar = null
        reflections = Reflections(packageName, MethodParameterNamesScanner(), SubTypesScanner())
    }

    constructor(packageName: String, jarPath: String) {
        this.packageName = packageName

        val commandJar = File(jarPath)
        check(commandJar.exists()) { "jarPath points to a non-existent file." }
        check(commandJar.extension == "jar") { "jarPath leads to a file which is not a jar." }

        val path = URL("jar:file:${commandJar.absolutePath}!/")
        this.classLoader = URLClassLoader.newInstance(arrayOf(path))
        this.jar = Jar(commandJar.nameWithoutExtension, commandJar.absolutePath, packageName, classLoader)
        reflections = Reflections(packageName, this.classLoader, MethodParameterNamesScanner(), Scanners.SubTypes)
    }

    fun getCogs(): List<Cog> {
        val cogs = reflections.getSubTypesOf(Cog::class.java)
        log.debug("Discovered ${cogs.size} cogs in $packageName")

        return cogs
            .filter { !Modifier.isAbstract(it.modifiers) && !it.isInterface && Cog::class.java.isAssignableFrom(it) }
            .map { it.getDeclaredConstructor().newInstance() }
    }

    @ExperimentalStdlibApi
    fun getCommands(cog: Cog): List<KFunction<*>> {
        log.debug("Scanning ${cog::class.simpleName} for commands...")

        val cogClass = cog::class
        val commands = cogClass.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.hasAnnotation<Command>() }

        log.debug("Found ${commands.size} commands in cog ${cog::class.simpleName}")
        return commands.toList()
    }

    @ExperimentalStdlibApi
    fun loadCommand(meth: KFunction<*>, cog: Cog): CommandFunction {
        require(meth.javaMethod!!.declaringClass == cog::class.java) { "${meth.name} is not from ${cog::class.simpleName}" }
        require(meth.hasAnnotation<Command>()) { "${meth.name} is not annotated with Command!" }

        val category = cog.name()
            ?: cog::class.java.`package`.name.split('.').last().replace('_', ' ').toLowerCase().capitalize()
        val name = meth.name.lowercase()
        val properties = meth.findAnnotation<Command>()!!
        val cooldown = meth.findAnnotation<Cooldown>()
        val ctxParam = meth.valueParameters.firstOrNull { it.type.isSubtypeOf(Context::class.starProjectedType) }

        require(ctxParam != null) { "${meth.name} is missing the Context parameter!" }

        val parameters = meth.valueParameters.filter { it != ctxParam }
        val arguments = loadParameters(parameters)
        val subcommands = getSubCommands(cog)

        val cogParentCommands = cog::class.functions.filter { m -> m.annotations.any { it is Command } }

        if (subcommands.isNotEmpty() && cogParentCommands.size > 1) {
            throw IllegalStateException("SubCommands are present within ${cog::class.simpleName} however there are multiple top-level commands!")
        }

        return CommandFunction(name, category, properties, cooldown, jar, subcommands, meth, cog, arguments, ctxParam)
    }

    @ExperimentalStdlibApi
    fun getSubCommands(cog: Cog): List<SubCommandFunction> {
        log.debug("Scanning ${cog::class.simpleName} for sub-commands...")

        val cogClass = cog::class
        val subcommands = cogClass.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.hasAnnotation<SubCommand>() }
            .map { loadSubCommand(it, cog) }

        log.debug("Found ${subcommands.size} sub-commands in cog ${cog::class.simpleName}")
        return subcommands.toList()
    }

    @ExperimentalStdlibApi
    private fun loadSubCommand(meth: KFunction<*>, cog: Cog): SubCommandFunction {
        require(meth.javaMethod!!.declaringClass == cog::class.java) { "${meth.name} is not from ${cog::class.simpleName}" }
        require(meth.hasAnnotation<SubCommand>()) { "${meth.name} is not annotated with SubCommand!" }

        val name = meth.name.lowercase()
        val properties = meth.findAnnotation<SubCommand>()!!
        val ctxParam = meth.valueParameters.firstOrNull { it.type.isSubtypeOf(Context::class.starProjectedType) }

        require(ctxParam != null) { "${meth.name} is missing the Context parameter!" }

        val parameters = meth.valueParameters.filter { it != ctxParam }
        val arguments = loadParameters(parameters)

        return SubCommandFunction(name, properties, meth, cog, arguments, ctxParam)
    }

    @ExperimentalStdlibApi
    private fun loadParameters(parameters: List<KParameter>): List<Argument> {
        val arguments = mutableListOf<Argument>()

        for (p in parameters) {
            val pName = p.findAnnotation<Name>()?.name ?: p.name ?: p.index.toString()
            val type = p.type.jvmErasure.javaObjectType
            val isGreedy = p.hasAnnotation<Greedy>()
            val isOptional = p.isOptional
            val isNullable = p.type.isMarkedNullable
            val isTentative = p.hasAnnotation<Tentative>()

            if (isTentative && !(isNullable || isOptional)) {
                throw IllegalStateException("${p.name} is marked as tentative, but does not have a default value and is not marked nullable!")
            }

            arguments.add(Argument(pName, type, isGreedy, isOptional, isNullable, isTentative, p))
        }

        return arguments
    }

    companion object {
        private val log = LoggerFactory.getLogger(Indexer::class.java)
    }

}
