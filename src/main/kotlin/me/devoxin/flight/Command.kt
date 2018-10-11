package me.devoxin.flight

public interface Command {

    fun execute(ctx: Context, args: Map<String, Any?>)

    fun commandProperties(): CommandProperties? {
        return this.javaClass.getAnnotation(CommandProperties::class.java)
    }

    fun commandArguments(): List<CommandArgument> {
        return this.javaClass.getAnnotationsByType(CommandArgument::class.java).toList()
    }

    fun name(): String {
        return this.javaClass.simpleName.toLowerCase()
    }

}