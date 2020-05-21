package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.entities.Role
import java.util.*

class RoleParser : Parser<Role> {

    override fun parse(ctx: Context, param: String): Optional<Role> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val role: Role? = if (snowflake.isPresent) {
            ctx.guild?.getRoleById(snowflake.get().resolved)
        } else {
            ctx.guild?.roleCache?.firstOrNull { it.name == param }
        }

        return Optional.ofNullable(role)
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}
