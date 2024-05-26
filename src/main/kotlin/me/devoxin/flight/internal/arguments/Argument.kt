package me.devoxin.flight.internal.arguments

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.devoxin.flight.api.annotations.Choices
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.Range
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import java.util.concurrent.ExecutorService
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend

class Argument(
    /** The argument's parameter name */
    val name: String,
    /** The argument's description, as given in the [Describe] annotation */
    val description: String,
    val range: Range?,
    val choices: Choices?,
    /** The parameter type for this argument **/
    val type: Class<*>,
    val greedy: Boolean,
    val optional: Boolean, // Denotes that a parameter has a default value.
    val isNullable: Boolean,
    val isTentative: Boolean,
    val autocompleteHandler: KFunction<*>?,
    internal val cog: Cog,
    val parameter: KParameter
) {
    val slashFriendlyName = name.replace(SLASH_NAME_REGEX, "_$1").lowercase()
    val autocompleteSupported = autocompleteHandler != null

    /**
     * Returns this argument as a [Pair]<[OptionType], [Boolean]>.
     * The [OptionType] represents the type of this argument.
     * The [Boolean] represents whether the argument is required. True if it is, false otherwise.
     */
    fun asSlashCommandType(): OptionData {
        val optionType = OPTION_TYPE_MAPPING[type]
            ?: throw IllegalStateException("Unable to find OptionType for type ${type.simpleName}")

        val option = OptionData(optionType, slashFriendlyName, description, !isNullable && !optional, autocompleteSupported)

        range?.let {
            it.double.takeIf(DoubleArray::isNotEmpty)?.let { range ->
                option.setMinValue(range[0])
                range.elementAtOrNull(1)?.let(option::setMaxValue)
            }

            it.long.takeIf(LongArray::isNotEmpty)?.let { range ->
                option.setMinValue(range[0])
                range.elementAtOrNull(1)?.let(option::setMaxValue)
            }

            it.string.takeIf(IntArray::isNotEmpty)?.let { range ->
                option.setMinLength(range[0])
                range.elementAtOrNull(1)?.let(option::setMaxLength)
            }
        }

        choices?.let { choices ->
            choices.double.takeIf { it.isNotEmpty() }?.let { option.addChoices(it.map { c -> Choice(c.key, c.value) }) }
            choices.long.takeIf { it.isNotEmpty() }?.let { option.addChoices(it.map { c -> Choice(c.key, c.value) }) }
            choices.string.takeIf { it.isNotEmpty() }?.let { option.addChoices(it.map { c -> Choice(c.key, c.value) }) }
        }

        return option
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
            OptionType.NUMBER -> when (type) {
                Float::class.java, java.lang.Float::class.java -> mapping.asDouble.toFloat()
                else -> mapping.asDouble
            }
            else -> throw IllegalStateException("Unsupported OptionType ${mapping.type.name}")
        }

        return parameter to mappingType
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
            Float::class.java to OptionType.NUMBER,
            java.lang.Float::class.java to OptionType.NUMBER,

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
