package me.devoxin.flight

import me.devoxin.flight.arguments.Snowflake
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.*
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel

public class CommandClientBuilder {

    private var parsers = hashMapOf<Class<*>, Parser<*>>()
    private var prefixes: List<String> = emptyList()
    private var allowMentionPrefix: Boolean = true
    private var useDefaultHelpCommand: Boolean = true
    private var ignoreBots: Boolean = true
    private var prefixProvider: PrefixProvider? = null
    private var eventListeners: MutableList<CommandClientAdapter> = mutableListOf()


    /**
     * Strings that messages must start with to trigger the bot.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun setPrefixes(prefixes: List<String>): CommandClientBuilder {
        this.prefixes = prefixes
        return this
    }

    /**
     * Strings that messages must start with to trigger the bot.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun setPrefixes(vararg prefixes: String): CommandClientBuilder {
        this.prefixes = prefixes.toList()
        return this
    }

    /**
     * Sets the provider used for obtaining prefixes
     */
    public fun setPrefixProvider(provider: PrefixProvider): CommandClientBuilder {
        this.prefixProvider = provider
        return this
    }

    /**
     * Whether the bot will allow mentions to be used as a prefix.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun setAllowMentionPrefix(allowMentionPrefix: Boolean): CommandClientBuilder {
        this.allowMentionPrefix = allowMentionPrefix
        return this
    }

    /**
     * Whether the default help command should be used or not.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun useDefaultHelpCommand(useDefaultHelpCommand: Boolean): CommandClientBuilder {
        this.useDefaultHelpCommand = useDefaultHelpCommand
        return this
    }

    /**
     * Whether bots and webhooks should be ignored. The recommended option is true to prevent feedback loops.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun setIgnoreBots(ignoreBots: Boolean): CommandClientBuilder {
        this.ignoreBots = ignoreBots
        return this
    }

    /**
     * Registers the provided listeners to make use of hooks
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun addEventListeners(vararg listeners: CommandClientAdapter): CommandClientBuilder {
        this.eventListeners.addAll(listeners)
        return this
    }

    /**
     * Registers an argument parser to the given class.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun addCustomParser(klass: Class<*>, parser: Parser<*>): CommandClientBuilder {
        this.parsers[klass] = parser
        return this
    }

    /**
     * Registers all default argument parsers.
     *
     * @return The builder instance. Useful for chaining.
     */
    public fun registerDefaultParsers(): CommandClientBuilder {
        parsers[Int::class.java] = IntParser()
        parsers[Member::class.java] = MemberParser()
        parsers[Role::class.java] = RoleParser()
        parsers[Snowflake::class.java] = SnowflakeParser()
        parsers[String::class.java] = StringParser()
        parsers[TextChannel::class.java] = TextChannelParser()

        return this
    }

    /**
     * Builds a new CommandClient instance
     *
     * @return a CommandClient instance
     */
    public fun build(): CommandClient {
        val prefixProvider = this.prefixProvider ?: DefaultPrefixProvider(prefixes, allowMentionPrefix)
        return CommandClient(parsers, prefixProvider, useDefaultHelpCommand, ignoreBots, eventListeners.toList())
    }

}
