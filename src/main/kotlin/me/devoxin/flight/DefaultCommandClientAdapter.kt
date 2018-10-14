package me.devoxin.flight

public abstract class DefaultCommandClientAdapter : CommandClientAdapter {

    override fun onBadArgument(ctx: Context, error: BadArgument) {
    }

    override fun onCommandError(ctx: Context, error: CommandError) {
    }

    override fun onCommandPostInvoke(ctx: Context, command: Command) {
    }

    override fun onCommandPreInvoke(ctx: Context, command: Command) = true

    override fun onParseError(ctx: Context, error: Throwable) {
    }

}
