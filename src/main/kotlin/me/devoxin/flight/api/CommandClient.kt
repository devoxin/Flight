package me.devoxin.flight.api

import me.devoxin.flight.models.Cog
import me.devoxin.flight.utils.Indexer
import org.slf4j.LoggerFactory

interface CommandClient {
    val commands: MutableMap<String, CommandWrapper>

    /**
     * Registers all commands that are discovered within the given package name.
     *
     * @param packageName
     *        The package name to look for commands in.
     */
    fun registerCommands(packageName: String) {
        val indexer = Indexer(packageName)
        val cogs = indexer.getCogs()

        for (cogClass in cogs) {
            val cog = cogClass.getDeclaredConstructor().newInstance()
            registerCommands(cog, indexer)
        }

        logger.info("Successfully loaded ${commands.size} commands")
    }

    /**
     * Registers all commands in the given class
     *
     * @param cog
     *        The cog to load commands from.
     * @param indexer
     *        The indexer to use. This can be omitted, but it's better to reuse an indexer if possible.
     */
    fun registerCommands(cog: Cog, indexer: Indexer? = null) {
        val i = indexer ?: Indexer(cog::class.java.`package`.name)

        val commands = i.getCommands(cog)

        for (command in commands) {
            val cmd = i.loadCommand(command, cog)
            this.commands[cmd.name] = cmd
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }
}
