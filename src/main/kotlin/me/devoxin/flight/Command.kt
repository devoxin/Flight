package me.devoxin.flight

interface Command {

    fun execute(ctx: Context)

    fun name(): String {
        return this.javaClass.simpleName.toLowerCase()
    }

}