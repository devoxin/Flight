package me.devoxin.flight.api.jda

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.api.CommandError
import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.api.Context
import me.devoxin.flight.arguments.ArgParser
import me.devoxin.flight.exceptions.BadArgument
import me.devoxin.flight.exceptions.AwaitTimeoutException
import me.devoxin.flight.internal.WaitingEvent
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.Parser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class JDACommandClient(
        parsers: HashMap<Class<*>, Parser<*>>,
        private val prefixProvider: PrefixProvider,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter<JDA>>,
        customOwnerIds: MutableSet<Long>?
) : CommandClient, ListenerAdapter() {
    override val commands = hashMapOf<String, CommandWrapper>()

    private val waiterScheduler = Executors.newSingleThreadScheduledExecutor()
    private val pendingEvents = hashMapOf<Class<*>, HashSet<WaitingEvent<*>>>()
    var ownerIds: MutableSet<Long> = customOwnerIds ?: mutableSetOf()

    init {
        ArgParser.parsers.putAll(parsers)
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

        val ctx = JDAContext(this, event, trigger)
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
            @Suppress("UNCHECKED_CAST")
            arguments = ArgParser.parseArguments(cmd, ctx as Context<Any>, args)
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
    private fun performPermCheck(member: Member, channel: TextChannel, permissions: Array<Permission>) =
            permissions.filter { !member.hasPermission(channel, it) }

    override fun onGenericEvent(event: GenericEvent) {
        val cls = event::class.java
        val events = pendingEvents[cls] ?: return
        val passed = events.filter { it.check(event) }

        events.removeAll(passed)
        passed.forEach { it.accept(event) }
    }

    // @todo: move this
    fun <T : Event> waitFor(event: Class<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T?> {
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
}
