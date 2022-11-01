package me.devoxin.flight.internal.arguments

import me.devoxin.flight.api.annotations.Range
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.reflect.KParameter

class Argument(
    val name: String,
    val description: String,
    val range: Range?,
    val type: Class<*>,
    val greedy: Boolean,
    val optional: Boolean, // Denotes that a parameter has a default value.
    val isNullable: Boolean,
    val isTentative: Boolean,
    internal val kparam: KParameter
) {
    val slashFriendlyName: String by lazy {
        return@lazy name.replace(SLASH_NAME_REGEX, "_$1").lowercase()
    }

    /**
     * Returns this argument as a [Pair]<[OptionType], [Boolean]>.
     * The [OptionType] represents the type of this argument.
     * The [Boolean] represents whether the argument is required. True if it is, false otherwise.
     */
    fun asSlashCommandType(): Pair<OptionType, Boolean> {
        val optionType = OPTION_TYPE_MAPPING[type]
            ?: throw IllegalStateException("Unable to find OptionType for type ${type.simpleName}")

        return Pair(optionType, !isNullable && !optional)
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

    companion object {
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
