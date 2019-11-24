package me.devoxin.flight.api

import me.devoxin.flight.internal.DefaultPrefixProvider
import me.devoxin.flight.arguments.Snowflake
import me.devoxin.flight.internal.DefaultHelpCommand
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.Emoji
import me.devoxin.flight.models.Invite
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.*
import net.dv8tion.jda.api.entities.*
import java.net.URL

class CommandClientBuilder {

    private var parsers = hashMapOf<Class<*>, Parser<*>>()
    private var prefixes: List<String> = emptyList()
    private var allowMentionPrefix: Boolean = true
    private var helpCommandConfig: DefaultHelpCommandConfig = DefaultHelpCommandConfig()
    private var ignoreBots: Boolean = true
    private var prefixProvider: PrefixProvider? = null
    private var eventListeners: MutableList<CommandClientAdapter> = mutableListOf()
    private var ownerIds: MutableSet<Long>? = null


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
     * Registers the provided listeners to make use of hooks
     *
     * @return The builder instance. Useful for chaining.
     */
    fun addEventListeners(vararg listeners: CommandClientAdapter): CommandClientBuilder {
        this.eventListeners.addAll(listeners)
        return this
    }

    /**
     * Registers an argument parser to the given class.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun addCustomParser(klass: Class<*>, parser: Parser<*>): CommandClientBuilder {
        this.parsers[klass] = parser
        return this
    }

    /**
     * Registers all default argument parsers.
     *
     * @return The builder instance. Useful for chaining.
     */
    fun registerDefaultParsers(): CommandClientBuilder {
        val inviteParser = InviteParser()

        parsers[Emoji::class.java] = EmojiParser()
        parsers[Int::class.java] = IntParser()
        parsers[Invite::class.java] = inviteParser
        parsers[net.dv8tion.jda.api.entities.Invite::class.java] = inviteParser
        parsers[Member::class.java] = MemberParser()
        parsers[Role::class.java] = RoleParser()
        parsers[Snowflake::class.java] = SnowflakeParser()
        parsers[String::class.java] = StringParser()
        parsers[TextChannel::class.java] = TextChannelParser()
        parsers[URL::class.java] = UrlParser()
        parsers[User::class.java] = UserParser()
        parsers[VoiceChannel::class.java] = VoiceChannelParser()

        return this
    }

    /**
     * Builds a new CommandClient instance
     *
     * @return a CommandClient instance
     */
    fun build(): CommandClient {
        val prefixProvider = this.prefixProvider ?: DefaultPrefixProvider(prefixes, allowMentionPrefix)
        val commandClient = CommandClient(parsers, prefixProvider, ignoreBots, eventListeners.toList(), ownerIds)

        if (helpCommandConfig.enabled) {
            commandClient.registerCommands(DefaultHelpCommand(helpCommandConfig.showParameterTypes))
        }

        return commandClient
    }

}
