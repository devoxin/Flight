package me.devoxin.flight.api.annotations

/**
 * Marks an argument as tentative.
 * When parsing fails, parsing/execution will continue rather than throwing.
 * Additionally, the default value, or null, will be passed in place of the value.
 *
 * Arguments utilising this annotation should be marked nullable, or have a default specified.
 *
 * Example:
 * fun ban(ctx: Context, member: Member, @Tentative deleteDays: Int = 7, @Greedy reason: String)
 *
 * This usage style allows command invocations such as:
 * !ban user#0000 Violated rule 3.
 * !ban user#0000 0 Violated rule 3.
 *
 * This annotation is redundant for String parameters, due to the way argument parsing works.
 * It's for this reason, that such arguments should be placed after other types if possible.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Tentative
