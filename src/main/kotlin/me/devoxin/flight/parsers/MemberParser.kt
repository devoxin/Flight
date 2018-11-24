package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import net.dv8tion.jda.core.entities.Member
import java.util.*

class MemberParser : Parser<Member> {

    override fun parse(ctx: Context, param: String): Optional<Member> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val member: Member?

        if (snowflake.isPresent) {
            member = ctx.guild?.getMemberById(snowflake.get())
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                member = ctx.guild?.memberCache?.find { it.user.name == tag[0] && it.user.discriminator == tag[1] }
            } else {
                member = ctx.guild?.memberCache?.find { it.user.name == param }
            }
        }

        if (member != null) {
            return Optional.of(member)
        }

        return Optional.empty()
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}