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

    fun removeByCog(cogName: String, ignoreCase: Boolean = true) {
        this.values.removeIf {
            it.cog.name().equals(cogName, ignoreCase)
        }
    }

    fun registerCommands(cog: Cog, indexer: Indexer? = null) {
        val i = indexer ?: Indexer(cog::class.java.`package`.name)
        val commands = i.getCommands(cog)

        for (command in commands) {
            val cmd = i.loadCommand(command, cog)
            this[cmd.name] = cmd
        }
    }

    fun registerCommands(packageName: String) {
        val indexer = Indexer(packageName)
        val cogs = indexer.getCogs()

        for (cogClass in cogs) {
            val cog = cogClass.getDeclaredConstructor().newInstance()
            registerCommands(cog, indexer)
        }
    }

    fun registerCommands(jarPath: String, packageName: String) {
        Indexer(packageName, jarPath).use {
            val cogClasses = it.getCogs()

            for (cls in cogClasses) {
                val cog = cls.getDeclaredConstructor().newInstance()
                registerCommands(cog, it)
            }
        }
    }

}
