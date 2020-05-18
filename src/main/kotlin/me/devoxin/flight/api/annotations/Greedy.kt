package me.devoxin.flight.api.annotations

/**
 * Marks an argument as greedy.
 * By default, arguments are split by spaces, and are consumed on a single-word basis.
 * If quotations are present, then the content within the arguments is parsed and used instead.
 *
 * This annotation tells the parser to consume all remaining arguments. This can be useful in situations
 * where you want to parse members, or a welcome message (for example) and don't want users to have to quote arguments.
 *
 * For example:
 * fun welcomemessage(ctx: Context, message: String)
 *
 * "!welcomemessage hello there"
 * The content of "message" will be "hello".
 *
 * fun welcomemessage(ctx: Context, @Greedy message: String)
 *
 * "!welcomemessage hello there"
 * The content of "message" will be "hello there".
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Greedy
