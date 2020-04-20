package me.devoxin.flight.api

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.devoxin.flight.internal.arguments.ArgParser
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.internal.entities.WaitingEvent
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.api.hooks.CommandEventAdapter
import me.devoxin.flight.api.entities.PrefixProvider
import me.devoxin.flight.internal.entities.CommandRegistry
import me.devoxin.flight.internal.parsers.Parser
import me.devoxin.flight.internal.utils.Indexer
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.reflect.KParameter

class CommandClient(
    parsers: HashMap<Class<*>, Parser<*>>,
    private val prefixProvider: PrefixProvider,
    private val ignoreBots: Boolean,
    private val eventListeners: List<CommandEventAdapter>,
    customOwnerIds: MutableSet<Long>?
) : EventListener {

    private val waiterScheduler = Executors.newSingleThreadScheduledExecutor()
    private val pendingEvents = hashMapOf<Class<*>, HashSet<WaitingEvent<*>>>()
    val commands = CommandRegistry()
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
    @ExperimentalStdlibApi
    fun registerCommands(packageName: String) = commands.registerCommands(packageName)

    /**
     * Registers all commands in the given cog.
     *
     * @param cog
     *        The cog to load commands from.
     * @param indexer
     *        The indexer to use. This can be omitted, but it's better to reuse an indexer if possible.
     */
    @ExperimentalStdlibApi
    fun registerCommands(cog: Cog, indexer: Indexer? = null) = commands.registerCommands(cog, indexer)

    /**
     * Registers all commands in a jar file.
     *
     * @param jarPath
     *        A string-representation of the path to the jar file.
     *
     * @param packageName
     *        The package name to scan for cogs/commands in.
     */
    @ExperimentalStdlibApi
    fun registerCommands(jarPath: String, packageName: String) = commands.registerCommands(jarPath, packageName)

    private fun onMessageReceived(event: MessageReceivedEvent) {
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
                val userCheck = props.userPermissions.filterNot { event.member!!.hasPermission(event.textChannel, it) }

                if (userCheck.isNotEmpty()) {
                    return eventListeners.forEach { it.onUserMissingPermissions(ctx, cmd, userCheck) }
                }
            }

            if (props.botPermissions.isNotEmpty()) {
                val botCheck = props.botPermissions.filterNot { event.guild.selfMember.hasPermission(event.textChannel, it) }

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

        val arguments: HashMap<KParameter, Any?>

        try {
            arguments = ArgParser.parseArguments(cmd, ctx, args)
        } catch (e: BadArgument) {
            return eventListeners.forEach { it.onBadArgument(ctx, cmd, e) }
        } catch (e: Throwable) {
            return eventListeners.forEach { it.onParseError(ctx, cmd, e) }
        }

        val cb = { success: Boolean, err: Throwable? ->
            if (err != null) {
                val handled = cmd.cog.onCommandError(ctx, cmd, err)

                if (!handled) {
                    eventListeners.forEach { it.onCommandError(ctx, cmd, err) }
                }
            }

            eventListeners.forEach { it.onCommandPostInvoke(ctx, cmd, !success) }
        }

        if (cmd.async) {
            GlobalScope.launch {
                cmd.executeAsync(ctx, arguments, complete = cb)
            }
        } else {
            cmd.execute(ctx, arguments, complete = cb)
        }
    }


    // +-------------------+
    // | Execution-Related |
    // +-------------------+
    override fun onEvent(event: GenericEvent) {
        onGenericEvent(event)

        if (event is ReadyEvent) {
            onReady(event)
        } else if (event is MessageReceivedEvent) {
            onMessageReceived(event)
        }
    }

    private fun onReady(event: ReadyEvent) {
        if (ownerIds.isEmpty()) {
            event.jda.retrieveApplicationInfo().queue {
                ownerIds.add(it.owner.idLong)
            }
        }
    }

    private fun onGenericEvent(event: GenericEvent) {
        val events = pendingEvents[event::class.java] ?: return
        val passed = events.filter { it.check(event) }

        events.removeAll(passed)
        passed.forEach { it.accept(event) }
    }

    inline fun <reified T: Event> waitFor(noinline predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T> {
        return waitFor(T::class.java, predicate, timeout)
    }

    fun <T: Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        val we = WaitingEvent(event, predicate, future)

        val set = pendingEvents.computeIfAbsent(event) { hashSetOf() }
        set.add(we)

        if (timeout > 0) {
            waiterScheduler.schedule({
                if (!future.isDone) {
                    future.completeExceptionally(TimeoutException())
                    set.remove(we)
                }
            }, timeout, TimeUnit.MILLISECONDS)
        }

        return future
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandClient::class.java)
    }
}
