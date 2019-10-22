package me.devoxin.flight.parsers.jda

import me.devoxin.flight.api.Context
import me.devoxin.flight.api.jda.JDAContext
import me.devoxin.flight.parsers.Parser
import me.devoxin.flight.parsers.SnowflakeParser
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import java.util.*

class JDAUserParser : Parser<User> {

    override fun parse(ctx: Context<*>, param: String): Optional<User> {
        require(ctx is JDAContext) { "Wrong context type" }
        val snowflake = snowflakeParser.parse(ctx, param)
        val user: User?

        if (snowflake.isPresent) {
            user = ctx.client.getUserById(snowflake.get())
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                user = ctx.client.userCache.find { it.name == tag[0] && it.discriminator == tag[1] }
            } else {
                user = ctx.client.userCache.find { it.name == param }
            }
        }

        if (user != null) {
            return Optional.of(user)
        }

        return Optional.empty()
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}
