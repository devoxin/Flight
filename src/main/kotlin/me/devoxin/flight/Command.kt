package me.devoxin.flight

public interface Command {

    fun execute(ctx: Context, args: Map<String, Any?>)

    fun commandProperties(): CommandProperties? {
        return this.javaClass.getAnnotation(CommandProperties::class.java)
    }

    fun commandArguments(): List<Argument> {
        val annotation = this.javaClass.getAnnotation(CommandArguments::class.java)
                ?: return emptyList()

        return annotation.arguments.toList()
    }

    fun name(): String {
        return this.javaClass.simpleName.toLowerCase()
    }

}