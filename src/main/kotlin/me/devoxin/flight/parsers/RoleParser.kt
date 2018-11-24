package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import net.dv8tion.jda.core.entities.Role
import java.util.*

class RoleParser : Parser<Role> {

    override fun parse(ctx: Context, param: String): Optional<Role> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val role: Role? = if (snowflake.isPresent) {
            ctx.guild?.getRoleById(snowflake.get())
        } else {
            ctx.guild?.roleCache?.firstOrNull { it.name == param }
        }

        if (role != null) {
            return Optional.of(role)
        }

        return Optional.empty()
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}