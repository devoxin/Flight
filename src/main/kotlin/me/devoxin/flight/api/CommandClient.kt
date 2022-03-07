package me.devoxin.flight.api

import me.devoxin.flight.api.entities.BucketType
import me.devoxin.flight.internal.arguments.ArgParser
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.internal.entities.WaitingEvent
import me.devoxin.flight.api.entities.CooldownProvider
import me.devoxin.flight.api.hooks.CommandEventAdapter
import me.devoxin.flight.api.entities.PrefixProvider
import me.devoxin.flight.internal.entities.CommandRegistry
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.KParameter

class CommandClient(
    private val prefixProvider: PrefixProvider,
    private val cooldownProvider: CooldownProvider,
    private val ignoreBots: Boolean,
    private val eventListeners: List<CommandEventAdapter>,
    private val commandExecutor: ExecutorService?,
    customOwnerIds: MutableSet<Long>
) : EventListener {
    private val waiterScheduler = Executors.newSingleThreadScheduledExecutor()
    private val pendingEvents = hashMapOf<Class<*>, HashSet<WaitingEvent<*>>>()
    val commands = CommandRegistry()
    val ownerIds = customOwnerIds

    private fun onMessageReceived(event: MessageReceivedEvent) {
        if (ignoreBots && (event.author.isBot || event.isWebhookMessage)) {
            return
        }

        val prefixes = prefixProvider.provide(event.message)
        val trigger = prefixes.firstOrNull { event.message.contentRaw.startsWith(it) } // This will break for "?", "??", "???"
                ?: return

        if (trigger.length == event.message.contentRaw.length) {
            return
        }

        val args = event.message.contentRaw.substring(trigger.length).split(" +".toRegex()).toMutableList()
        val command = args.removeAt(0).lowercase()

        val cmd = commands[command]
                ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
                ?: return dispatchSafely { it.onUnknownCommand(event, command, args) }

        val subcommand = args.firstOrNull()?.let { cmd.subcommands[it.lowercase()] }
        val invoked = subcommand ?: cmd

        if (subcommand != null) {
            args.removeAt(0)
        }

        val ctx = MessageContext(this, event, trigger, invoked)

        if (cmd.cooldown != null) {
            val entityId = when (cmd.cooldown.bucket) {
                BucketType.USER -> ctx.author.idLong
                BucketType.GUILD -> ctx.guild?.idLong
                BucketType.GLOBAL -> -1
            }

            if (entityId != null) {
                if (cooldownProvider.isOnCooldown(entityId, cmd.cooldown.bucket, cmd)) {
                    val time = cooldownProvider.getCooldownTime(entityId, cmd.cooldown.bucket, cmd)
                    return dispatchSafely { it.onCommandCooldown(ctx, cmd, time) }
                }
            }
        }

        val props = cmd.properties

        if (props.developerOnly && !ownerIds.contains(event.author.idLong)) {
            return
        }

        if (!event.channelType.isGuild && props.guildOnly) {
            return
        }
        // TODO: More events for failed checks

        if (event.channelType.isGuild) {
            if (props.userPermissions.isNotEmpty()) {
                val userCheck = props.userPermissions.filterNot { event.member!!.hasPermission(event.textChannel, it) }

                if (userCheck.isNotEmpty()) {
                    return dispatchSafely { it.onUserMissingPermissions(ctx, cmd, userCheck) }
                }
            }

            if (props.botPermissions.isNotEmpty()) {
                val botCheck = props.botPermissions.filterNot { event.guild.selfMember.hasPermission(event.textChannel, it) }

                if (botCheck.isNotEmpty()) {
                    return dispatchSafely { it.onBotMissingPermissions(ctx, cmd, botCheck) }
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

        val exc = subcommand ?: cmd
        val arguments: HashMap<KParameter, Any?>

        try {
            arguments = ArgParser.parseArguments(exc, ctx, args, cmd.properties.argDelimiter)
        } catch (e: BadArgument) {
            return dispatchSafely { it.onBadArgument(ctx, cmd, e) }
        } catch (e: Throwable) {
            return dispatchSafely { it.onParseError(ctx, cmd, e) }
        }

        val cb = { success: Boolean, err: Throwable? ->
            if (err != null) {
                val handled = cmd.cog.onCommandError(ctx, cmd, err)

                if (!handled) {
                    dispatchSafely { it.onCommandError(ctx, cmd, err) }
                }
            }

            dispatchSafely { it.onCommandPostInvoke(ctx, cmd, !success) }
        }

        if (cmd.cooldown != null && cmd.cooldown.duration > 0) {
            val entityId = when (cmd.cooldown.bucket) {
                BucketType.USER -> ctx.author.idLong
                BucketType.GUILD -> ctx.guild?.idLong
                BucketType.GLOBAL -> -1
            }

            if (entityId != null) {
                val time = cmd.cooldown.timeUnit.toMillis(cmd.cooldown.duration)
                cooldownProvider.setCooldown(entityId, cmd.cooldown.bucket, time, cmd)
            }
        }

        exc.execute(ctx, arguments, cb, commandExecutor)
    }

    private fun onSlashCommand(event: SlashCommandInteractionEvent) {

    }


    // +-------------------+
    // | Execution-Related |
    // +-------------------+
    override fun onEvent(event: GenericEvent) {
        onGenericEvent(event)

        try {
            when (event) {
                is ReadyEvent -> onReady(event)
                is MessageReceivedEvent -> onMessageReceived(event)
                is SlashCommandInteractionEvent -> onSlashCommand(event)
            }
        } catch (e: Throwable) {
            dispatchSafely { it.onInternalError(e) }
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

    private fun dispatchSafely(invoker: (CommandEventAdapter) -> Unit) {
        try {
            eventListeners.forEach(invoker)
        } catch (e: Throwable) {
            try {
                eventListeners.forEach { it.onInternalError(e) }
            } catch (inner: Throwable) {
                log.error("An uncaught exception occurred during event dispatch!", inner)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandClient::class.java)
    }
}
