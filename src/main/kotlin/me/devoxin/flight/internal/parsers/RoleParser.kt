package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.Role
import java.util.*

class RoleParser : Parser<Role> {
    override fun parse(ctx: MessageContext, param: String): Optional<Role> {
        val snowflake = snowflakeParser.parse(ctx, param).takeIf { it.isPresent }?.get()?.resolved

        return when {
            snowflake != null -> Optional.ofNullable(ctx.guild?.getRoleById(snowflake))
            else -> Optional.ofNullable(ctx.guild?.roleCache?.firstOrNull { it.name == param })
        }
    }

    companion object {
        private val snowflakeParser = SnowflakeParser() // We can reuse this
    }
}
