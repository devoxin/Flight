package me.devoxin.flight.models

import com.mewna.catnip.Catnip
import com.mewna.catnip.entity.guild.Invite

class Invite(private val catnip: Catnip, val url: String, val code: String) {

    fun resolve(success: (Invite) -> Unit, failure: (Throwable) -> Unit) {
        catnip.rest().invite().getInvite(code).handle { invite, throwable ->
            if (throwable == null) success(invite)
            else failure(throwable)
        }
    }

}
