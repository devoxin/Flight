package me.devoxin.flight.internal

import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.models.Cog
import me.devoxin.flight.utils.Indexer

class CommandRegistry : HashMap<String, CommandWrapper>() {

    fun findCommandByName(name: String): CommandWrapper? {
        return this[name]
    }

    fun findCommandByAlias(alias: String): CommandWrapper? {
        return this.values.firstOrNull { it.properties.aliases.contains(alias) }
    }

    fun unload(commandWrapper: CommandWrapper) {
        this.values.remove(commandWrapper)
    }

    fun unload(cog: Cog) {
        val commands = this.values.filter { it.cog == cog }
        this.values.removeAll(commands)

        val jar = commands.firstOrNull { it.jar != null }?.jar
            ?: return // No commands loaded from jar, thus no classloader to close.

        val canCloseLoader = this.values.none { it.jar == jar }

        // No other commands were loaded from the jar, so it's safe to close the loader.
        if (canCloseLoader) {
            println("Closing loader.")
            jar.close()
        }
    }

    fun unload(jar: Jar) {
        val commands = this.values.filter { it.jar == jar }
        this.values.removeAll(commands)

        jar.close()
    }

    @ExperimentalStdlibApi
    fun registerCommands(packageName: String) {
        val indexer = Indexer(packageName)
        val cogs = indexer.getCogs()

        for (cog in cogs) {
            registerCommands(cog, indexer)
        }
    }

    @ExperimentalStdlibApi
    fun registerCommands(jarPath: String, packageName: String) {
        val indexer = Indexer(packageName, jarPath)
        val cogs = indexer.getCogs()

        for (cog in cogs) {
            registerCommands(cog, indexer)
        }
    }

    @ExperimentalStdlibApi
    fun registerCommands(cog: Cog, indexer: Indexer? = null) {
        val i = indexer ?: Indexer(cog::class.java.`package`.name)
        val commands = i.getCommands(cog)

        for (command in commands) {
            val cmd = i.loadCommand(command, cog)

            if (this.containsKey(cmd.name)) {
                continue
            }

            this[cmd.name] = cmd
        }
    }
}
