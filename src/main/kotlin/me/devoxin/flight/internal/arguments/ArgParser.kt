package me.devoxin.flight.internal.arguments

import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.api.exceptions.ParserNotRegistered
import me.devoxin.flight.internal.parsers.Parser
import java.util.*
import kotlin.reflect.KParameter

class ArgParser(
    private val ctx: Context,
    commandArgs: List<String>,
    private val delimiter: Char
) {

    private var args = commandArgs.toList()

    private fun getArgs(amount: Int): List<String> {
        if (args.isEmpty()) {
            return emptyList()
        }

        val taken = args.take(amount)
        args = args.drop(amount) // I don't like re-assignment, so @todo figure out why .removeAt didn't work.

        /*
        for (i in 0 until amount) {
            args.removeAt(0)
        }
         */

        return taken
    }

    private fun parseNextArgument(consumeRest: Boolean = false): String {
        if (args.isEmpty()) {
            return ""
        } else {
            if (consumeRest) {
                return getArgs(args.size).joinToString(delimiter.toString()).trim()
            }
        }

        val isQuoted = args[0].startsWith('"') // Quotes! TODO: accept other forms of quote chars

        if (!isQuoted || delimiter != ' ') { // Don't handle quote arguments if a custom delimiter was specified.
            return getArgs(1).joinToString(delimiter.toString()).trim()
        }

        val iterator = args.joinToString(delimiter.toString()).iterator()
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
            } else if (!quoting && char == delimiter) { // char.isWhitespace
                // If we're not quoting and it's not whitespace we should throw
                // ex: !test  blah -- Extraneous whitespace. Currently we ignore this
                // (effectively "trimming" the argument) and just use the next part.
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
        args = remainingArgs.toString().split(delimiter).toMutableList()

        return argument.toString().trim()
    }

    fun parse(arg: Argument): Any? {
        val argument = parseNextArgument(arg.greedy)
        val parser = parsers[arg.type]
            ?: throw ParserNotRegistered("No parsers registered for `${arg.type}`")

        val result: Optional<out Any?>

        result = if (argument.isEmpty()) {
            Optional.empty()
        } else {
            try {
                parser.parse(ctx, argument)
            } catch (e: Exception) {
                throw BadArgument(arg, argument, e)
            }
        }

        if (!result.isPresent && !arg.isNullable && (!arg.optional || argument.isNotEmpty())) {
            throw BadArgument(arg, argument)
        }

        return result.orElse(null)
    }

    companion object {
        val parsers = hashMapOf<Class<*>, Parser<*>>()

        fun parseArguments(cmd: CommandFunction, ctx: Context, args: List<String>): HashMap<KParameter, Any?> {
            if (cmd.arguments.isEmpty()) {
                return hashMapOf()
            }

            val delimiter = cmd.properties.argDelimiter
            val commandArgs = if (delimiter == ' ') {
                args
            } else {
                args.joinToString(" ").split(delimiter).toMutableList()
            }

            val parser = ArgParser(ctx, commandArgs, delimiter)
            val resolvedArgs = hashMapOf<KParameter, Any?>()

            for (arg in cmd.arguments) {
                val res = parser.parse(arg)

                if (res != null || (arg.isNullable && !arg.optional)) {
                    //This will only place the argument into the map if the value is null,
                    // or if the parameter requires a value (i.e. marked nullable).
                    //Commands marked optional already have a parameter so they don't need user-provided values
                    // unless the argument was successfully resolved for that parameter.
                    resolvedArgs[arg.kparam] = res
                }
            }

            return resolvedArgs
        }
    }
}
