package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.User
import java.util.*

class UserParser : Parser<User> {
    override fun parse(ctx: MessageContext, param: String): Optional<User> {
        val snowflake = snowflakeParser.parse(ctx, param).takeIf { it.isPresent }?.get()?.resolved

        val user = when {
            snowflake != null -> ctx.message.mentions.users.firstOrNull { it.idLong == snowflake } ?: ctx.jda.getUserById(snowflake)
            else -> {
                if (param.length > 5 && param[param.length - 5] == '#') {
                    val tag = param.split("#")
                    ctx.jda.userCache.find { it.name == tag[0] && it.discriminator == tag[1] }
                } else {
                    ctx.jda.userCache.find { it.name == param }
                }
            }
        }

        return Optional.ofNullable(user)
    }

    companion object {
        private val snowflakeParser = SnowflakeParser() // We can reuse this
    }
}
