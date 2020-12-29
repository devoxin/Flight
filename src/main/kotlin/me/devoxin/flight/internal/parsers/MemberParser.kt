package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.Context
import net.dv8tion.jda.api.entities.Member
import java.util.*

class MemberParser : Parser<Member> {

    // TODO: Check ctx.message.mentionedMembers
    override fun parse(ctx: Context, param: String): Optional<Member> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val member: Member?

        member = if (snowflake.isPresent) {
            ctx.guild?.getMemberById(snowflake.get().resolved)
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                ctx.guild?.memberCache?.find { it.user.name == tag[0] && it.user.discriminator == tag[1] }
            } else {
                ctx.guild?.getMembersByName(param, false)?.firstOrNull()
            }
        }

        return Optional.ofNullable(member)
    }

    companion object {
        val snowflakeParser = SnowflakeParser() // We can reuse this
    }

}
