package me.devoxin.flight.parsers.jda

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.jda.JDAContext
import me.devoxin.flight.parsers.Parser
import me.devoxin.flight.parsers.SnowflakeParser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Role
import java.util.*

class JDARoleParser : Parser<Role> {

    override fun parse(ctx: Context<*>, param: String): Optional<Role> {
        require(ctx is JDAContext) { "Wrong context type" }
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
