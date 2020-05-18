package me.devoxin.flight

import java.io.InputStreamReader

object FlightInfo {
    val VERSION: String
    val GIT_REVISION: String

    init {
        val stream = FlightInfo::class.java.classLoader.getResourceAsStream("flight.txt")!!
        val reader = InputStreamReader(stream).readText()
        val (buildVersion, buildRevision) = reader.split('\n')

        VERSION = buildVersion
        GIT_REVISION = buildRevision
    }
}
