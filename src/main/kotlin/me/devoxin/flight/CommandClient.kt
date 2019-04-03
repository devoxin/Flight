package me.devoxin.flight

import com.google.common.reflect.ClassPath
import com.mewna.catnip.Catnip
import com.mewna.catnip.entity.channel.TextChannel
import com.mewna.catnip.entity.guild.Member
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.entity.misc.Ready
import com.mewna.catnip.entity.util.Permission
import com.mewna.catnip.shard.event.EventType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import me.devoxin.flight.annotations.Async
import me.devoxin.flight.annotations.Command
import me.devoxin.flight.arguments.ArgParser
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
        private val catnip: Catnip,
        private val parsers: HashMap<Class<*>, Parser<*>>,
        private val prefixProvider: PrefixProvider,
        private val useDefaultHelpCommand: Boolean,
        private val ignoreBots: Boolean,
        private val eventListeners: List<CommandClientAdapter>,
        customOwnerIds: MutableSet<Long>?
) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private val waiterScheduler = Executors.newSingleThreadScheduledExecutor()!!
    public val commands = hashMapOf<String, CommandWrapper>()
    public var ownerIds: MutableSet<Long>

    init {
        if (this.useDefaultHelpCommand) {
            registerCommands(DefaultHelpCommand())
        }

        ownerIds = customOwnerIds ?: mutableSetOf()
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

    fun onReady(event: Ready) {
        if (ownerIds.isEmpty()) {
            event.catnip().rest().user().currentApplicationInformation.thenAccept {
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

        if (!message.channel().isGuild && props.guildOnly) {
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
        return permissions.filter { !member.permissions(channel).containsAll(permissions.toCollection(mutableListOf())) }.toTypedArray()
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

    fun <T : EventType<T>> waitFor(event: EventType<T>, predicate: (T) -> Boolean, timeout: Long): CompletableFuture<T?> {
        val future = CompletableFuture<T?>()
        val handler = catnip.on(event)
        handler.handler {
            val data = it.body()
            if (predicate(data)) {
                handler.unregister()
                future.complete(it.body())
            }
        }

        if (timeout > 0) {
            Thread {
                Thread.sleep(timeout)
                if (!future.isCancelled) {
                    future.completeExceptionally(AwaitTimeoutException())
                    handler.unregister()
                }
            }.start()
        }

        return future
    }

}
