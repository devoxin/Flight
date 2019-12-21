package me.devoxin.flight.utils

import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.api.Context
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
import java.io.Closeable
import java.io.File
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaMethod

class Indexer : Closeable {

    private val packageName: String
    private val reflections: Reflections
    private val classLoader: URLClassLoader?

    constructor(packageName: String) {
        this.packageName = packageName
        this.classLoader = null
        reflections = Reflections(packageName, MethodParameterNamesScanner(), SubTypesScanner())
    }

    constructor(packageName: String, jarPath: String) {
        this.packageName = packageName

        val commandJar = File(jarPath)
        check(commandJar.exists()) { "jarPath points to a non-existent file." }
        check(commandJar.extension == "jar") { "jarPath leads to a file which is not a jar." }

        val path = URL("jar:file:${commandJar.absolutePath}!/")
        this.classLoader = URLClassLoader.newInstance(arrayOf(path))
        reflections = Reflections(packageName, this.classLoader, MethodParameterNamesScanner(), SubTypesScanner())
    }

    fun getCogs(): List<Class<out Cog>> {
        logger.debug("Scanning $packageName for cogs...")
        val cogs = reflections.getSubTypesOf(Cog::class.java)
        logger.debug("Found ${cogs.size} cogs")

        return cogs
                .filter { !Modifier.isAbstract(it.modifiers) && !it.isInterface && Cog::class.java.isAssignableFrom(it) }
                .toList()
    }

    fun getCommands(cog: Cog): List<Method> {
        logger.debug("Scanning ${cog.name()} for commands...")
        val commands = cog::class.java.methods.filter { it.isAnnotationPresent(Command::class.java) }
        logger.debug("Found ${commands.size} commands in cog ${cog.name()}")

        return commands.toList()
    }

    @ExperimentalStdlibApi
    fun getCommands(cog: KClass<out Cog>): List<KFunction<*>> {
        logger.debug("Scanning ${cog.simpleName} for commands...")
        val commands = cog.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.hasAnnotation<Command>() }

        logger.debug("Found ${commands.size} commands in cog ${cog.simpleName}")
        return commands.toList()
    }

    @ExperimentalStdlibApi
    fun loadCommand(meth: KFunction<*>, cog: Cog): CommandWrapper {
        require(meth.javaClass.declaringClass == cog::class.java) { "${meth.name} is not from ${cog.name()}" }
        require(meth.hasAnnotation<Command>()) { "${meth.name} is not annotated with Command!" }

        val category = cog.name()
        val name = meth.name
        val properties = meth.findAnnotation<Command>()!!
        val async = meth.isSuspend

        val parameters = meth.parameters
            .filter { it.type != Context::class }

        val arguments = mutableListOf<Argument>()

        for (p in parameters) {
            val pName = p.findAnnotation<Name>()?.name ?: p.name ?: p.index.toString()
            val type = p.type
            val greedy = p.hasAnnotation<Greedy>()
            val required = !p.type.isMarkedNullable

            arguments.add(Argument(pName, type::class.java, greedy, required))
        }

        return CommandWrapper(name, arguments.toList(), category, properties, async, meth.javaMethod!!, cog)
    }

    fun loadCommand(meth: Method, cog: Cog): CommandWrapper {
        require(meth.declaringClass == cog::class.java) { "${meth.name} is not from ${cog.name()}" }
        require(meth.isAnnotationPresent(Command::class.java)) { "${meth.name} is not annotated with Command!" }

        val category = cog.name()
        val name = meth.name
        val properties = meth.getAnnotation(Command::class.java)
        val async = meth.isAnnotationPresent(Async::class.java)

        val allParamNames = getParamNames(meth)
        val paramNames = allParamNames.drop(allParamNames.indexOf("this") + 2)
                .filter { !it.startsWith("$") } // Continuation, Completion
        val parameters = meth.parameters.filter { it.type != Context::class.java && it.type != Continuation::class.java }

        require(paramNames.size == parameters.size) {
            "Parameter count mismatch in command ${meth.name}, expected: ${parameters.size}, got: ${paramNames.size}\n" +
                "Expected: ${parameters.map { it.type.simpleName }}\n" +
                "Got: ${paramNames.joinToString(", ")}\n"
        }

        val arguments = mutableListOf<Argument>()

        for ((i, p) in parameters.withIndex()) {
            val pName = if (p.isAnnotationPresent(Name::class.java)) {
                p.getAnnotation(Name::class.java).name
            } else {
                paramNames[i]
            }
            val type = p.type
            val greedy = p.isAnnotationPresent(Greedy::class.java)
            val required = !p.isAnnotationPresent(Optional::class.java)

            arguments.add(Argument(pName, type, greedy, required))
        }

        return CommandWrapper(name, arguments.toList(), category, properties, async, meth, cog)
    }

    fun getParamNames(meth: Method): List<String> {
        return reflections.getMethodParamNames(meth)
    }

    override fun close() {
        this.classLoader?.close()
        // classLoader must be closed otherwise external jar files will remain open
        // which kinda defeats the purpose of reloadable commands.
        // This is only a problem if loading from jar files anyway.
    }

    companion object {
        private val logger = LoggerFactory.getLogger(Indexer::class.java)
    }

}
