package me.devoxin.flight.internal.arguments

import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.api.exceptions.ParserNotRegistered
import me.devoxin.flight.internal.entities.Executable
import me.devoxin.flight.internal.parsers.Parser
import java.util.*
import kotlin.reflect.KParameter

class ArgParser(
    private val ctx: MessageContext,
    private val delimiter: Char,
    commandArgs: List<String>
) {
    private val delimiterStr = delimiter.toString()
    private var args = commandArgs.toMutableList()

    private fun take(amount: Int) = args.take(amount).onEach { args.removeAt(0) }
    private fun restore(argList: List<String>) = args.addAll(0, argList)

    private fun parseQuoted(): Pair<String, List<String>> {
        val iterator = args.joinToString(delimiterStr).iterator()
        val original = StringBuilder()
        val argument = StringBuilder("\"")
        var quoting = false
        var escaping = false

        loop@ while (iterator.hasNext()) {
            val char = iterator.nextChar()
            original.append(char)

            when {
                escaping -> {
                    argument.append(char)
                    escaping = false
                }
                char == '\\' -> escaping = true
                quoting && char == '"' -> quoting = false // accept other quote chars
                !quoting && char == '"' -> quoting = true // accept other quote chars
                !quoting && char == delimiter -> {
                    // Maybe this should throw? !test  blah -- Extraneous whitespace is ignored.
                    if (argument.isEmpty()) continue@loop
                    else break@loop
                }
                else -> argument.append(char)
            }
        }

        argument.append('"')

        val remainingArgs = StringBuilder().apply {
            iterator.forEachRemaining { this.append(it) }
        }
        args = remainingArgs.toString().split(delimiter).toMutableList()
        return Pair(argument.toString(), original.split(delimiterStr))
    }

    /**
     * @returns a Pair of the parsed argument, and the original args.
     */
    private fun getNextArgument(greedy: Boolean): Pair<String, List<String>> {
        val (argument, original) = when {
            args.isEmpty() -> Pair("", emptyList())
            greedy -> {
                val args = take(args.size)
                Pair(args.joinToString(delimiterStr), args)
            }
            args[0].startsWith('"') && delimiter == ' ' -> parseQuoted() // accept other quote chars
            else -> {
                val taken = take(1)
                Pair(taken.joinToString(delimiterStr), taken)
            }
        }

        var unquoted = argument.trim()

        if (!greedy) {
            unquoted = unquoted.removeSurrounding("\"")
        }

        return Pair(unquoted, original)
    }

    fun parse(arg: Argument): Any? {
        val parser = parsers[arg.type]
            ?: throw ParserNotRegistered("No parsers registered for `${arg.type}`")
        val (argument, original) = getNextArgument(arg.greedy)
        val result = if (argument.isEmpty()) {
            Optional.empty()
        } else {
            try {
                parser.parse(ctx, argument)
            } catch (e: Throwable) {
                throw BadArgument(arg, argument, e)
            }
        }

        val canSubstitute = arg.isTentative || arg.isNullable || (arg.optional && argument.isEmpty())
        val (rangeCheck, rangeMessage) = checkRange(arg, result)

        if (!result.isPresent || !rangeCheck) {
            if (!canSubstitute) { // canSubstitute -> Whether we can pass null or the default value.
                val cause = if (result.isPresent && rangeCheck) null else IllegalArgumentException(rangeMessage)
                // This should throw if the result is not present, and one of the following is not true:
                // - The arg is marked tentative (isTentative)
                // - The arg can use null (isNullable)
                // - The arg has a default (isOptional) and no value was specified for it (argument.isEmpty())

                //!arg.isNullable && (!arg.optional || argument.isNotEmpty())) {
                throw BadArgument(arg, argument, cause)
            }

            if (arg.isTentative) {
                restore(original)
            }
        }

        return result.takeIf { rangeCheck }?.orElse(null)
    }

    private fun checkRange(arg: Argument, parsed: Optional<out Any?>): Pair<Boolean, String?> {
        arg.range ?: return true to null
        val num = parsed.orElse(null) as? Number ?: return false to null

        val double = arg.range.double
        val long = arg.range.long

        if (double.isNotEmpty()) {
            val dbl = num.toDouble()
            return when (double.size) {
                1 -> (dbl >= double[0]) to "`${arg.name}` must be at least ${double[0]} or bigger"
                2 -> (dbl >= double[0] && dbl <= double[1]) to "`${arg.name}` be within range ${double.joinToString("-")}"
                else -> false to "<Invalid range for `${arg.name}:double`>"
            }
        } else if (long.isNotEmpty()) {
            val lng = num.toLong()
            return when (long.size) {
                1 -> (lng >= long[0]) to "`${arg.name}` must be at least ${long[0]} or bigger"
                2 -> (lng >= long[0] && lng <= long[1]) to "`${arg.name}` be within range ${long.joinToString("-")}"
                else -> false to "<Invalid range for `${arg.name}:long`>"
            }
        }

        return true to null
    }

    companion object {
        val parsers = hashMapOf<Class<*>, Parser<*>>()

        fun parseArguments(cmd: Executable, ctx: MessageContext, args: List<String>, delimiter: Char): HashMap<KParameter, Any?> {
            if (cmd.arguments.isEmpty()) {
                return hashMapOf()
            }

            val commandArgs = if (delimiter == ' ') args else args.joinToString(" ").split(delimiter).toMutableList()
            val parser = ArgParser(ctx, delimiter, commandArgs)
            val resolvedArgs = hashMapOf<KParameter, Any?>()

            for (arg in cmd.arguments) {
                val res = parser.parse(arg)
                val useValue = res != null || (arg.isNullable && !arg.optional) || (arg.isTentative && arg.isNullable)

                if (useValue) {
                    //This will only place the argument into the map if the value is null,
                    // or if the parameter requires a value (i.e. marked nullable).
                    //Commands marked optional already have a parameter, so they don't need user-provided values
                    // unless the argument was successfully resolved for that parameter.
                    resolvedArgs[arg.kparam] = res
                }
            }

            return resolvedArgs
        }
    }
}
