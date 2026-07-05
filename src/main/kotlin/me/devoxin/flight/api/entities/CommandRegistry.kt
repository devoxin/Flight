package me.devoxin.flight.api.entities

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.ContextType.SLASH
import me.devoxin.flight.internal.arguments.Argument
import me.devoxin.flight.internal.entities.Jar
import me.devoxin.flight.internal.utils.ExceptionUtils
import me.devoxin.flight.internal.utils.Indexer
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.slf4j.LoggerFactory

class CommandRegistry : HashMap<String, CommandFunction>() {
    val objectStorage = ObjectStorage()

    /**
     * Serializes every registered command into [CommandData] to allow syncing via JDA.
     * Optionally takes a [predicate] that should return {@code true} if the command should be
     * included in the resulting [CommandData] list. This is useful for per-guild command syncing.
     *
     * @param predicate The predicate to use for command filtering.
     */
    fun toDiscordCommands(predicate: (CommandFunction) -> Boolean = { true }): List<CommandData> {
        return values.filter { it.contextType >= SLASH }
            .filter(predicate)
            .map(::toCommandData)
            .toList()
    }

    /**
     * Serializes a single command into [CommandData] that can be passed to JDA for syncing.
     *
     * @param command The command to serialize.
     */
    fun toCommandData(command: CommandFunction): CommandData {
        if (command.contextType < SLASH) {
            throw IllegalArgumentException("${command.contextType}-type command cannot be used as a slash command!")
        }

        val contexts = mutableListOf(InteractionContextType.GUILD)

        if (!command.properties.guildOnly) {
            contexts.add(InteractionContextType.PRIVATE_CHANNEL)
            contexts.add(InteractionContextType.BOT_DM)
        }

        val data = Commands.slash(command.name, command.properties.description)
            .setContexts(contexts)
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

    /**
     * Clears all registered commands.
     */
    override fun clear() {
        val cogs = values.map(CommandFunction::cog)
        super.clear()
        doUnload(cogs)
    }

    /**
     * Finds a single command by its registered name.
     */
    fun findCommandByName(name: String): CommandFunction? {
        return this[name]
    }

    /**
     * Finds a single command by its registered aliases.
     */
    fun findCommandByAlias(alias: String): CommandFunction? {
        return values.firstOrNull { alias in it.properties.aliases }
    }

    /**
     * Finds a single cog by its registered name.
     */
    fun findCogByName(name: String): Cog? {
        return values.firstOrNull { it.cog.name() == name || it.cog::class.simpleName == name }?.cog
    }

    /**
     * Finds all commands by their associated cog.
     */
    fun findCommandsByCog(cog: Cog): List<CommandFunction> {
        return values.filter { it.cog == cog }
    }

    /**
     * Unloads a single command, cleaning up its associated cog as needed.
     */
    fun unload(commandFunction: CommandFunction) {
        values.remove(commandFunction)

        if (values.none { it.cog == commandFunction.cog }) {
            // unload the command's cog if there are no other registered commands from this cog.
            doUnload(commandFunction.cog)
        }
    }

    /**
     * Unloads a single cog.
     */
    fun unload(cog: Cog) {
        val commands = values.filter { it.cog == cog }
        values.removeAll(commands)

        doUnload(cog)

        val jar = commands.firstOrNull { it.jar != null }?.jar
            ?: return // No commands loaded from jar, thus no classloader to close.

        val canCloseLoader = values.none { it.jar == jar }

        // No other commands were loaded from the jar, so it's safe to close the loader.
        if (canCloseLoader) {
            ExceptionUtils.suppressed { jar.close() }
        }
    }

    /**
     * Unloads a single command, cleaning up its associated cog as needed.
     */
    fun unload(jar: Jar) {
        val commands = values.filter { it.jar == jar }
        values.removeAll(commands)

        val cogs = commands.map { it.cog }.distinct()

        for (cog in cogs) {
            if (values.none { it.cog == cog }) {
                doUnload(cog)
            }
        }

        ExceptionUtils.suppressed { jar.close() }
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
