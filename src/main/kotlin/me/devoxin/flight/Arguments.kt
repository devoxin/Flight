package me.devoxin.flight

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import java.util.regex.Pattern

class Arguments(
        private val ctx: Context,
        private var args: MutableList<String>
) {

    companion object {
        private val snowflakeMatch: Pattern = Pattern.compile("[0-9]{17,20}")
    }

    private fun getArgs(amount: Int): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        val elements = args.take(amount)

        for (i in 0 until amount) {
            args.removeAt(0)
        }

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

    fun resolveTextChannelId(arg: String): String? {
        return parseSnowflake(arg) ?: ctx.guild?.textChannels?.firstOrNull { it.name == arg }?.id
    }

    fun resolveTextChannel(arg: String): TextChannel? {
        val id = resolveTextChannelId(arg) ?: return null
        return ctx.guild?.getTextChannelById(id)
    }

    fun resolveMemberId(arg: String): String? {
        return parseSnowflake(arg) ?: if (arg.length > 5 && arg[arg.length - 5].toString() == "#") {
            val tag = arg.split("#")
            ctx.guild?.members?.find { it.user.name == tag[0] && it.user.discriminator == tag[1] }?.user?.id
        } else {
            ctx.guild?.members?.find { it.user.name == arg }?.user?.id
        }
    }

    fun resolveMember(arg: String): Member? {
        val id = resolveMemberId(arg) ?: return null
        return ctx.guild?.getMemberById(id)
    }

    fun resolveRoleId(arg: String): String? {
        return parseSnowflake(arg) ?: ctx.guild?.roles?.firstOrNull { it.name == arg }?.id
    }

    fun resolveRole(arg: String): Role? {
        val id = resolveRoleId(arg) ?: return null
        return ctx.guild?.getRoleById(id)
    }

    fun resolveString(arg: String, cleanContent: Boolean = false): String? {
        return if (arg.isEmpty() || arg.isBlank()) {
            null
        } else {
            arg
        }
        // TODO cleanContent needs to do something
    }

    fun parse(arg: Argument): Any? {
        val argument = parseNextArgument(arg.greedy)

        val result = when (arg.type) {
            ArgType.Member -> resolveMember(argument)
            ArgType.MemberId -> resolveMemberId(argument)
            ArgType.Role -> resolveRole(argument)
            ArgType.RoleId -> resolveRoleId(argument)
            ArgType.String -> resolveString(argument)
            ArgType.TextChannel -> resolveTextChannel(argument)
            ArgType.TextChannelId -> resolveTextChannelId(argument)
        }

        if (result == null && arg.required) {
            throw BadArgument(arg.name, arg.type, argument)
        }

        return result
    }

}

public enum class ArgType {
    Member,
    MemberId,
    Role,
    RoleId,
    String,
    TextChannel,
    TextChannelId
}
