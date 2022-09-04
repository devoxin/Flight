package me.devoxin.flight.api.entities

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.ContextType.SLASH
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.internal.utils.Indexer
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

class CommandRegistry : HashMap<String, CommandFunction>() {
    fun toDiscordCommands(): List<CommandData> {
        val commands = mutableListOf<CommandData>()

        for (command in this.values.filter { it.contextType >= SLASH }) {
            val data = Commands.slash(command.name, command.properties.description)
            data.isGuildOnly = command.properties.guildOnly

            if (command.subcommands.isNotEmpty()) {
                for (sc in command.subcommands.values) {
                    val scData = SubcommandData(sc.name, sc.properties.description)

                    if (sc.arguments.isNotEmpty()) {
                        val options = getArgumentsAsOptions(sc.arguments)
                        scData.addOptions(options)
                    }

                    data.addSubcommands(scData)
                }
            } else if (command.arguments.isNotEmpty()) {
                val options = getArgumentsAsOptions(command.arguments)
                data.addOptions(options)
            }

            commands.add(data)
        }

        return commands
    }

    private fun getArgumentsAsOptions(arguments: List<Argument>): List<OptionData> {
        return arguments.map {
            val (type, required) = it.asSlashCommandType()

            val option = OptionData(type, it.slashFriendlyName, it.description, required)

            it.range?.let { r ->
                r.double.takeIf { t -> t.isNotEmpty() }?.let { range ->
                    option.setMinValue(range[0])
                    range.elementAtOrNull(1)?.let(option::setMaxValue)
                }

                r.long.takeIf { t -> t.isNotEmpty() }?.let { range ->
                    option.setMinValue(range[0])
                    range.elementAtOrNull(1)?.let(option::setMaxValue)
                }
            }

            option
        }
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

    fun register(packageName: String) {
        val indexer = Indexer(packageName)

        for (cog in indexer.getCogs()) {
            register(cog, indexer)
        }
    }

    fun register(jarPath: String, packageName: String) {
        val indexer = Indexer(packageName, jarPath)

        for (cog in indexer.getCogs()) {
            register(cog, indexer)
        }
    }

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
