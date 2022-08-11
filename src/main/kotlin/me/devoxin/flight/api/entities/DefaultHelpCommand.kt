package me.devoxin.flight.api.entities

import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.context.MessageContext
import me.devoxin.flight.internal.utils.TextUtils

open class DefaultHelpCommand(private val showParameterTypes: Boolean) : Cog {

    override fun name() = "No Category"

    @Command(aliases = ["commands", "cmds"], description = "Displays bot help.")
    open suspend fun help(ctx: MessageContext, command: String?) {
        val pages = if (command == null) {
            buildCommandList(ctx)
        } else {
            val commands = ctx.commandClient.commands
            val cmd = commands.findCommandByName(command)
                ?: commands.findCommandByAlias(command)

            if (cmd != null) {
                buildCommandHelp(ctx, cmd)
            } else {
                val cog = commands.findCogByName(command)
                    ?: return ctx.send("No commands or cogs found with that name.")

                buildCogHelp(ctx, cog)
            }
        }

        sendPages(ctx, pages)
    }

    open fun buildCommandList(ctx: MessageContext): List<String> {
        val categories = hashMapOf<String, HashSet<CommandFunction>>()
        val helpMenu = StringBuilder()
        val commands = ctx.commandClient.commands.values.filter { !it.properties.hidden }
        val padLength = ctx.commandClient.commands.values.maxByOrNull { it.name.length }!!.name.length

        for (command in commands) {
            val category = command.category.lowercase()
            val list = categories.computeIfAbsent(category) { hashSetOf() }
            list.add(command)
        }

        for (entry in categories.entries.sortedBy { it.key }) {
            helpMenu.append(toTitleCase(entry.key)).append("\n")

            for (cmd in entry.value.sortedBy { it.name }) {
                val description = cmd.properties.description

                helpMenu.apply {
                    append("  ")
                    append(cmd.name.padEnd(padLength + 1, ' '))
                    append(" ")
                    append(truncate(description, 100))
                    append("\n")
                }
            }
        }

        return TextUtils.split(helpMenu.toString().trim(), 1990)
    }

    open fun buildCommandHelp(ctx: MessageContext, command: CommandFunction): List<String> {
        val builder = StringBuilder()

        val trigger = if (ctx.trigger.matches("<@!?${ctx.jda.selfUser.id}> ".toRegex())) "@${ctx.jda.selfUser.name} " else ctx.trigger
        builder.append(trigger)

        val properties = command.properties

        if (properties.aliases.isNotEmpty()) {
            builder.append("[")
                .append(command.name)
                .append(properties.aliases.joinToString("|", prefix = "|"))
                .append("] ")
        } else {
            builder.append(command.name).append(" ")
        }

        for (arg in command.arguments) {
            builder.append(arg.format(showParameterTypes)).append(" ")
        }

        builder.append("\n\n").append(properties.description)
        return listOf(builder.toString())
    }

    open fun buildCogHelp(ctx: MessageContext, cog: Cog): List<String> {
        val builder = StringBuilder("Commands in ${cog::class.simpleName}\n")
        val commands = ctx.commandClient.commands.findCommandsByCog(cog).filter { !it.properties.hidden }
        val padLength = ctx.commandClient.commands.values.maxByOrNull { it.name.length }!!.name.length

        for (command in commands) {
            builder.apply {
                append(" ")
                append(command.name.padEnd(padLength + 1, ' '))
                append(truncate(command.properties.description, 100))
                append("\n")
            }
        }

        return TextUtils.split(builder.toString(), 1990)
    }

    // TODO: Subcommand help

    open suspend fun sendPages(ctx: MessageContext, pages: Collection<String>) {
        for (page in pages) {
            ctx.sendAsync("```\n$page```")
        }
    }

    private fun toTitleCase(s: String) = s.split(" +".toRegex()).joinToString(" ", transform = TextUtils::capitalise)

    private fun truncate(s: String, maxLength: Int): String {
        return s.takeUnless { it.length > maxLength } ?: (s.take(maxLength - 3) + "...")
    }

}
