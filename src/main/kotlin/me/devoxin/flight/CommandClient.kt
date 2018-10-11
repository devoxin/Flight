package me.devoxin.flight

import com.google.common.reflect.ClassPath
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.lang.reflect.Modifier

@Suppress("UnstableApiUsage")
class CommandClient(
        private val prefixProvider: PrefixProvider,
        private val useDefaultHelpCommand: Boolean,
        private val ignoreBots: Boolean
) : ListenerAdapter() {

    private val commands = hashMapOf<String, Command>()

    // +------------------+
    // | Custom Functions |
    // +------------------+

    public fun registerCommands(packageName: String) {
        val classes = ClassPath.from(this.javaClass.classLoader).getTopLevelClassesRecursive(packageName)

        for (clazz in classes) {
            System.out.println("Loading ${clazz.name}")
            val klass = clazz.load()

            if (Modifier.isAbstract(klass.modifiers) || klass.isInterface) {
                continue
            }

            val command = klass.getDeclaredConstructor().newInstance() as Command
            this.commands[command.name()] = command
        }
    }

    public fun registerCommands(vararg commands: Command) {
        commands.forEach { this.commands[it.name()] = it }
    }


    // +------------------+
    // |  Event Handling  |
    // +------------------+

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (ignoreBots && (event.author.isBot || event.author.isFake)) {
            return
        }

        println("hi")

        val prefixes = prefixProvider.provide(event.message)
        println(prefixes)
        val trigger = prefixes.firstOrNull { event.message.contentRaw.startsWith(it) }
                ?: return

        println("triggered lol")

        if (trigger.length == event.message.contentRaw.length) {
            return
        }

        println("hoi")

        val args = event.message.contentRaw.substring(0, trigger.length).split(" +".toRegex()).toMutableList()
        println(args)
        val command = args.removeAt(0)
        println(command)

        if (!commands.containsKey(command)) {
            return
        }

        commands[command]!!.execute(Context(event, trigger))
    }
}
