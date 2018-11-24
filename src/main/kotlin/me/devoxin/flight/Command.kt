package me.devoxin.flight

import me.devoxin.flight.arguments.Argument
import me.devoxin.flight.arguments.Greedy
import me.devoxin.flight.arguments.Optional
import java.lang.reflect.Method

public interface Command {

    /**
     * Invoked when an error occurs during command execution.
     * This is local to this command, allowing for per-command error handling.
     *
     * @return Whether the error was handled or not. If it wasn't,
     *         the error will be passed back to the registered
     *         CommandClientAdapter for handling.
     */
    fun onCommandError(ctx: Context, error: CommandError): Boolean = false

    /**
     * The properties for this command, if specified.
     *
     * @return Possibly-null CommandProperties.
     */
    fun commandProperties(): CommandProperties? {
        return this.javaClass.getAnnotation(CommandProperties::class.java)
    }

    /**
     * The arguments for this command, if specified.
     *
     * @return A list of command arguments.
     */
    fun commandArguments(): List<Argument> {
        val method = this.getExecutionMethod() ?: return emptyList()
        val arguments = mutableListOf<Argument>()

        for (p in method.parameters) {
            if (p.type == Context::class.java) {
                continue
            }

            val name = p.name
            val type = p.type
            val greedy = p.isAnnotationPresent(Greedy::class.java)
            val required = !p.isAnnotationPresent(Optional::class.java)

            arguments.add(
                    Argument(name, type, greedy, required)
            )
        }

        return arguments
    }

    /**
     * The name of this command.
     *
     * @return The name of the command.
     */
    fun name(): String {
        return this.javaClass.simpleName.toLowerCase()
    }

    fun getExecutionMethod(): Method? { // Reflection, sue me
        return this.javaClass.methods.firstOrNull { it.name == "execute" }
    }

}