package me.devoxin.flight.internal

import com.mewna.catnip.Catnip
import me.devoxin.flight.annotations.Async
import me.devoxin.flight.annotations.Command
import me.devoxin.flight.api.CommandWrapper
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.jda.JDAContext
import me.devoxin.flight.arguments.Optional
import me.devoxin.flight.models.Cog
import me.devoxin.flight.utils.TextSplitter
import net.dv8tion.jda.api.JDA

class DefaultHelpCommand(private val showParameterTypes: Boolean) : Cog {

    override fun name(): String = "No Category"

    @Async
    @Command(aliases = ["commands", "cmds"], description = "Displays bot help.")
    suspend fun help(ctx: Context<*>, @Optional command: String?) {
        if (command != null) {
            val commands = ctx.commandClient.commands
            val cmd = commands[command]
                    ?: commands.values.firstOrNull { it.properties.aliases.contains(command) }
                    ?: return ctx.send("No commands matching `$command` found.")

            sendCommandHelp(ctx, cmd)
        } else {
            sendHelpMenu(ctx)
        }
    }

    private suspend fun sendHelpMenu(ctx: Context<*>) {
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

    private fun sendCommandHelp(ctx: Context<*>, command: CommandWrapper) {
        val builder = StringBuilder("```\n")
        var selfUserId = ""
        var selfUserName = ""
        if (ctx is JDAContext) {
            selfUserId = ctx.client.selfUser.id
            selfUserName = ctx.client.selfUser.name
        } /* else if (ctx is CatnipContext) {
            selfUserId = (ctx.client as Catnip).selfUser()!!.id()
            selfUserName = (ctx.client as Catnip).selfUser()!!.username()
        } */
        if (ctx.trigger.matches("<@!?${selfUserId}> ".toRegex())) {
            builder.append("@${selfUserName} ")
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
            if (arg.required) {
                builder.append("<")
            } else {
                builder.append("[")
            }

            builder.append(arg.name)

            if (showParameterTypes) {
                builder.append(": ")
                    .append(arg.type.simpleName)
            }

            if (arg.required) {
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
