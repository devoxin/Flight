package me.devoxin.flight.api.entities

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.annotations.GuildIds
import me.devoxin.flight.api.context.ContextType.SLASH
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.internal.utils.Indexer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.sharding.ShardManager
import org.slf4j.LoggerFactory
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation

class CommandRegistry : HashMap<String, CommandFunction>() {
    val objectStorage = ObjectStorage()

    /**
     * Returns a list of all registered slash commands as [CommandData].
     * If [includeGuildSpecific] is true, this will include any commands annotated with
     * [me.devoxin.flight.api.annotations.GuildIds].
     */
    fun toDiscordCommands(includeGuildSpecific: Boolean = true): List<CommandData> {
        return values.filter { it.contextType >= SLASH }
            .filter { includeGuildSpecific || !it.method.hasAnnotation<GuildIds>() }
            .map(::toCommandData)
            .toList()
    }

    fun toCommandData(command: CommandFunction): CommandData {
        if (command.contextType < SLASH) {
            throw IllegalArgumentException("${command.contextType}-type command cannot be used as a slash command!")
        }

        val data = Commands.slash(command.name, command.properties.description)
            .setGuildOnly(command.properties.guildOnly)
            .setNSFW(command.properties.nsfw)

        if (command.subcommands.isNotEmpty()) {
            for (sc in command.subcommands.values.toSet()) {
                val scData = SubcommandData(sc.name, sc.properties.description)

                if (sc.arguments.isNotEmpty()) {
                    scData.addOptions(sc.arguments.map(Argument::asSlashCommandType))
                }

                data.addSubcommands(scData)
            }
        } else if (command.arguments.isNotEmpty()) {
            data.addOptions(command.arguments.map(Argument::asSlashCommandType))
        }

        return data
    }

    override fun clear() {
        val cogs = values.map(CommandFunction::cog)
        super.clear()
        doUnload(cogs)
    }

    fun findCommandByName(name: String): CommandFunction? {
        return this[name]
    }

    fun findCommandByAlias(alias: String): CommandFunction? {
        return values.firstOrNull { alias in it.properties.aliases }
    }

    fun findCogByName(name: String): Cog? {
        return values.firstOrNull { it.cog.name() == name || it.cog::class.simpleName == name }?.cog
    }

    fun findCommandsByCog(cog: Cog): List<CommandFunction> {
        return values.filter { it.cog == cog }
    }

    fun unload(commandFunction: CommandFunction) {
        values.remove(commandFunction)
        doUnload(commandFunction.cog)
    }

    fun unload(cog: Cog) {
        val commands = values.filter { it.cog == cog }
        values.removeAll(commands)

        commands.map(CommandFunction::cog).let(::doUnload)

        val jar = commands.firstOrNull { it.jar != null }?.jar
            ?: return // No commands loaded from jar, thus no classloader to close.

        val canCloseLoader = values.none { it.jar == jar }

        // No other commands were loaded from the jar, so it's safe to close the loader.
        if (canCloseLoader) {
            jar.close()
        }
    }

    fun unload(jar: Jar) {
        val commands = values.filter { it.jar == jar }
        values.removeAll(commands)

        commands.map(CommandFunction::cog).let(::doUnload)

        jar.close()
    }

    fun register(packageName: String) {
        val indexer = Indexer(packageName)

        for (cog in indexer.getCogs(objectStorage)) {
            register(cog, indexer)
        }
    }

    fun register(jarPath: String, packageName: String) {
        val indexer = Indexer(packageName, jarPath)

        for (cog in indexer.getCogs(objectStorage)) {
            register(cog, indexer)
        }
    }

    fun register(cog: Cog, indexer: Indexer? = null) {
        val i = indexer ?: Indexer(cog::class.java.`package`.name)
        val commands = i.getCommands(cog)

        for (command in commands) {
            val cmd = i.loadCommand(command, cog)

            if (containsKey(cmd.name)) {
                throw RuntimeException("Cannot register command ${cmd.name} as the trigger has already been registered.")
            }

            this[cmd.name] = cmd
        }
    }

    private fun doUnload(cogs: Iterable<Cog>) {
        val uniqueCogs = cogs.distinctBy(Cog::name)

        for (cog in uniqueCogs) {
            doUnload(cog)
        }
    }

    private fun doUnload(cog: Cog) {
        try {
            cog.unload()
        } catch (t: Throwable) {
            log.error("An error occurred whilst unloading cog \"{}\"", cog.name() ?: cog::class.java.simpleName, t)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandRegistry::class.java)
    }
}
