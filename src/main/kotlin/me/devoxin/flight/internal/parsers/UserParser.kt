package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.User
import java.util.*

class UserParser : Parser<User> {
    override fun parse(ctx: MessageContext, param: String): User? {
        val snowflake = snowflakeParser.parse(ctx, param)?.resolved

        val user = when {
            snowflake != null -> ctx.message.mentions.users.firstOrNull { it.idLong == snowflake } ?: ctx.jda.getUserById(snowflake)
            param.length > 5 && param[param.length - 5] == '#' -> {
                val tag = param.split("#")
                ctx.jda.userCache.find { (it.discriminator != "0000" && it.name == tag[0]) || it.asTag == param }
            }
            else -> ctx.jda.userCache.find { it.name == param }
        }

        return user
    }

    companion object {
        private val snowflakeParser = SnowflakeParser() // We can reuse this
    }
}
