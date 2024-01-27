package me.devoxin.flight.api

import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.context.ContextType
import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.context.SlashContext
import me.devoxin.flight.api.entities.BucketType
import me.devoxin.flight.api.entities.CheckType
import me.devoxin.flight.internal.arguments.ArgParser
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.internal.entities.WaitingEvent
import me.devoxin.flight.api.entities.CooldownProvider
import me.devoxin.flight.api.hooks.CommandEventAdapter
import me.devoxin.flight.api.entities.PrefixProvider
import me.devoxin.flight.api.entities.CommandRegistry
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.reflect.KParameter

class CommandClient(
    private val prefixProvider: PrefixProvider,
    val cooldownProvider: CooldownProvider,
    private val ignoreBots: Boolean,
    private val eventListeners: List<CommandEventAdapter>,
    private val commandExecutor: ExecutorService?,
    val ownerIds: MutableSet<Long>
) : EventListener {
    private val waiterScheduler = Executors.newSingleThreadScheduledExecutor()
    private val pendingEvents = hashMapOf<Class<*>, HashSet<WaitingEvent<*>>>()
    val commands = CommandRegistry()

    /**
     * Checks whether the provided [message] is a command.
     *
     * @param message
     *        The message to check.
     * @return True, if the message is a command.
     */
    fun isCommand(message: Message): Boolean {
        val prefixes = prefixProvider.provide(message)
        val trigger = prefixes.firstOrNull(message.contentRaw::startsWith) // This will break for repetitive prefixes
            ?: return false

        if (trigger.length == message.contentRaw.length) {
            return false
        }

        val args = message.contentRaw.substring(trigger.length).split(" +".toRegex()).toMutableList()
        val command = args.removeAt(0).lowercase()

        return (commands[command] ?: commands.findCommandByAlias(command)) != null
    }

    private fun onMessageReceived(event: MessageReceivedEvent) {
        if (ignoreBots && (event.author.isBot || event.isWebhookMessage)) {
            return
        }

        val prefixes = prefixProvider.provide(event.message)
        val trigger = prefixes.firstOrNull(event.message.contentRaw::startsWith) // This will break for repetitive prefixes
            ?: return

        if (trigger.length == event.message.contentRaw.length) {
            return
        }

        val args = event.message.contentRaw.substring(trigger.length).split(" +".toRegex()).toMutableList()
        val command = args.removeAt(0).lowercase()

        val cmd = commands[command]
            ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
            ?: return dispatchSafely { it.onUnknownCommand(event, command, args) }

        val subcommand = args.firstOrNull()?.lowercase().let { cmd.subcommands[it] ?: cmd.subcommandAliases[it] }
        val invoked = subcommand ?: cmd

        if (subcommand != null) {
            args.removeAt(0)
        }

        val ctx = MessageContext(this, event, trigger, invoked)

        if (isOnCooldown(cmd, ctx)) { // This function dispatches the event.
            return
        }

        if (!shouldExecuteCommand(ctx, cmd)) {
            return
        }

        val arguments: HashMap<KParameter, Any?>

        try {
            arguments = ArgParser.parseArguments(invoked, ctx, args, cmd.properties.argDelimiter)
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

        setCooldown(cmd, ctx)
        invoked.execute(ctx, arguments, cb, commandExecutor)
    }

    private fun onSlashCommand(event: SlashCommandInteractionEvent) {
        val cmd = commands[event.name] ?: return
        val subcommand = event.subcommandName?.let { cmd.subcommands[it] ?: return }
        val invoked = subcommand ?: cmd
        val ctx = SlashContext(this, event, invoked)

        if (isOnCooldown(cmd, ctx)) {
            return
        }

        if (!shouldExecuteCommand(ctx, cmd)) {
            return
        }

        val arguments = invoked.resolveArguments(event.options)
        val cb = { success: Boolean, err: Throwable? ->
            if (err != null) {
                val handled = cmd.cog.onCommandError(ctx, cmd, err)

                if (!handled) {
                    dispatchSafely { it.onCommandError(ctx, cmd, err) }
                }
            }

            dispatchSafely { it.onCommandPostInvoke(ctx, cmd, !success) }
        }

        setCooldown(cmd, ctx)
        invoked.execute(ctx, arguments, cb, null)
    }

    private fun onAutocomplete(event: CommandAutoCompleteInteractionEvent) {
        val commandName = event.name
        val subcommandName = event.subcommandName

        val command = commands[commandName]
            ?: return

        val subcommand = subcommandName?.let { command.subcommands[subcommandName] ?: return }

        val executable = subcommand ?: command
        val argument = executable.arguments.find { it.name == event.focusedOption.name }
            ?: return

        val cb = { err: Throwable? ->
            if (err != null) {
                dispatchSafely { it.onAutocompleteError(event, err) }
            }
        }

        argument.executeAutocomplete(event, cb, commandExecutor)
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
                is CommandAutoCompleteInteractionEvent -> onAutocomplete(event)
                //else -> println(event)
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

    private fun shouldExecuteCommand(ctx: Context, cmd: CommandFunction): Boolean {
        val props = cmd.properties

        val contextType = ctx.contextType
        if (cmd.contextType != contextType && cmd.contextType != ContextType.MESSAGE_OR_SLASH) {
            dispatchSafely { it.onCheckFailed(ctx, cmd, CheckType.EXECUTION_CONTEXT) }
            return false
        }

        if (props.developerOnly && !ownerIds.contains(ctx.author.idLong)) {
            dispatchSafely { it.onCheckFailed(ctx, cmd, CheckType.DEVELOPER_CHECK) }
            return false
        }

        if (!ctx.isFromGuild && props.guildOnly) {
            dispatchSafely { it.onCheckFailed(ctx, cmd, CheckType.GUILD_CHECK) }
            return false
        }

        if (ctx.isFromGuild) {
            if (props.userPermissions.isNotEmpty()) {
                val userCheck = props.userPermissions.filterNot { ctx.member!!.hasPermission(ctx.guildChannel!!, it) }

                if (userCheck.isNotEmpty()) {
                    dispatchSafely { it.onUserMissingPermissions(ctx, cmd, userCheck) }
                    return false
                }
            }

            if (props.botPermissions.isNotEmpty()) {
                val botCheck = props.botPermissions.filterNot { ctx.guild!!.selfMember.hasPermission(ctx.guildChannel!!, it) }

                if (botCheck.isNotEmpty()) {
                    dispatchSafely { it.onBotMissingPermissions(ctx, cmd, botCheck) }
                    return false
                }
            }

            if (props.nsfw && (ctx.guildChannel as? StandardGuildMessageChannel)?.isNSFW != true) {
                dispatchSafely { it.onCheckFailed(ctx, cmd, CheckType.NSFW_CHECK) }
                return false
            }
        }

        return eventListeners.all { it.onCommandPreInvoke(ctx, cmd) }
            && cmd.cog.localCheck(ctx, cmd)
    }

    private fun isOnCooldown(cmd: CommandFunction, ctx: Context): Boolean {
        if (cmd.cooldown != null) {
            val entityId = when (cmd.cooldown.bucket) {
                BucketType.USER -> ctx.author.idLong
                BucketType.GUILD -> ctx.guild?.idLong //?: ctx.messageChannel.idLong
                BucketType.GLOBAL -> -1
            }

            if (entityId != null) {
                if (cooldownProvider.isOnCooldown(entityId, cmd.cooldown.bucket, cmd)) {
                    val time = cooldownProvider.getCooldownTime(entityId, cmd.cooldown.bucket, cmd)
                    dispatchSafely { it.onCommandCooldown(ctx, cmd, time) }

                    return true
                }
            }
        }

        return false
    }

    private fun setCooldown(cmd: CommandFunction, ctx: Context) {
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
    }

    companion object {
        private val log = LoggerFactory.getLogger(CommandClient::class.java)

        fun builder() = CommandClientBuilder()

        fun create(config: CommandClientBuilder.() -> Unit): CommandClient {
            return CommandClientBuilder().apply(config).build()
        }
    }
}
