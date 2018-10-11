package me.devoxin.flight

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import java.lang.IndexOutOfBoundsException
import java.util.regex.Pattern

class Arguments(
        private val command: Command,
        private val ctx: Context,
        private var args: List<String>
) {

    companion object {
        private val snowflakeMatch: Pattern = Pattern.compile("[0-9]{17,20}")
    }

    private fun getArgs(amount: Int): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        if (amount > args.size) {
            throw IndexOutOfBoundsException("No more args to retrieve!")
        }

        val elements = args.take(amount)
        args = args.subList(0, amount)

        return elements
    }

    private fun parseNextArgument(consumeRest: Boolean = false): String {
        if (args.isEmpty()) {
            return ""
        } else {
            if (consumeRest) {
                return getArgs(args.size).joinToString(" ")
            }
        }

        val isQuoted = args[0].startsWith("\"") // Quotes! TODO: accept other forms of quote chars

        if (!isQuoted) {
            return getArgs(1).joinToString(" ")
        }

        val iterator = args.joinToString(" ").iterator()
        val argument = StringBuilder()
        var quoting = false
        var escaping = false

        while (iterator.hasNext()) {
            val char = iterator.nextChar()

            if (escaping) {
                argument.append(char)
                escaping = false
            } else if (char == '\\') {
                escaping = true
            } else if (quoting && char == '"') { // TODO: accept other forms of quote chars
                quoting = false
            } else if (!quoting && char == '"') { // TODO: accept other forms of quote chars
                quoting = true
            } else if (!quoting && char.isWhitespace()) { // If we're not quoting and it's not whitespace we should throw
                if (argument.isEmpty()) {
                    continue
                } else {
                    break
                }
            } else {
                argument.append(char)
            }
        }

        val remainingArgs = StringBuilder()
        iterator.forEachRemaining { remainingArgs.append(it) }
        args = remainingArgs.toString().split(" +".toRegex()).toMutableList()

        return argument.toString()
    }

    private fun parseSnowflake(arg: String): String? {
        val match = snowflakeMatch.matcher(arg)
        return if (match.find()) match.group() else null
    }

    fun resolveTextChannelId(consumeRest: Boolean = false): String? {
        val target = parseNextArgument(consumeRest)
        return parseSnowflake(target) ?: ctx.guild?.textChannels?.firstOrNull { it.name == target }?.id
    }

    fun resolveTextChannel(consumeRest: Boolean = false): TextChannel? {
        val id = resolveTextChannelId(consumeRest) ?: return null
        return ctx.guild?.getTextChannelById(id)
    }

    fun resolveMemberId(consumeRest: Boolean = false): String? {
        val target = parseNextArgument(consumeRest)

        return parseSnowflake(target) ?: if (target.length > 5 && target[target.length - 5].toString() == "#") {
            val tag = target.split("#")
            ctx.guild?.members?.find { it.user.name == tag[0] && it.user.discriminator == tag[1] }?.user?.id
        } else {
            ctx.guild?.members?.find { it.user.name == target }?.user?.id
        }
    }

    fun resolveMember(consumeRest: Boolean = false): Member? {
        val id = resolveMemberId(consumeRest) ?: return null
        return ctx.guild?.getMemberById(id)
    }

    fun resolveRoleId(consumeRest: Boolean = false): String? {
        val target = parseNextArgument(consumeRest)
        return parseSnowflake(target) ?: ctx.guild?.roles?.firstOrNull { it.name == target }?.id
    }

    fun resolveRole(consumeRest: Boolean = false): Role? {
        val id = resolveRoleId(consumeRest) ?: return null
        return ctx.guild?.getRoleById(id)
    }

    fun resolveString(consumeRest: Boolean = false, cleanContent: Boolean = false): String {
        return parseNextArgument(consumeRest)
        // TODO cleanContent needs to do something
    }

}
