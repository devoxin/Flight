package me.devoxin.flight.api.entities

import java.util.concurrent.ConcurrentHashMap

class ObjectStorage {
    private val map = ConcurrentHashMap<String, Any>()

    val size: Int
        get() = map.size

    operator fun set(key: String, value: Any) {
        map[key] = value
    }

    operator fun get(key: String): Any? {
        return map[key]
    }

    fun <T> get(key: String, cls: Class<T>): T? {
        val value = map[key]

        if (cls.isInstance(value)) {
            return cls.cast(value)
        }

        return null
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> computeIfAbsent(key: String, initializer: (String) -> T): T {
        return map.computeIfAbsent(key, initializer) as T
    }

    fun clear() {
        map.clear()
    }
}
