package me.devoxin.flight

public class CommandClientBuilder {

    private var prefixes: List<String> = emptyList()
    private var allowMentionPrefix: Boolean = true
    private var useDefaultHelpCommand: Boolean = true
    private var ignoreBots: Boolean = true
    private var prefixProvider: PrefixProvider? = null
    private var eventListeners: MutableList<CommandClientAdapter> = mutableListOf()

    public fun setPrefixes(prefixes: List<String>): CommandClientBuilder {
        this.prefixes = prefixes
        return this
    }

    public fun setPrefixes(vararg prefixes: String): CommandClientBuilder {
        this.prefixes = prefixes.toList()
        return this
    }

    public fun setAllowMentionPrefix(allowMentionPrefix: Boolean): CommandClientBuilder {
        this.allowMentionPrefix = allowMentionPrefix
        return this
    }

    public fun useDefaultHelpCommand(useDefaultHelpCommand: Boolean): CommandClientBuilder {
        this.useDefaultHelpCommand = useDefaultHelpCommand
        return this
    }

    public fun setIgnoreBots(ignoreBots: Boolean): CommandClientBuilder {
        this.ignoreBots = ignoreBots
        return this
    }

    public fun addEventListeners(vararg listeners: CommandClientAdapter): CommandClientBuilder {
        this.eventListeners.addAll(listeners)
        return this
    }

    public fun build(): CommandClient {
        val prefixProvider = this.prefixProvider ?: DefaultPrefixProvider(prefixes, allowMentionPrefix)
        return CommandClient(prefixProvider, useDefaultHelpCommand, ignoreBots, eventListeners.toList())
    }

}
