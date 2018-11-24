package me.devoxin.flight.arguments

import me.devoxin.flight.BadArgument
import me.devoxin.flight.Context
import me.devoxin.flight.parsers.Parser
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class Arguments(
        private val parsers: HashMap<Class<*>, Parser<*>>,
        private val ctx: Context,
        private var args: MutableList<String>) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

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

    fun parse(arg: Argument): Any? {
        val argument = parseNextArgument(arg.greedy)

        if (!parsers.containsKey(arg.type)) {
            logger.error("No parsers registered for `${arg.type}`")
            return null
        }

        val result = parsers[arg.type]!!.parse(ctx, argument)

        if (!result.isPresent && arg.required) {
            throw BadArgument(arg.name, arg.type, argument)
        }

        return result.get()
    }

    companion object {
        private val snowflakeMatch: Pattern = Pattern.compile("[0-9]{17,20}")
    }

}
