package me.devoxin.flight

import com.google.common.reflect.ClassPath
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier

@Suppress("UnstableApiUsage")
class CommandClient(
        private val prefixProvider: PrefixProvider,
        private val useDefaultHelpCommand: Boolean,
        private val ignoreBots: Boolean,
        val eventListeners: List<CommandClientAdapter>
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    public val commands = hashMapOf<String, Command>()
    public var ownerId: Long = 0L

    init {
        if (this.useDefaultHelpCommand) {
            registerCommands(DefaultHelpCommand())
        }
    }

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

    override fun onReady(event: ReadyEvent) {
        if (ownerId == 0L) {
            event.jda.asBot().applicationInfo.queue {
                ownerId = it.owner.idLong
            }
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (ignoreBots && (event.author.isBot || event.author.isFake)) {
            return
        }

        val prefixes = prefixProvider.provide(event.message)
        val trigger = prefixes.firstOrNull { event.message.contentRaw.startsWith(it) } // This will break for "?", "??", "???"
                ?: return

        if (trigger.length == event.message.contentRaw.length) {
            return
        }

        val args = event.message.contentRaw.substring(trigger.length).split(" +".toRegex()).toMutableList()
        val command = args.removeAt(0)

        val cmd = commands[command]
                ?: commands.values.firstOrNull { it.commandProperties() != null && it.commandProperties()!!.aliases.contains(command) }
                ?: return

        val ctx = Context(this, event, trigger)

        val props = cmd.commandProperties()

        if (props != null && props.developerOnly && event.author.idLong != ownerId) {
            return
        }

        if (event.channelType.isGuild && props != null) {
            if (props.userPermissions.isNotEmpty()) {
                val userCheck = performPermCheck(event.member, event.textChannel, props.userPermissions)

                if (userCheck.isNotEmpty()) {
                    return eventListeners.forEach { it.onUserMissingPermissions(ctx, cmd, userCheck) }
                }
            }

            if (props.botPermissions.isNotEmpty()) {
                val botCheck = performPermCheck(event.guild.selfMember, event.textChannel, props.botPermissions)

                if (botCheck.isNotEmpty()) {
                    return eventListeners.forEach { it.onBotMissingPermissions(ctx, cmd, botCheck) }
                }
            }

            if (props.nsfw && !event.textChannel.isNSFW) {
                return
            }
        }

        if (!event.channelType.isGuild && props != null && props.guildOnly) {
            return
        }

        val shouldExecute = eventListeners.all { it.onCommandPreInvoke(ctx, cmd) }

        if (!shouldExecute) {
            return
        }

        val arguments: Map<String, Any?>

        try {
            arguments = parseArgs(ctx, args, cmd)
        } catch (e: BadArgument) {
            return eventListeners.forEach { it.onBadArgument(ctx, e) }
        } catch (e: Throwable) {
            return eventListeners.forEach { it.onParseError(ctx, e) }
        }

        try {
            cmd.execute(ctx, arguments)
        } catch (e: Throwable) {
            val commandError = CommandError(e, cmd)
            eventListeners.forEach { it.onCommandError(ctx, commandError) }
        }

        eventListeners.forEach { it.onCommandPostInvoke(ctx, cmd) }
    }


    // +-------------------+
    // | Execution-Related |
    // +-------------------+

    private fun performPermCheck(member: Member, channel: TextChannel, permissions: Array<Permission>): Array<Permission> {
        return permissions.filter { !member.hasPermission(channel, it) }.toTypedArray()
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
