package me.devoxin.flight

import com.google.common.reflect.ClassPath
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

@Suppress("UnstableApiUsage")
class CommandClient(
        private val prefixProvider: PrefixProvider,
        private val useDefaultHelpCommand: Boolean,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter>
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val commands = hashMapOf<String, Command>()

    // +------------------+
    // | Custom Functions |
    // +------------------+

    public fun registerCommands(packageName: String) {
        logger.debug("Scanning $packageName for commands...")
        val classes = ClassPath.from(this.javaClass.classLoader).getTopLevelClassesRecursive(packageName)
        logger.debug("Found ${classes.size} commands")


        for (clazz in classes) {
            val klass = clazz.load()

            if (Modifier.isAbstract(klass.modifiers) || klass.isInterface) {
                continue
            }

            val command = klass.getDeclaredConstructor().newInstance() as Command
            this.commands[command.name()] = command
        }

        logger.info("Successfully loaded ${commands.size} commands")
    }

    public fun registerCommands(vararg commands: Command) {
        commands.forEach { this.commands[it.name()] = it }
    }


    // +------------------+
    // |  Event Handling  |
    // +------------------+

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (ignoreBots && (event.author.isBot || event.author.isFake)) {
            return
        }

        val prefixes = prefixProvider.provide(event.message)
        val trigger = prefixes.firstOrNull { event.message.contentRaw.startsWith(it) }
                ?: return

        if (trigger.length == event.message.contentRaw.length) {
            return
        }

        val args = event.message.contentRaw.substring(trigger.length).split(" +".toRegex()).toMutableList()
        val command = args.removeAt(0)

        if (!commands.containsKey(command)) {
            return
        }

        val cmd = commands[command]!!
        val ctx = Context(event, trigger)
        val arguments: Map<String, Any?>

        try {
            arguments = parseArgs(ctx, args, cmd)
        } catch (e: BadArgument) {
            return eventListeners.forEach { it.onBadArgument(ctx, e) }
        } catch (e: Throwable) {
            return eventListeners.forEach { it.onParseError(ctx, e) }
        }

        val shouldExecute = eventListeners.all { it.onCommandPreInvoke(ctx, cmd) }

        if (!shouldExecute) {
            return
        }

        try {
            cmd.execute(ctx, arguments)
        } catch (e: Throwable) {
            val commandError = CommandError(e, cmd)
            eventListeners.forEach { it.onCommandError(ctx, commandError) }
        }

        eventListeners.forEach { it.onCommandPostInvoke(ctx, cmd) }
    }

    private fun parseArgs(ctx: Context, args: MutableList<String>, cmd: Command): Map<String, Any?> {
        val arguments = cmd.commandArguments()

        if (arguments.isEmpty()) {
            return emptyMap()
        }

        val parser = Arguments(ctx, args)
        val parsed = mutableMapOf<String, Any?>()

        for (arg in arguments) {
            val result = parser.parse(arg)
            parsed[arg.name] = result
        }

        return parsed.toMap()
    }

}
