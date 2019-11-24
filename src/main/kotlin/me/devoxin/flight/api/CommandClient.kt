package me.devoxin.flight.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.devoxin.flight.arguments.ArgParser
import me.devoxin.flight.exceptions.BadArgument
import me.devoxin.flight.exceptions.AwaitTimeoutException
import me.devoxin.flight.internal.DefaultHelpCommand
import me.devoxin.flight.internal.WaitingEvent
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
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CommandClient(
        parsers: HashMap<Class<*>, Parser<*>>,
        private val prefixProvider: PrefixProvider,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter>,
        customOwnerIds: MutableSet<Long>?
) : ListenerAdapter() {

    private val waiterScheduler = Executors.newSingleThreadScheduledExecutor()
    private val pendingEvents = hashMapOf<Class<*>, HashSet<WaitingEvent<*>>>()
    val commands = hashMapOf<String, CommandWrapper>()
    val ownerIds = customOwnerIds ?: mutableSetOf()

    init {
        ArgParser.parsers.putAll(parsers)
    }


    // +------------------+
    // | Custom Functions |
    // +------------------+

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
     * Registers all commands in the given class.
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

    /**
     * Registers all commands in a jar file.
     *
     * @param jar
     *        A file representing the jar.
     *
     * @param pkg
     *        The package name to scan for commands in.
     *
     * @param indexer
     *        The indexer to use. This can be omitted, but it's better to reuse an indexer if possible.
     */
    fun registerCommands(jar: File, pkg: String, indexer: Indexer? = null) {
        require(jar.exists()) { "File does not exist!" }

        val path = URL("jar:file:${jar.absolutePath}!/")
        val classLoader = URLClassLoader.newInstance(arrayOf(path))

        val reflections = Reflections(pkg, classLoader)

        reflections.getSubTypesOf(Cog::class.java).map { it.simpleName }
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

        if (!event.channelType.isGuild && props.guildOnly) {
            return
        }

        if (event.channelType.isGuild) {
            if (props.userPermissions.isNotEmpty()) {
                val userCheck = performPermCheck(event.member!!, event.textChannel, props.userPermissions)

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

        val shouldExecute = eventListeners.all { it.onCommandPreInvoke(ctx, cmd) }
                && cmd.cog.localCheck(ctx, cmd)

        if (!shouldExecute) {
            return
        }

        val arguments: Array<Any?>

        try {
            arguments = ArgParser.parseArguments(cmd, ctx, args)
        } catch (e: BadArgument) {
            return eventListeners.forEach { it.onBadArgument(ctx, e) }
        } catch (e: Throwable) {
            return eventListeners.forEach { it.onParseError(ctx, e) }
        }

        val cb = { success: Boolean, err: CommandError? ->
            if (err != null) {
                val handled = err.command.cog.onCommandError(ctx, err)

                if (!handled) {
                    eventListeners.forEach { it.onCommandError(ctx, err) }
                }
            }

            eventListeners.forEach { it.onCommandPostInvoke(ctx, cmd, !success) }
        }

        if (cmd.async) {
            GlobalScope.launch {
                cmd.executeAsync(ctx, *arguments, complete = cb)
            }
        } else {
            cmd.execute(ctx, *arguments, complete = cb)
        }
    }


    // +-------------------+
    // | Execution-Related |
    // +-------------------+

    private fun performPermCheck(member: Member, channel: TextChannel,
                                 permissions: Array<Permission>) = permissions.filter { !member.hasPermission(channel, it) }


    override fun onGenericEvent(event: GenericEvent) {
        val cls = event::class.java
        val events = pendingEvents[cls] ?: return
        val passed = events.filter { it.check(event) }

        events.removeAll(passed)
        passed.forEach { it.accept(event) }
    }

    fun <T: Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T?> {
        val future = CompletableFuture<T?>()
        val we = WaitingEvent(event, predicate, future)

        val set = pendingEvents.computeIfAbsent(event) { hashSetOf() }
        set.add(we)

        if (timeout > 0) {
            waiterScheduler.schedule({
                if (!future.isDone) {
                    future.completeExceptionally(AwaitTimeoutException())
                    set.remove(we)
                }
            }, timeout, TimeUnit.MILLISECONDS)
        }

        return future
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandClient::class.java)
    }
}
