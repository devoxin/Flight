package me.devoxin.flight

import com.google.common.reflect.ClassPath
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import me.devoxin.flight.annotations.Async
import me.devoxin.flight.annotations.Command
import me.devoxin.flight.arguments.ArgParser
import me.devoxin.flight.models.Cog
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.Parser
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
        private val parsers: HashMap<Class<*>, Parser<*>>,
        private val prefixProvider: PrefixProvider,
        private val useDefaultHelpCommand: Boolean,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter>,
        private val customOwnerIds: MutableSet<Long>?
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    public val commands = hashMapOf<String, CommandWrapper>()
    public var ownerIds: MutableSet<Long>

    init {
        if (this.useDefaultHelpCommand) {
            registerCommands(NoCategory::class.java)
        }

        ownerIds = customOwnerIds ?: mutableSetOf()
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

            if (Modifier.isAbstract(klass.modifiers) || klass.isInterface || !Cog::class.java.isAssignableFrom(klass)) {
                continue
            }

            registerCommands(klass)
        }

        logger.info("Successfully loaded ${commands.size} commands")
    }

    public fun registerCommands(klass: Class<*>) {
        if (!Cog::class.java.isAssignableFrom(klass)) {
            throw IllegalArgumentException("${klass.simpleName} must implement `Cog`!")
        }

        val cog = klass.getDeclaredConstructor().newInstance() as Cog
        val category = cog.name().replace("_", " ")

        for (meth in klass.methods) {
            if (!meth.isAnnotationPresent(Command::class.java)) {
                continue
            }

            val name = meth.name.toLowerCase()
            val properties = meth.getAnnotation(Command::class.java)
            val async = meth.isAnnotationPresent(Async::class.java)

            val wrapper = CommandWrapper(name, category, properties, async, meth, cog)
            this.commands[name] = wrapper
        }
    }

    // +------------------+
    // |  Event Handling  |
    // +------------------+

    override fun onReady(event: ReadyEvent) {
        if (ownerIds.isEmpty()) {
            event.jda.asBot().applicationInfo.queue {
                ownerIds.add(it.owner.idLong)
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
                ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
                ?: return

        val ctx = Context(this, event, trigger)

        val props = cmd.properties

        if (props.developerOnly && !ownerIds.contains(event.author.idLong)) {
            return
        }

        if (event.channelType.isGuild) {
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

        if (!event.channelType.isGuild && props.guildOnly) {
            return
        }

        val shouldExecute = eventListeners.all { it.onCommandPreInvoke(ctx, cmd) }
                && cmd.cog.localCheck(ctx, cmd)

        if (!shouldExecute) {
            return
        }

        val arguments: Array<Any?>

        try {
            arguments = parseArgs(ctx, args, cmd)
        } catch (e: BadArgument) {
            return eventListeners.forEach { it.onBadArgument(ctx, e) }
        } catch (e: Throwable) {
            return eventListeners.forEach { it.onParseError(ctx, e) }
        }

        if (cmd.async) {
            GlobalScope
                    .async {
                        cmd.executeAsync(ctx, *arguments) { success, err ->
                            if (err != null) {
                                handleCommandError(ctx, err)
                            }

                            handleCommandCompletion(ctx, cmd, !success)
                        }
                    }
        } else {
            cmd.execute(ctx, *arguments) { success, err ->
                if (err != null) {
                    handleCommandError(ctx, err)
                }

                handleCommandCompletion(ctx, cmd, !success)
            }
        }
    }

    private fun handleCommandError(ctx: Context, error: CommandError) {
        val handled = error.command.cog.onCommandError(ctx, error)
        if (!handled) {
            eventListeners.forEach { it.onCommandError(ctx, error) }
        }
    }

    private fun handleCommandCompletion(ctx: Context, cmd: CommandWrapper, failed: Boolean) {
        eventListeners.forEach { it.onCommandPostInvoke(ctx, cmd, failed) }
    }


    // +-------------------+
    // | Execution-Related |
    // +-------------------+

    private fun performPermCheck(member: Member, channel: TextChannel, permissions: Array<Permission>): Array<Permission> {
        return permissions.filter { !member.hasPermission(channel, it) }.toTypedArray()
    }

    private fun parseArgs(ctx: Context, args: MutableList<String>, cmd: CommandWrapper): Array<Any?> {
        val arguments = cmd.commandArguments()

        if (arguments.isEmpty()) {
            return emptyArray()
        }

        val parser = ArgParser(parsers, ctx, args)
        val parsed = mutableListOf<Any?>()

        for (arg in arguments) {
            parsed.add(parser.parse(arg))
        }

        return parsed.toTypedArray()
    }

}
