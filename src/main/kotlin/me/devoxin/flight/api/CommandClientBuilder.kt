package me.devoxin.flight.api

import me.devoxin.flight.internal.DefaultPrefixProvider
import me.devoxin.flight.arguments.Snowflake
import me.devoxin.flight.internal.DefaultHelpCommand
import me.devoxin.flight.models.Emoji
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.*
import java.net.URL

abstract class CommandClientBuilder(private val client: DiscordClient) {
    protected var parsers = hashMapOf<Class<*>, Parser<*>>()
    protected var prefixes: List<String> = emptyList()
    protected var allowMentionPrefix: Boolean = true
    protected var useDefaultHelpCommand: Boolean = true
    protected var showParameterTypes: Boolean = false
    protected var ignoreBots: Boolean = true
    protected var prefixProvider: PrefixProvider? = null
    protected var ownerIds: MutableSet<Long>? = null

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
    fun useDefaultHelpCommand(useDefaultHelpCommand: Boolean, showParameterTypes: Boolean = false): CommandClientBuilder {
        this.useDefaultHelpCommand = useDefaultHelpCommand
        this.showParameterTypes = showParameterTypes
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
        this.ownerIds = mutableSetOf(*ids.toTypedArray())
        return this
    }

    /**
     * Uses the given list of IDs as the owners. Any users with the given IDs
     * are then able to use commands marked with `developerOnly`.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun setOwnerIds(vararg ids: String): CommandClientBuilder {
        this.ownerIds = mutableSetOf(*ids.map { it.toLong() }.toTypedArray())
        return this
    }

    /**
     * Registers an argument parser to the given class.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun addCustomParser(klass: Class<Any>, parser: Parser<*>): CommandClientBuilder {
        this.parsers[klass] = parser
        return this
    }

    /**
     * Registers all default argument parsers.
     *
     * @return The builder instance. Useful for chaining.
     */
    open fun registerDefaultParsers(): CommandClientBuilder {
        parsers[Emoji::class.java] = EmojiParser()
        parsers[Int::class.java] = IntParser()
        parsers[Snowflake::class.java] = SnowflakeParser()
        parsers[String::class.java] = StringParser()
        parsers[URL::class.java] = UrlParser()
        return this
    }

    /**
     * Builds a new CommandClient instance
     *
     * @return a CommandClient instance
     */
    fun build(): CommandClient {
        val prefixProvider = this.prefixProvider ?: DefaultPrefixProvider(prefixes, allowMentionPrefix)
        val commandClient = buildClient(parsers, prefixProvider, ignoreBots, ownerIds)

        if (useDefaultHelpCommand) {
            commandClient.registerCommands(DefaultHelpCommand(showParameterTypes))
        }

        return commandClient
    }

    protected abstract fun buildClient(parsers: HashMap<Class<*>, Parser<*>>, prefixProvider: PrefixProvider, ignoreBots: Boolean, ownerIds: MutableSet<Long>?): CommandClient
}
