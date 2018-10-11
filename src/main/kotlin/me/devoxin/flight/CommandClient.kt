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
        private val ignoreBots: Boolean
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
        var arguments: Map<String, Any?>

        try {
            arguments = parseArgs(ctx, args, cmd)
        } catch (e: BadArgument) {
            ctx.send(e.localizedMessage)
            return
        }

        cmd.execute(ctx, arguments)
    }

    private fun parseArgs(ctx: Context, args: List<String>, cmd: Command): Map<String, Any?> {
        val arguments = cmd.commandArguments()

        if (arguments.isEmpty()) {
            return emptyMap()
        }

        val parser = Arguments(ctx, args)
        val parsed = mutableMapOf<String, Any?>()

        for (arg in arguments) {
            val result = when (arg.type) {
                Arguments.ArgType.Member -> parser.resolveMember(arg.greedy)
                Arguments.ArgType.MemberId -> parser.resolveMemberId(arg.greedy)
                Arguments.ArgType.Role -> parser.resolveRole(arg.greedy)
                Arguments.ArgType.RoleId -> parser.resolveRoleId(arg.greedy)
                Arguments.ArgType.String -> parser.resolveString(arg.greedy)
                Arguments.ArgType.TextChannel -> parser.resolveTextChannel(arg.greedy)
                Arguments.ArgType.TextChannelId -> parser.resolveTextChannelId(arg.greedy)
            }

            if (result == null && arg.required) {
                throw BadArgument("`${arg.name}` must be of type ${arg.type}")
            }

            parsed[arg.name] = result
        }

        return parsed.toMap()
    }
}
