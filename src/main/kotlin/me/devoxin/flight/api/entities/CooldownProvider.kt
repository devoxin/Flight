package me.devoxin.flight.api.entities

import me.devoxin.flight.api.CommandFunction

interface CooldownProvider {

    /**
     * Checks whether the entity associated with the provided ID is on cool-down.
     * When BucketType is `GUILD` and the command was invoked in a private context, this
     * method won't be called.
     *
     * @param id
     *        The ID of the entity. If the bucket type is USER, this will be a user ID.
     *        If the bucket type is GUILD, this will be the guild id.
     *        If the bucket type is GLOBAL, this will be -1.
     *
     * @param bucket
     *        The type of bucket the cool-down belongs to.
     *        For example, one bucket for each entity type; USER, GUILD, GLOBAL.
     *        If this parameter is GUILD, theoretically you would do `bucket[type].get(id) != null`
     *
     * @param command
     *        The command that was invoked.
     *
     * @returns True, if the entity associated with the ID is on cool-down and the command should
     *          not be executed.
     */
    fun isOnCooldown(id: Long, bucket: BucketType, command: CommandFunction): Boolean

    /**
     * Gets the remaining time of the cool-down in milliseconds.
     * This may either return 0L, or throw an exception if an entry isn't present, however
     * this should not happen as `isOnCooldown` should be called prior to this.
     *
     * @param id
     *        The ID of the entity. The ID could belong to a user or guild, or be -1 if the bucket is GLOBAL.
     *
     * @param bucket
     *        The type of bucket to check the cool-down of.
     *
     * @param command
     *        The command to get the cool-down time of.
     */
    fun getCooldownTime(id: Long, bucket: BucketType, command: CommandFunction): Long

    /**
     * Adds a cool-down for the given entity ID.
     * It is up to you whether this passively, or actively removes expired cool-downs.
     * When [bucket] is [BucketType.GUILD] and the command was invoked in a private context, this
     * method won't be called.
     *
     * @param id
     *        The ID of the entity, that the cool-down should be associated with.
     *        This ID could belong to a user or guild. If bucket is BucketType.GLOBAL, this will be -1.
     *
     * @param bucket
     *        The type of bucket the cool-down belongs to.
     *
     * @param time
     *        How long the cool-down should last for, in milliseconds.
     *
     * @param command
     *        The command to set cool-down for.
     */
    fun setCooldown(id: Long, bucket: BucketType, time: Long, command: CommandFunction)

    /**
     * Remove a cool-down for a command by entity ID and bucket type, if it exists.
     *
     * @param id
     *        The ID of the entity associated with the cool-down to be removed.
     *        This ID could belong to a user or guild. If bucket is BucketType.GLOBAL, this will be -1.
     *
     * @param bucket
     *        The type of bucket the cool-down belongs to.
     *
     * @param command
     *        The command to remove the cool-down for.
     */
    fun removeCooldown(id: Long, bucket: BucketType, command: CommandFunction)

    /**
     * Removes all cool-downs for a single command.
     *
     * @param command
     *        The command to remove the cool-downs for.
     */
    fun clearCooldowns(command: CommandFunction)

    /**
     * Removes all cool-downs for the given entity ID and bucket type, if there are any.
     *
     * @param id
     *        The ID of the entity associated with the cool-down to be removed.
     *        This ID could belong to a user or guild. If bucket is BucketType.GLOBAL, this will be -1.
     *
     * @param bucket
     *        The type of bucket the cool-down belongs to.
     */
    fun clearCooldowns(id: Long, bucket: BucketType)

    /**
     * Clears all cool-downs stored in this provider.
     */
    fun clearCooldowns()
}
