package me.devoxin.flight.parsers

import com.mewna.catnip.entity.user.User
import me.devoxin.flight.api.Context
import java.util.*

class UserParser : Parser<User> {

    override fun parse(ctx: Context, param: String): Optional<User> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val user: User?

        user = if (snowflake.isPresent) {
            ctx.catnip.cache().user(snowflake.get())
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                ctx.catnip.cache().users().firstOrNull { it.username() == tag[0] && it.discriminator() == tag[1] }
            } else {
                ctx.catnip.cache().users().firstOrNull { it.username() == param }
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
