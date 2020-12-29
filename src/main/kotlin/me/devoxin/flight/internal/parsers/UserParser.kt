package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.entities.User
import java.util.*

class UserParser : Parser<User> {

    // TODO: Check ctx.message.mentionedUsers
    override fun parse(ctx: Context, param: String): Optional<User> {
        val snowflake = snowflakeParser.parse(ctx, param)

        val user = if (snowflake.isPresent) {
            ctx.jda.getUserById(snowflake.get().resolved)
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                ctx.jda.userCache.find { it.name == tag[0] && it.discriminator == tag[1] }
            } else {
                ctx.jda.userCache.find { it.name == param }
            }
        }

        return Optional.ofNullable(user)
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}
