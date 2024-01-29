package me.devoxin.flight.internal.arguments

import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.api.exceptions.BadArgument
import me.devoxin.flight.api.exceptions.ParserNotRegistered
import me.devoxin.flight.internal.entities.Executable
import me.devoxin.flight.internal.parsers.Parser
import me.devoxin.flight.internal.utils.TextUtils
import me.devoxin.flight.internal.utils.Tuple
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

        while (iterator.hasNext()) {
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
                    if (argument.isEmpty()) continue
                    else break
                }
                else -> argument.append(char)
            }
        }

        argument.append('"')

        val remainingArgs = StringBuilder().apply {
            iterator.forEachRemaining(this::append)
        }

        args = remainingArgs.toString().split(delimiter).toMutableList()
        return argument.toString() to original.split(delimiterStr)
    }

    /**
     * @returns a Pair of the parsed argument, and the original args.
     */
    private fun getNextArgument(greedy: Boolean): Pair<String, List<String>> {
        val (argument, original) = when {
            args.isEmpty() -> "" to emptyList()
            greedy -> {
                val args = take(args.size)
                args.joinToString(delimiterStr) to args
            }
            args[0].startsWith('"') && delimiter == ' ' -> parseQuoted() // accept other quote chars
            else -> {
                val taken = take(1)
                taken.joinToString(delimiterStr) to taken
            }
        }

        var unquoted = argument.trim()

        if (!greedy) {
            unquoted = unquoted.removeSurrounding("\"")
        }

        return unquoted to original
    }

    fun parse(arg: Argument): Any? {
        val parser = parsers[arg.type]
            ?: throw ParserNotRegistered("No parsers registered for `${arg.type}`")

        val (argument, original) = getNextArgument(arg.greedy)
        val (choiceCheck, choiceResolved, choiceMessage) = checkChoices(arg, argument)

        var result: Any? = null

        if (choiceResolved != null) {
            // use the resolved value if available
            result = choiceResolved
        } else if (choiceMessage == null) {
            // otherwise try and parse into the required type
            // this mustn't try and parse if choiceMessage is not null as it indicates
            // this argument has choices, so we should try and use those choices first
            result = argument.takeIf { it.isNotEmpty() }?.let {
                try {
                    parser.parse(ctx, argument)
                } catch (e: Throwable) {
                    throw BadArgument(arg, argument, e)
                }
            }
        }

        val canSubstitute = arg.isTentative || arg.isNullable || (arg.optional && argument.isEmpty())
        val (rangeCheck, rangeMessage) = checkRange(arg, result)

        if (result == null || !rangeCheck || !choiceCheck) {
            if (!canSubstitute) { // canSubstitute -> Whether we can pass null or the default value.
                val cause = (rangeMessage ?: choiceMessage)?.let(::IllegalArgumentException)
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

        return (choiceResolved ?: result).takeIf { rangeCheck && choiceCheck }
    }

    private fun <T : Any?> checkRange(arg: Argument, res: T): Pair<Boolean, String?> {
        arg.range ?: return true to null

        if (res !is Number && res !is String) {
            return false to null
        }

        val double = arg.range.double
        val long = arg.range.long
        val string = arg.range.string

        @Suppress("KotlinConstantConditions")
        when {
            res is Number -> when {
                long.isNotEmpty() -> {
                    val n = res.toLong()

                    return when (long.size) {
                        1 -> (n >= long[0]) to "`${arg.name}` must be at least ${long[0]} or bigger"
                        2 -> (n >= long[0] && n <= long[1]) to "`${arg.name}` must be within range ${long.joinToString("-")}"
                        else -> false to "Invalid long range for `${arg.name}`"
                    }
                }
                double.isNotEmpty() -> {
                    val n = res.toDouble()

                    return when (double.size) {
                        1 -> (n >= double[0]) to "`${arg.name}` must be at least ${double[0]} or bigger."
                        2 -> (n >= double[0] && n <= double[1]) to "`${arg.name}` must be within range ${double.joinToString("-")}"
                        else -> false to "Invalid double range for `${arg.name}`"
                    }
                }
            }
            res is String && string.isNotEmpty() -> {
                val n = res.length

                return when (string.size) {
                    1 -> (n >= string[0]) to "`${arg.name}` must be at least ${string[0]} character${TextUtils.plural(string[0])} or longer."
                    2 -> (n >= string[0] && n <= string[1]) to "`${arg.name}` must be within the range of ${string.joinToString("-")} characters."
                    else -> false to "Invalid string range for `${arg.name}`"
                }
            }
        }

        return true to null
    }

    /**
     * Checks whether provided [res] is a valid choice.
     *
     * This will return any of the following:
     * [true, null, null] - if there are no choices for this argument.
     * [true, Any, null] - if the choice was resolved. Any will be the choice value.
     * [false, null, null] - if there are choices, but [res] is not an applicable type.
     * [false, null, String] - if there are choices, but [res] matched none of them. String will be an error message.
     */
    private fun checkChoices(arg: Argument, res: String?): Tuple<Boolean, Any?, String?> {
        arg.choices ?: return Tuple(true, null, null)

        if (res !is String) {
            return Tuple(false, null, null)
        }

        val double = arg.choices.double
        val long = arg.choices.long
        val string = arg.choices.string

        val (resolved, error) = when {
            long.isNotEmpty() -> long.find { it.key == res }?.let { it.value to null }
                ?: (null to long.joinToString("`, `", prefix = "`", postfix = "`"))
            double.isNotEmpty() -> double.find { it.key == res }?.let { it.value to null }
                ?: (null to double.joinToString("`, `", prefix = "`", postfix = "`"))
            string.isNotEmpty() -> string.find { it.key == res }?.let { it.value to null }
                ?: (null to string.joinToString("`, `", prefix = "`", postfix = "`"))
            else -> null to null
        }

        if (error != null) {
            return Tuple(false, null, "Invalid choice for `${arg.name}`.\nValid choices are: $error")
        }

        if (resolved != null) {
            return Tuple(true, resolved, null)
        }

        return Tuple(true, null, null)
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
                    resolvedArgs[arg.parameter] = res
                }
            }

            return resolvedArgs
        }
    }
}
