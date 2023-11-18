package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.Member
import java.util.*

class MemberParser : Parser<Member> {
    override fun parse(ctx: MessageContext, param: String): Member? {
        val snowflake = SnowflakeParser.INSTANCE.parse(ctx, param)?.resolved

        val member = when {
            snowflake != null -> ctx.message.mentions.members.firstOrNull { it.user.idLong == snowflake } ?: ctx.guild?.getMemberById(snowflake)
            param.length > 5 && param[param.length - 5] == '#' -> {
                val tag = param.split("#")
                ctx.guild?.memberCache?.find { (it.user.discriminator != "0000" && it.user.name == tag[0]) || it.user.asTag == param }
            }
            else -> ctx.guild?.memberCache?.firstOrNull { it.user.name == param }
        }

        return member
    }
}
