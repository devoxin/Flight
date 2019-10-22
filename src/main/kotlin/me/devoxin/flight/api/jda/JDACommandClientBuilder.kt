package me.devoxin.flight.api.jda

import me.devoxin.flight.api.CommandClient
import me.devoxin.flight.api.CommandClientBuilder
import me.devoxin.flight.api.DiscordClient
import me.devoxin.flight.models.CommandClientAdapter
import me.devoxin.flight.models.JDAInvite
import me.devoxin.flight.models.PrefixProvider
import me.devoxin.flight.parsers.*
import me.devoxin.flight.parsers.jda.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*

class JDACommandClientBuilder : CommandClientBuilder(DiscordClient.JDA) {
    private var eventListeners: MutableList<CommandClientAdapter<JDA>> = mutableListOf()

    /**
     * Registers the provided listeners to make use of hooks
     *
     * @return The builder instance. Useful for chaining.
     */
    fun addEventListeners(vararg listeners: CommandClientAdapter<JDA>): CommandClientBuilder {
        this.eventListeners.addAll(listeners)
        return this
    }

    override fun registerDefaultParsers(): JDACommandClientBuilder {
        super.registerDefaultParsers()

        val inviteParser = JDAInviteParser()

        parsers[JDAInvite::class.java] = inviteParser
        parsers[net.dv8tion.jda.api.entities.Invite::class.java] = inviteParser
        parsers[Member::class.java] = JDAMemberParser()
        parsers[Role::class.java] = JDARoleParser()
        parsers[TextChannel::class.java] = JDATextChannelParser()
        parsers[User::class.java] = JDAUserParser()
        parsers[VoiceChannel::class.java] = JDAVoiceChannelParser()

        return this
    }

    override fun buildClient(parsers: HashMap<Class<*>, Parser<*>>, prefixProvider: PrefixProvider, ignoreBots: Boolean, ownerIds: MutableSet<Long>?): CommandClient {
        return JDACommandClient(parsers, prefixProvider, ignoreBots, eventListeners.toList(), ownerIds)
    }
}
