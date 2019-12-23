package me.devoxin.flight.internal

import me.devoxin.flight.annotations.Command
import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.api.Context
import me.devoxin.flight.arguments.Name
import me.devoxin.flight.models.Cog
import me.devoxin.flight.utils.TextSplitter

class DefaultHelpCommand(private val showParameterTypes: Boolean) : Cog {

    override fun name(): String = "No Category"

    @Command(aliases = ["commands", "cmds"], description = "Displays bot help.")
    suspend fun help(ctx: Context, @Name("command") command: String?) {
        if (command == null) {
            sendHelpMenu(ctx)
            return
        }

        val commands = ctx.commandClient.commands
        val cmd = commands[command]
            ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }

        if (cmd == null) {
            ctx.send("No commands matching `${ctx.cleanContent(command)}` found.")
            return
        }

        sendCommandHelp(ctx, cmd)
    }

    private suspend fun sendHelpMenu(ctx: Context) {
        val categories = hashMapOf<String, HashSet<CommandWrapper>>()
        val helpMenu = StringBuilder()

        for (command in ctx.commandClient.commands.values) {
            val category = command.category.toLowerCase()

            val list = categories.computeIfAbsent(category) {
                hashSetOf()
            }

            list.add(command)
        }

        for (entry in categories.entries.sortedBy { it.key }) {
            helpMenu.append(toTitleCase(entry.key)).append("\n")

            for (cmd in entry.value.sortedBy { it.name }) {
                val description = cmd.properties.description

                helpMenu.append("  ")
                        .append(cmd.name.padEnd(15, ' '))
                        .append(" ")
                        .append(truncate(description, 100))
                        .append("\n")
            }
        }

        val pages = TextSplitter.split(helpMenu.toString().trim(), 1990)

        for (page in pages) {
            ctx.sendAsync("```\n$page```")
        }
    }

    private fun sendCommandHelp(ctx: Context, command: CommandWrapper) {
        val builder = StringBuilder("```\n")

        if (ctx.trigger.matches("<@!?${ctx.jda.selfUser.id}> ".toRegex())) {
            builder.append("@${ctx.jda.selfUser.name}")
        } else {
            builder.append(ctx.trigger)
        }

        val properties = command.properties

        if (properties.aliases.isNotEmpty()) {
            builder.append("[")
                    .append(command.name)
                    .append(properties.aliases.joinToString("|", prefix = "|"))
                    .append("] ")
        } else {
            builder.append(command.name)
                    .append(" ")
        }

        val args = command.arguments

        for (arg in args) {
            if (!arg.optional) {
                builder.append("<")
            } else {
                builder.append("[")
            }

            builder.append(arg.name)

            if (showParameterTypes) {
                builder.append(": ")
                    .append(arg.type.simpleName)
            }

            if (!arg.optional) {
                builder.append(">")
            } else {
                builder.append("]")
            }
            builder.append(" ")
        }

        builder.trim()

        val description = properties.description

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

}