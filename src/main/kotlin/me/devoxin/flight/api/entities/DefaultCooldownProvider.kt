package me.devoxin.flight.api.entities

import me.devoxin.flight.api.CommandFunction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class DefaultCooldownProvider : CooldownProvider {
    private val buckets = ConcurrentHashMap<BucketType, Bucket>()

    override fun isOnCooldown(id: Long, bucket: BucketType, command: CommandFunction): Boolean {
        return buckets[bucket]?.isOnCooldown(id, command.name) ?: false
    }

    override fun getCooldownTime(id: Long, bucket: BucketType, command: CommandFunction): Long {
        return buckets[bucket]?.getCooldownRemainingTime(id, command.name) ?: 0
    }

    override fun setCooldown(id: Long, bucket: BucketType, time: Long, command: CommandFunction) {
        buckets.computeIfAbsent(bucket) { Bucket() }.setCooldown(id, time, command.name)
    }

    override fun removeCooldown(id: Long, bucket: BucketType, command: CommandFunction) {
        buckets[bucket]?.removeCooldown(id, command.name)
    }

    override fun clearCooldowns(command: CommandFunction) {
        buckets.values.forEach { it.clearCooldown(command.name) }
    }

    override fun clearCooldowns(id: Long, bucket: BucketType) {
        buckets[bucket]?.clearCooldowns(id)
    }

    override fun clearCooldowns() {
        buckets.values.forEach { it.empty() }
    }


    inner class Bucket {
        private val sweeperThread = Executors.newSingleThreadScheduledExecutor()
        private val cooldowns = ConcurrentHashMap<Long, MutableSet<Cooldown>>() // EntityID => [Commands...]

        fun isOnCooldown(id: Long, commandName: String): Boolean {
            return getCooldownRemainingTime(id, commandName) > 0
        }

        fun getCooldownRemainingTime(id: Long, commandName: String): Long {
            val cd = cooldowns[id]?.firstOrNull { it.name == commandName }
                ?: return 0

            return abs(cd.expires - System.currentTimeMillis())
        }

        fun setCooldown(id: Long, time: Long, commandName: String) {
            val cds = cooldowns.computeIfAbsent(id) { mutableSetOf() }
            val cooldown = Cooldown(commandName, System.currentTimeMillis() + time)
            cds.add(cooldown)

            sweeperThread.schedule({ cds.remove(cooldown) }, time, TimeUnit.MILLISECONDS)
        }

        fun removeCooldown(id: Long, commandName: String) {
            cooldowns[id]?.removeIf { it.name == commandName }
        }

        fun clearCooldown(commandName: String) {
            cooldowns.values.forEach { it.removeIf { cd -> cd.name == commandName } }
        }

        fun clearCooldowns(id: Long) {
            cooldowns.remove(id)
        }

        fun empty() {
            cooldowns.clear()
        }
    }

    inner class Cooldown(val name: String, val expires: Long) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Cooldown

            return name == other.name
        }

        override fun hashCode(): Int {
            return 31 * name.hashCode()
        }
    }
}
