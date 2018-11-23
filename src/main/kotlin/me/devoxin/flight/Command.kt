package me.devoxin.flight

public interface Command {

    /**
     * Invoked when a user calls the command.
     */
    fun execute(ctx: Context, args: Map<String, Any?>)

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
        val annotation = this.javaClass.getAnnotation(CommandArguments::class.java)
                ?: return emptyList()

        return annotation.arguments.toList()
    }

    /**
     * The name of this command.
     *
     * @return The name of the command.
     */
    fun name(): String {
        return this.javaClass.simpleName.toLowerCase()
    }

}