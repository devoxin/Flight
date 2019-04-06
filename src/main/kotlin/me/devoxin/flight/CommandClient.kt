package me.devoxin.flight

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.devoxin.flight.arguments.ArgParser
import me.devoxin.flight.models.Cog
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.Parser
import me.devoxin.flight.utils.Indexer
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class CommandClient(
        private val parsers: HashMap<Class<*>, Parser<*>>,
        private val prefixProvider: PrefixProvider,
        private val useDefaultHelpCommand: Boolean,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter>,
        customOwnerIds: MutableSet<Long>?
) : ListenerAdapter() {

    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val pendingEvents = hashMapOf<Class<*>, HashSet<WaitingEvent<*>>>()
    public val commands = hashMapOf<String, CommandWrapper>()
    public var ownerIds: MutableSet<Long>

    init {
        if (this.useDefaultHelpCommand) {
            registerCommands(NoCategory())
        }

        ownerIds = customOwnerIds ?: mutableSetOf()
    }

    // +------------------+
    // | Custom Functions |
    // +------------------+

    public fun registerCommands(packageName: String) {
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
    public fun registerCommands(cog: Cog, indexer: Indexer? = null) {
        val i = indexer ?: Indexer(cog::class.java.`package`.name)

        val commands = i.getCommands(cog)

        for (command in commands) {
            val cmd = i.loadCommand(command, cog)
            this.commands[cmd.name] = cmd
        }
    }

    // +------------------+
    // |  Event Handling  |
    // +------------------+

    override fun onReady(event: ReadyEvent) {
        if (ownerIds.isEmpty()) {
            event.jda.retrieveApplicationInfo().queue {
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
            GlobalScope.async {
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
        val arguments = cmd.arguments

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


    override fun onGenericEvent(event: GenericEvent) {
        val cls = event::class.java

        if (pendingEvents.containsKey(cls)) {
            val events = pendingEvents[cls]!!
            val passed = events.filter { it.check(event) }

            events.removeAll(passed)
            passed.forEach { it.accept(event) }
        }
    }

    fun <T : Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T?> {
        val future = CompletableFuture<T?>()
        val we = WaitingEvent(event, predicate, future)

        val set = pendingEvents.computeIfAbsent(event) { hashSetOf() }
        set.add(we)

        // TODO: Stuff with the timeout

        return future
    }

}
