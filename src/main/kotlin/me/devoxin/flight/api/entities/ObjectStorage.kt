package me.devoxin.flight.api.entities

import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.MutableMap.MutableEntry

class ObjectStorage : MutableMap<String, Any> {
    private val map = ConcurrentHashMap<String, Any>()

    override val size: Int
        get() = map.size

    override val entries: MutableSet<MutableEntry<String, Any>>
        get() = map.entries

    override val keys: MutableSet<String>
        get() = map.keys

    override val values: MutableCollection<Any>
        get() = map.values

    operator fun set(key: String, value: Any) {
        map[key] = value
    }

    override operator fun get(key: String): Any? {
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
        return map.computeIfAbsent(key, initializer) as? T
            ?: throw IllegalStateException("Computed object is not of type T!")
    }

    override fun clear() {
        map.clear()
    }

    override fun isEmpty() = map.isEmpty()

    override fun remove(key: String): Any? = map.remove(key)

    override fun putAll(from: Map<out String, Any>) = map.putAll(from)

    override fun put(key: String, value: Any): Any? = map.put(key, value)

    override fun containsValue(value: Any) = map.containsValue(value)

    override fun containsKey(key: String) = map.containsKey(key)
}
