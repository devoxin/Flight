package me.devoxin.flight.api

import me.devoxin.flight.api.arguments.types.Emoji
import me.devoxin.flight.api.arguments.types.Invite
import me.devoxin.flight.api.arguments.types.Snowflake
import me.devoxin.flight.api.entities.*
import me.devoxin.flight.api.hooks.CommandEventAdapter
import me.devoxin.flight.api.hooks.DefaultCommandEventAdapter
import me.devoxin.flight.internal.arguments.ArgParser
import me.devoxin.flight.internal.parsers.*
import me.devoxin.flight.internal.parsers.TextChannelParser
import me.devoxin.flight.internal.parsers.VoiceChannelParser
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import java.net.URL
import java.util.concurrent.ExecutorService

class CommandClientBuilder {
    private var prefixes: List<String> = emptyList()
    private var allowMentionPrefix: Boolean = true
    private var helpCommandConfig: DefaultHelpCommandConfig = DefaultHelpCommandConfig()
    private var ignoreBots: Boolean = true
    private var prefixProvider: PrefixProvider? = null
    private var cooldownProvider: CooldownProvider? = null
    private var eventListeners: MutableList<CommandEventAdapter> = mutableListOf()
    private var commandExecutor: ExecutorService? = null
    private val ownerIds: MutableSet<Long> = mutableSetOf()


    /**
     * Strings that messages must start with to trigger the bot.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setPrefixes(prefixes: List<String>): CommandClientBuilder {
        this.prefixes = prefixes
        return this
    }

    /**
     * Strings that messages must start with to trigger the bot.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setPrefixes(vararg prefixes: String): CommandClientBuilder {
        this.prefixes = prefixes.toList()
        return this
    }

    /**
     * Sets the provider used for obtaining prefixes
     */
    fun setPrefixProvider(provider: PrefixProvider): CommandClientBuilder {
        this.prefixProvider = provider
        return this
    }

    /**
     * Sets the provider used for cool-downs.
     */
    fun setCooldownProvider(provider: CooldownProvider): CommandClientBuilder {
        this.cooldownProvider = provider
        return this
    }

    /**
     * Whether the bot will allow mentions to be used as a prefix.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setAllowMentionPrefix(allowMentionPrefix: Boolean): CommandClientBuilder {
        this.allowMentionPrefix = allowMentionPrefix
        return this
    }

    /**
     * Whether the default help command should be used or not.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun configureDefaultHelpCommand(config: DefaultHelpCommandConfig.() -> Unit): CommandClientBuilder {
        config(helpCommandConfig)
        return this
    }

    /**
     * Whether bots and webhooks should be ignored. The recommended option is true to prevent feedback loops.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setIgnoreBots(ignoreBots: Boolean): CommandClientBuilder {
        this.ignoreBots = ignoreBots
        return this
    }

    /**
     * Uses the given list of IDs as the owners. Any users with the given IDs
     * are then able to use commands marked with `developerOnly`.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setOwnerIds(vararg ids: Long): CommandClientBuilder {
        this.ownerIds.clear()
        this.ownerIds.addAll(ids.toTypedArray())
        return this
    }

    /**
     * Uses the given list of IDs as the owners. Any users with the given IDs
     * are then able to use commands marked with `developerOnly`.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setOwnerIds(vararg ids: String): CommandClientBuilder {
        this.ownerIds.clear()
        this.ownerIds.addAll(ids.map(String::toLong))
        return this
    }

    /**
     * Registers the provided listeners to make use of hooks
     *
     * @return The builder instance. Useful for chaining.
     */
    fun addEventListeners(vararg listeners: CommandEventAdapter): CommandClientBuilder {
        this.eventListeners.addAll(listeners)
        return this
    }

    /**
     * Registers an argument parser to the given class.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun addCustomParser(klass: Class<*>, parser: Parser<*>): CommandClientBuilder {
        // This is kinda unsafe. Would use T, but nullable/boxed types revert
        // to their java.lang counterparts. E.g. Int? becomes java.lang.Integer,
        // but Int remains kotlin.Int.
        // See https://youtrack.jetbrains.com/issue/KT-35423

        ArgParser.parsers[klass] = parser
        return this
    }

    inline fun <reified T> addCustomParser(parser: Parser<T>) = addCustomParser(T::class.java, parser)

    /**
     * Registers all default argument parsers.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun registerDefaultParsers(): CommandClientBuilder {
        // Kotlin types and primitives
        val booleanParser = BooleanParser()
        ArgParser.parsers[Boolean::class.java] = booleanParser
        ArgParser.parsers[java.lang.Boolean::class.java] = booleanParser

        val doubleParser = DoubleParser()
        ArgParser.parsers[Double::class.java] = doubleParser
        ArgParser.parsers[java.lang.Double::class.java] = doubleParser

        val floatParser = FloatParser()
        ArgParser.parsers[Float::class.java] = floatParser
        ArgParser.parsers[java.lang.Float::class.java] = floatParser

        val intParser = IntParser()
        ArgParser.parsers[Int::class.java] = intParser
        ArgParser.parsers[java.lang.Integer::class.java] = intParser

        val longParser = LongParser()
        ArgParser.parsers[Long::class.java] = longParser
        ArgParser.parsers[java.lang.Long::class.java] = longParser

        // JDA entities
        val inviteParser = InviteParser()
        ArgParser.parsers[Invite::class.java] = inviteParser
        ArgParser.parsers[net.dv8tion.jda.api.entities.Invite::class.java] = inviteParser

        ArgParser.parsers[Member::class.java] = MemberParser()
        ArgParser.parsers[Role::class.java] = RoleParser()
        ArgParser.parsers[TextChannel::class.java] = TextChannelParser()
        ArgParser.parsers[User::class.java] = UserParser()
        ArgParser.parsers[VoiceChannel::class.java] = VoiceChannelParser()

        // Custom entities
        ArgParser.parsers[Emoji::class.java] = EmojiParser()
        ArgParser.parsers[String::class.java] = StringParser()
        ArgParser.parsers[Snowflake::class.java] = SnowflakeParser()
        ArgParser.parsers[URL::class.java] = UrlParser()

        return this
    }

    /**
     * Sets the thread pool used for executing commands.
     *
     * @param executorPool
     *        The pool to use. If null is given, commands will be executed on the WebSocket thread.
     *
     * @return The builder instance, useful for chaining.
     */
    fun setExecutionThreadPool(executorPool: ExecutorService?): CommandClientBuilder {
        this.commandExecutor = executorPool
        return this
    }

    /**
     * Builds a new CommandClient instance
     *
     * @return a CommandClient instance
     */
    fun build(): CommandClient {
        if (eventListeners.isEmpty()) {
            eventListeners.add(DefaultCommandEventAdapter())
        }

        val prefixProvider = this.prefixProvider ?: DefaultPrefixProvider(prefixes, allowMentionPrefix)
        val cooldownProvider = this.cooldownProvider ?: DefaultCooldownProvider()
        val commandClient = CommandClient(prefixProvider, cooldownProvider, ignoreBots, eventListeners.toList(),
            commandExecutor, ownerIds)

        if (helpCommandConfig.enabled) {
            commandClient.commands.register(DefaultHelpCommand(helpCommandConfig.showParameterTypes))
        }

        return commandClient
    }
}
