package me.devoxin.flight.internal.arguments

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.devoxin.flight.api.annotations.Range
import me.devoxin.flight.api.entities.Cog
import me.devoxin.flight.internal.entities.Executable
import me.devoxin.flight.internal.entities.Executable.Companion
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import java.util.concurrent.ExecutorService
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend

class Argument(
    val name: String,
    val description: String,
    val range: Range?,
    val type: Class<*>,
    val greedy: Boolean,
    val optional: Boolean, // Denotes that a parameter has a default value.
    val isNullable: Boolean,
    val isTentative: Boolean,
    val autocompleteHandler: KFunction<*>?,
    internal val cog: Cog,
    internal val kparam: KParameter
) {
    val slashFriendlyName: String by lazy {
        return@lazy name.replace(SLASH_NAME_REGEX, "_$1").lowercase()
    }

    val autocompleteSupported = autocompleteHandler != null

    /**
     * Returns this argument as a [Pair]<[OptionType], [Boolean]>.
     * The [OptionType] represents the type of this argument.
     * The [Boolean] represents whether the argument is required. True if it is, false otherwise.
     */
    fun asSlashCommandType(): Pair<OptionType, Boolean> {
        val optionType = OPTION_TYPE_MAPPING[type]
            ?: throw IllegalStateException("Unable to find OptionType for type ${type.simpleName}")

        return optionType to (!isNullable && !optional)
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun getEntityFromOptionMapping(mapping: OptionMapping): Pair<KParameter, Any?> {
        val mappingType = when (mapping.type) {
            OptionType.STRING -> mapping.asString
            OptionType.INTEGER -> mapping.asLong
            OptionType.BOOLEAN -> mapping.asBoolean
            OptionType.USER -> {
                when (type) {
                    Member::class.java -> mapping.asMember
                    User::class.java -> mapping.asUser
                    else -> throw IllegalStateException("OptionType is user but argument type is ${type.simpleName}")
                }
            }
            OptionType.CHANNEL -> mapping.asChannel
            OptionType.ROLE -> mapping.asRole
            OptionType.NUMBER -> mapping.asDouble
            OptionType.ATTACHMENT -> mapping.asAttachment
            else -> throw IllegalStateException("Unsupported OptionType ${mapping.type.name}")
        }

        return kparam to mappingType
    }

    fun format(withType: Boolean): String {
        return buildString {
            if (optional || isNullable) {
                append('[')
            } else {
                append('<')
            }

            append(name)

            if (withType) {
                append(": ")
                append(type.simpleName)
            }

            if (optional || isNullable) {
                append(']')
            } else {
                append('>')
            }
        }
    }

    fun executeAutocomplete(event: CommandAutoCompleteInteractionEvent, callback: (Throwable?) -> Unit, executor: ExecutorService?) {
        if (autocompleteHandler == null) {
            return callback(IllegalStateException("Cannot process autocomplete event as $name does not have a registered handler!"))
        }

        if (autocompleteHandler.isSuspend) {
            DEFAULT_DISPATCHER.launch {
                executeAutocompleteAsync(event, callback)
            }
        } else {
            executor?.execute { executeAutocompleteSync(event, callback) }
                ?: executeAutocompleteSync(event, callback)
        }
    }

    private fun executeAutocompleteSync(event: CommandAutoCompleteInteractionEvent, callback: (Throwable?) -> Unit) {
        try {
            autocompleteHandler?.call(cog, event)
            callback(null)
        } catch (e: Throwable) {
            callback(e)
        }
    }

    private suspend fun executeAutocompleteAsync(event: CommandAutoCompleteInteractionEvent, callback: (Throwable?) -> Unit) {
        try {
            autocompleteHandler?.callSuspend(cog, event)
            callback(null)
        } catch (e: Throwable) {
            callback(e)
        }
    }

    companion object {
        private val DEFAULT_DISPATCHER = CoroutineScope(Dispatchers.Default)

        val SLASH_NAME_REGEX = "((?<=[a-z])[A-Z]|[A-Z](?=[a-z]))".toRegex()

        val OPTION_TYPE_MAPPING = mapOf(
            String::class.java to OptionType.STRING,

            Integer::class.java to OptionType.INTEGER,
            java.lang.Integer::class.java to OptionType.INTEGER,

            Long::class.java to OptionType.INTEGER,
            java.lang.Long::class.java to OptionType.INTEGER,

            Double::class.java to OptionType.NUMBER,
            java.lang.Double::class.java to OptionType.NUMBER,

            Boolean::class.java to OptionType.BOOLEAN,
            java.lang.Boolean::class.java to OptionType.BOOLEAN,

            Member::class.java to OptionType.USER,
            User::class.java to OptionType.USER,
            GuildChannel::class.java to OptionType.CHANNEL,
            TextChannel::class.java to OptionType.CHANNEL,
            VoiceChannel::class.java to OptionType.CHANNEL,
            Role::class.java to OptionType.ROLE,
            Message.Attachment::class.java to OptionType.ATTACHMENT
        )
    }
}
