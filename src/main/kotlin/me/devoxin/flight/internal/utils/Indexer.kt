package me.devoxin.flight.internal.utils

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Cooldown
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.Name
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.api.entities.Cog
import org.reflections.Reflections
import org.reflections.scanners.MethodParameterNamesScanner
import org.reflections.scanners.SubTypesScanner
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.valueParameters
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
        reflections = Reflections(packageName, this.classLoader, MethodParameterNamesScanner(), SubTypesScanner())
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
        val cogClass = cog::class
        log.debug("Scanning ${cog.name()} for commands...")
        val commands = cogClass.members
            .filterIsInstance<KFunction<*>>()
            .filter { it.hasAnnotation<Command>() }

        log.debug("Found ${commands.size} commands in cog ${cog.name()}")
        return commands.toList()
    }

    @ExperimentalStdlibApi
    fun loadCommand(meth: KFunction<*>, cog: Cog): CommandFunction {
        require(meth.javaMethod!!.declaringClass == cog::class.java) { "${meth.name} is not from ${cog.name()}" }
        require(meth.hasAnnotation<Command>()) { "${meth.name} is not annotated with Command!" }

        val category = cog.name()
        val name = meth.name.toLowerCase()
        val properties = meth.findAnnotation<Command>()!!
        val cooldown = meth.findAnnotation<Cooldown>()
        val async = meth.isSuspend
        val ctxParam = meth.valueParameters.firstOrNull { it.type.classifier?.equals(Context::class) == true }

        require(ctxParam != null) { "${meth.name} is missing the Context parameter!" }

        val parameters = meth.valueParameters
            .filterNot { it.type.classifier?.equals(Context::class) == true }

        val arguments = mutableListOf<Argument>()

        for (p in parameters) {
            val pName = p.findAnnotation<Name>()?.name ?: p.name ?: p.index.toString()
            val type = p.type.jvmErasure.javaObjectType
            val greedy = p.hasAnnotation<Greedy>()
            val optional = p.isOptional
            val isNullable = p.type.isMarkedNullable

            arguments.add(Argument(pName, type, greedy, optional, isNullable, p))
        }

        return CommandFunction(name, arguments, category, properties, cooldown, async, meth, cog, jar, ctxParam)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Indexer::class.java)
    }

}
