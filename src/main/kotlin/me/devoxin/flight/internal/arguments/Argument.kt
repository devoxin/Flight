package me.devoxin.flight.internal.arguments

import me.devoxin.flight.api.entities.Attachment
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType
import kotlin.reflect.KParameter

class Argument(
    val name: String,
    val description: String,
    val type: Class<*>,
    val greedy: Boolean,
    val optional: Boolean, // Denotes that a parameter has a default value.
    val isNullable: Boolean,
    val isTentative: Boolean,
    internal val kparam: KParameter
) {
    val slashFriendlyName: String
        get() {
            val match = SLASH_NAME_REGEX.matcher(name)
            return if (match.matches()) "${match.group(1)}_${match.group(2).lowercase()}" else name.lowercase()
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
        val SLASH_NAME_REGEX = "([a-z]+)([A-Z][a-z]+)".toPattern()

        val OPTION_TYPE_MAPPING = mapOf(
            String::class.java to OptionType.STRING,
            Integer::class.java to OptionType.INTEGER,
            Long::class.java to OptionType.INTEGER,
            Boolean::class.java to OptionType.BOOLEAN,
            Member::class.java to OptionType.USER,
            User::class.java to OptionType.USER,
            GuildChannel::class.java to OptionType.CHANNEL,
            TextChannel::class.java to OptionType.CHANNEL,
            VoiceChannel::class.java to OptionType.CHANNEL,
            Role::class.java to OptionType.ROLE,
            Double::class.java to OptionType.NUMBER,
            Attachment::class.java to OptionType.ATTACHMENT
        )
    }
}
