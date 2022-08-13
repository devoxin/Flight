package me.devoxin.flight.internal.entities

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.ContextType.SLASH
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.utils.Indexer
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData

class CommandRegistry : HashMap<String, CommandFunction>() {
    fun toDiscordCommands(): List<CommandData> {
        val commands = mutableListOf<CommandData>()

        for (command in this.values.filter { it.properties.executionContext >= SLASH }) {
            val data = Commands.slash(command.name, command.properties.description)
            data.isGuildOnly = command.properties.guildOnly

            for (argument in command.arguments) {
                val (type, required) = argument.asSlashCommandType()

                val option = OptionData(type, argument.slashFriendlyName, argument.description, required)

                argument.range?.let {
                    it.double.takeIf { r -> r.isNotEmpty() }?.let { range ->
                        option.setMinValue(range[0])
                        range.elementAtOrNull(1)?.let(option::setMaxValue)
                    }

                    it.long.takeIf { r -> r.isNotEmpty() }?.let { range ->
                        option.setMinValue(range[0])
                        range.elementAtOrNull(1)?.let(option::setMaxValue)
                    }
                }

                data.addOptions(option)
            }

            // TODO: subcommands
            commands.add(data)
        }

        return commands
    }

    fun findCommandByName(name: String): CommandFunction? {
        return this[name]
    }

    fun findCommandByAlias(alias: String): CommandFunction? {
        return this.values.firstOrNull { it.properties.aliases.contains(alias) }
    }

    fun findCogByName(name: String): Cog? {
        return this.values.firstOrNull { it.cog::class.simpleName == name }?.cog
    }

    fun findCommandsByCog(cog: Cog): List<CommandFunction> {
        return this.values.filter { it.cog == cog }
    }

    fun unload(commandFunction: CommandFunction) {
        this.values.remove(commandFunction)
    }

    fun unload(cog: Cog) {
        val commands = this.values.filter { it.cog == cog }
        this.values.removeAll(commands)

        val jar = commands.firstOrNull { it.jar != null }?.jar
            ?: return // No commands loaded from jar, thus no classloader to close.

        val canCloseLoader = this.values.none { it.jar == jar }

        // No other commands were loaded from the jar, so it's safe to close the loader.
        if (canCloseLoader) {
            jar.close()
        }
    }

    fun unload(jar: Jar) {
        val commands = this.values.filter { it.jar == jar }
        this.values.removeAll(commands)

        jar.close()
    }

    @ExperimentalStdlibApi
    fun register(packageName: String) {
        val indexer = Indexer(packageName)

        for (cog in indexer.getCogs()) {
            register(cog, indexer)
        }
    }

    @ExperimentalStdlibApi
    fun register(jarPath: String, packageName: String) {
        val indexer = Indexer(packageName, jarPath)

        for (cog in indexer.getCogs()) {
            register(cog, indexer)
        }
    }

    @ExperimentalStdlibApi
    fun register(cog: Cog, indexer: Indexer? = null) {
        val i = indexer ?: Indexer(cog::class.java.`package`.name)
        val commands = i.getCommands(cog)

        for (command in commands) {
            val cmd = i.loadCommand(command, cog)

            if (this.containsKey(cmd.name)) {
                throw RuntimeException("Cannot register command ${cmd.name}; the trigger has already been registered.")
            }

            this[cmd.name] = cmd
        }
    }
}
