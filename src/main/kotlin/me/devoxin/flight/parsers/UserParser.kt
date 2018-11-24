package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import net.dv8tion.jda.core.entities.User
import java.util.*

class UserParser : Parser<User> {

    override fun parse(ctx: Context, param: String): Optional<User> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val user: User?

        if (snowflake.isPresent) {
            user = ctx.jda.getUserById(snowflake.get())
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                user = ctx.jda.userCache.find { it.name == tag[0] && it.discriminator == tag[1] }
            } else {
                user = ctx.jda.userCache.find { it.name == param }
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