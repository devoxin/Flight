package me.devoxin.flight.parsers

import com.mewna.catnip.entity.guild.Member
import me.devoxin.flight.Context
import java.util.*

class MemberParser : Parser<Member> {

    override fun parse(ctx: Context, param: String): Optional<Member> {
        val snowflake = snowflakeParser.parse(ctx, param)
        val member: Member?

        member = if (snowflake.isPresent) {
            ctx.guild?.member(snowflake.get())
        } else {
            if (param.length > 5 && param[param.length - 5].toString() == "#") {
                val tag = param.split("#")
                ctx.guild?.members()?.firstOrNull { it.user().username() == tag[0] && it.user().discriminator() == tag[1] }
            } else {
                ctx.guild?.members()?.firstOrNull { it.effectiveName().contains(param, true) }
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
