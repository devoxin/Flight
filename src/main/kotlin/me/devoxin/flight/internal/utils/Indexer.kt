package me.devoxin.flight.internal.utils

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.SubCommandFunction
import me.devoxin.flight.api.annotations.*
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.api.entities.ObjectStorage
import org.reflections.Reflections
import org.reflections.scanners.MethodParameterNamesScanner
import org.reflections.scanners.Scanners
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Constructor
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
        reflections = Reflections(packageName, MethodParameterNamesScanner(), Scanners.SubTypes)
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

    fun getCogs(objectStorage: ObjectStorage): List<Cog> {
        val cogs = reflections.getSubTypesOf(Cog::class.java)
        log.debug("Discovered ${cogs.size} cogs in $packageName")

        return cogs
            .filter { !Modifier.isAbstract(it.modifiers) && !it.isInterface && Cog::class.java.isAssignableFrom(it) }
            .map { construct(it, objectStorage) }
    }

    fun getCommands(cog: Cog): List<KFunction<*>> {
        log.debug("Scanning ${cog::class.simpleName} for commands...")

        val cogClass = cog::class
        val commands = cogClass.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.hasAnnotation<Command>() }

        log.debug("Found ${commands.size} commands in cog ${cog::class.simpleName}")
        return commands.toList()
    }

    fun loadCommand(meth: KFunction<*>, cog: Cog): CommandFunction {
        require(meth.javaMethod!!.declaringClass == cog::class.java) { "${meth.name} is not from ${cog::class.simpleName}" }
        require(meth.hasAnnotation<Command>()) { "${meth.name} is not annotated with Command!" }

        val categoryOriginal = cog.name()
            ?: cog::class.java.`package`.name.split('.').last().replace('_', ' ')
        val category = TextUtils.capitalise(categoryOriginal)
        val name = meth.name.lowercase()
        val properties = meth.findAnnotation<Command>()!!
        val cooldown = meth.findAnnotation<Cooldown>()
        val ctxParam = meth.valueParameters.firstOrNull { it.type.isSubtypeOf(Context::class.starProjectedType) }

        require(ctxParam != null) { "${meth.name} is missing the Context parameter!" }

        val parameters = meth.valueParameters.filter { it != ctxParam }
        val arguments = loadParameters(cog, parameters)
        val subcommands = getSubCommands(cog)

        val cogParentCommands = cog::class.functions.filter { m -> m.annotations.any { it is Command } }

        if (subcommands.isNotEmpty() && cogParentCommands.size > 1) {
            throw IllegalStateException("Sub-commands are present within ${cog::class.simpleName} however there are multiple top-level commands!")
        }

        return CommandFunction(name, category, properties, cooldown, jar, subcommands, meth, cog, arguments, ctxParam)
    }

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

    private fun loadSubCommand(meth: KFunction<*>, cog: Cog): SubCommandFunction {
        require(meth.javaMethod!!.declaringClass == cog::class.java) { "${meth.name} is not from ${cog::class.simpleName}" }
        require(meth.hasAnnotation<SubCommand>()) { "${meth.name} is not annotated with SubCommand!" }

        val name = meth.name.lowercase()
        val properties = meth.findAnnotation<SubCommand>()!!
        val ctxParam = meth.valueParameters.firstOrNull { it.type.isSubtypeOf(Context::class.starProjectedType) }

        require(ctxParam != null) { "${meth.name} is missing the Context parameter!" }

        val parameters = meth.valueParameters.filter { it != ctxParam }
        val arguments = loadParameters(cog, parameters)

        return SubCommandFunction(name, properties, meth, cog, arguments, ctxParam)
    }

    private fun loadParameters(cog: Cog, parameters: List<KParameter>): List<Argument> {
        val arguments = mutableListOf<Argument>()

        for (p in parameters) {
            val name = p.findAnnotation<Name>()?.value ?: p.name ?: p.index.toString()
            val description = p.findAnnotation<Describe>()?.value ?: "No description available."
            val range = p.findAnnotation<Range>()
            val choices = p.findAnnotation<Choices>()
            val type = p.type.jvmErasure.javaObjectType
            val isGreedy = p.hasAnnotation<Greedy>()
            val isOptional = p.isOptional
            val isNullable = p.type.isMarkedNullable
            val isTentative = p.hasAnnotation<Tentative>()
            val autocomplete = p.findAnnotation<Autocomplete>()
            val autocompleteMethod = autocomplete?.method?.let { cog::class.functions.find { f -> f.name == it } }

            if (isTentative && !(isNullable || isOptional)) {
                throw IllegalStateException("${p.name} is marked as tentative, but does not have a default value and is not marked nullable!")
            }

            if (autocomplete != null && autocompleteMethod == null) {
                throw IllegalStateException("Couldn't find autocompleteMethod with name ${autocomplete.method} for parameter ${p.name}")
            }

            arguments.add(Argument(name, description, range, choices, type, isGreedy, isOptional, isNullable, isTentative, autocompleteMethod, cog, p))
        }

        return arguments
    }

    private fun construct(cls: Class<out Cog>, objectStorage: ObjectStorage): Cog {
        return try {
            cls.getDeclaredConstructor(ObjectStorage::class.java).newInstance(objectStorage)
        } catch (t: NoSuchMethodException) {
            cls.getDeclaredConstructor().newInstance()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Indexer::class.java)
    }
}
