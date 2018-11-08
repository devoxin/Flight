package me.devoxin.flight

import me.devoxin.flight.utils.split

@CommandProperties(aliases = ["commands"], description = "Displays bot help")
@CommandArguments(Argument("command", ArgType.CleanString, false, false))
public class DefaultHelpCommand : AsyncCommand() {

    override suspend fun executeAsync(ctx: Context, args: Map<String, Any?>) {
        val cmd = args["command"] as String?

        if (cmd != null) {
            val commands = ctx.commandClient.commands
            val command = commands[cmd]
                    ?: commands.values.firstOrNull { it.commandProperties() != null && it.commandProperties()!!.aliases.contains(cmd) }
                    ?: return ctx.send("No commands matching `$cmd` found.")

            sendCommandHelp(ctx, command)
        } else {
            sendHelpMenu(ctx)
        }
    }

    private suspend fun sendHelpMenu(ctx: Context) {
        val categories = hashMapOf<String, HashSet<Command>>()
        val helpMenu = StringBuilder()

        for (command in ctx.commandClient.commands.values) {
            val category = command.commandProperties()?.category?.toLowerCase() ?: "no category"

            val list = categories.computeIfAbsent(category) {
                hashSetOf()
            }

            list.add(command)
        }

        for (entry in categories.entries.sortedBy { it.key }) {
            helpMenu.append(toTitleCase(entry.key)).append("\n")

            for (cmd in entry.value.sortedBy { it.name() }) {
                val description = cmd.commandProperties()?.description ?: "No description available"

                helpMenu.append("  ")
                        .append(cmd.name().padEnd(20, ' '))
                        .append(" ")
                        .append(truncate(description, 100))
                        .append("\n")
            }
        }

        val pages = split(helpMenu.toString().trim(), 1990)

        for (page in pages) {
            ctx.sendAsync("```\n$page```")
        }
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
            builder.append(" ")
        }

        builder.trim()

        val description = properties?.description ?: "No description available"

        builder.append("\n\n")
                .append(description)
                .append("```")

        ctx.send(builder.toString())
    }

    private fun toTitleCase(s: String): String {
        return s.split(" +".toRegex())
                .joinToString(" ") { it[0].toUpperCase() + it.substring(1).toLowerCase() }
    }

    private fun truncate(s: String, maxLength: Int): String {
        if (s.length > maxLength) {
            return s.substring(0, maxLength - 3) + "..."
        }

        return s
    }

    override fun name(): String {
        return "help"
    }

}