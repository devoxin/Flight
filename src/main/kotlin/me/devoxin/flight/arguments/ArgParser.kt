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



}