package me.devoxin.flight.parsers

import me.devoxin.flight.Context
import java.util.*

interface Parser<T> {

    fun parse(ctx: Context, param: String): Optional<T>

}