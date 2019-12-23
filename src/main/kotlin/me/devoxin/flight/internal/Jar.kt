package me.devoxin.flight.internal

import java.net.URLClassLoader

class Jar(
    val name: String,
    val location: String,
    val packageName: String,
    private val classLoader: URLClassLoader
) {

    internal fun close() {
        classLoader.close()
    }

}
