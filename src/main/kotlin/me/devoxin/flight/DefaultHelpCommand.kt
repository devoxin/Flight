package me.devoxin.flight

@CommandProperties(aliases = ["commands"], description = "Displays bot help")
@CommandArguments(Argument("command", ArgType.CleanString, false, false))
public class DefaultHelpCommand : Command {

    override fun execute(ctx: Context, args: Map<String, Any?>) {
        val cmd = args["command"] as String?

        if (cmd != null) {
            val commands = ctx.commandClient.commands
            val command = commands[cmd]
                    ?: commands.values.firstOrNull { it.commandProperties() != null && it.commandProperties()!!.aliases.contains(cmd) }
                    ?: return ctx.send("No commands matching `$cmd` found.")

            sendCommandHelp(ctx, command)
        } else {
            buildHelpMenu(ctx)
        }
    }

    private fun buildHelpMenu(ctx: Context) {
        val helpMenu = StringBuilder()

        for (command in ctx.commandClient.commands.values) {

        }
    }

    private fun sendChunk(ctx: Context, chunk: String, success: () -> Unit, failure: () -> Unit) {

    }

    private fun sendCommandHelp(ctx: Context, command: Command) {
        val builder = StringBuilder("```\n")

        builder.append(ctx.trigger) // todo: resolve mention prefixes as @Username

        val properties = command.commandProperties()

        if (properties != null && properties.aliases.isNotEmpty()) {
            builder.append("[")
                    .append(command.name())
                    .append(properties.aliases.joinToString("|", prefix = "|"))
                    .append("] ")
        } else {
            builder.append(command.name())
                    .append(" ")
        }

        val args = command.commandArguments()

        for (arg in args) {
            if (arg.required) {
                builder.append("<")
                        .append(arg.name)
                        .append(">")
            } else {
                builder.append("[")
                        .append(arg.name)
                        .append("]")
            }
        }

        val description = properties?.description ?: "No description available"

        builder.append("\n\n")
                .append(description)
                .append("```")

        ctx.send(builder.toString())
    }

}