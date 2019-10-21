package me.devoxin.flight.api

import com.mewna.catnip.entity.channel.TextChannel
import com.mewna.catnip.entity.guild.Member
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.entity.misc.Ready
import com.mewna.catnip.entity.util.Permission
import com.mewna.catnip.extension.AbstractExtension
import com.mewna.catnip.shard.DiscordEvent
import com.mewna.catnip.shard.event.EventType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.devoxin.flight.arguments.ArgParser
import me.devoxin.flight.exceptions.BadArgument
import me.devoxin.flight.exceptions.AwaitTimeoutException
import me.devoxin.flight.models.Cog
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.Parser
import me.devoxin.flight.utils.Indexer
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CommandClient(
        parsers: HashMap<Class<*>, Parser<*>>,
        private val prefixProvider: PrefixProvider,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter>,
        customOwnerIds: MutableSet<Long>?
): AbstractExtension("Flight command client") {

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    val commands = hashMapOf<String, CommandWrapper>()
    var ownerIds: MutableSet<Long> = customOwnerIds ?: mutableSetOf()

    init {
        ArgParser.parsers.putAll(parsers)
    }

    override fun start() {
        // @todo: Catnip 2
        on(DiscordEvent.READY, this::onReady)
        on(DiscordEvent.MESSAGE_CREATE, this::onMessageReceived)
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
     * Registers all commands in the given class
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


    // +------------------+
    // |  Event Handling  |
    // +------------------+

    fun onReady(event: Ready) {
        if (ownerIds.isEmpty()) {
            event.catnip().rest().user().currentApplicationInformation.thenAccept {
                @Suppress("ControlFlowWithEmptyBody")
                if (it.owner().isTeam) {
                    // @todo: hey discord add members fetching when
                } else {
                    ownerIds.add(it.owner().idAsLong())
                }
            }
        }
    }

    fun onMessageReceived(message: Message) {
        if (ignoreBots && message.author().bot()) {
            return
        }

        val prefixes = prefixProvider.provide(message)
        val trigger = prefixes.firstOrNull { message.content().startsWith(it) } // This will break for "?", "??", "???"
                ?: return

        if (trigger.length == message.content().length) {
            return
        }

        val args = message.content().substring(trigger.length).split(" +".toRegex()).toMutableList()
        val command = args.removeAt(0)

        val cmd = commands[command]
                ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
                ?: return

        val ctx = Context(this, message, trigger)
        val props = cmd.properties

        if (props.developerOnly && !ownerIds.contains(message.author().idAsLong())) {
            return
        }

        if (!message.channel().isGuild && props.guildOnly) {
            return
        }

        if (message.channel().isGuild) {
            if (props.userPermissions.isNotEmpty()) {
                val userCheck = performPermCheck(message.member()!!, message.channel().asTextChannel(), props.userPermissions)

                if (userCheck.isNotEmpty()) {
                    return eventListeners.forEach { it.onUserMissingPermissions(ctx, cmd, userCheck) }
                }
            }

            if (props.botPermissions.isNotEmpty()) {
                val botCheck = performPermCheck(message.guild()!!.selfMember(), message.channel().asTextChannel(), props.botPermissions)

                if (botCheck.isNotEmpty()) {
                    return eventListeners.forEach { it.onBotMissingPermissions(ctx, cmd, botCheck) }
                }
            }

            if (props.nsfw && !message.channel().asTextChannel().nsfw()) {
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
    private fun performPermCheck(member: Member, channel: TextChannel, permissions: Array<Permission>) =
            permissions.filter { !member.permissions(channel).containsAll(permissions.toCollection(mutableListOf())) }

    fun <T : EventType<T>> waitFor(event: EventType<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T?> {
        val future = CompletableFuture<T?>()
        val handler = on(event)
        handler.handler {
            val data = it.body()
            if (predicate(data)) {
                handler.unregister()
                future.complete(it.body())
            }
        }

        if (timeout > 0) {
            scheduler.schedule({
                if (!future.isDone) {
                    future.completeExceptionally(AwaitTimeoutException())
                    handler.unregister()
                }
            }, timeout, TimeUnit.MILLISECONDS)
        }

        return future
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CommandClient::class.java)
    }
}
