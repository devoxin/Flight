package me.devoxin.flight.internal.parsers

import me.devoxin.flight.api.context.MessageContext
import net.dv8tion.jda.api.entities.Member
import java.util.*

class MemberParser : Parser<Member> {
    override fun parse(ctx: MessageContext, param: String): Optional<Member> {
        val snowflake = snowflakeParser.parse(ctx, param).takeIf { it.isPresent }?.get()?.resolved

        val member = when {
            snowflake != null -> ctx.message.mentions.members.firstOrNull { it.user.idLong == snowflake } ?: ctx.guild?.getMemberById(snowflake)
            else -> {
                if (param.length > 5 && param[param.length - 5] == '#') {
                    val tag = param.split("#")
                    ctx.guild?.memberCache?.find { it.user.name == tag[0] && it.user.discriminator == tag[1] }
                } else {
                    ctx.guild?.getMembersByName(param, false)?.firstOrNull()
                }
            }
        }

        return Optional.ofNullable(member)
    }

    companion object {
        private val snowflakeParser = SnowflakeParser() // We can reuse this
    }
}
