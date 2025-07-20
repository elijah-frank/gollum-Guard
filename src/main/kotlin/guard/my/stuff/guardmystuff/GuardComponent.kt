package guard.my.stuff.guardmystuff

import net.minecraft.entity.LivingEntity
import net.minecraft.nbt.NbtCompound
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Component that stores guard-related data for golems.
 * Since we can't use Cardinal Components API in this simple implementation,
 * we'll use a map-based approach to store guard data.
 */
class GuardComponent {
    var isGuard: Boolean = false
    var keyName: String? = null
    var lastPatrolTime: Long = 0L
    
    fun writeToNbt(nbt: NbtCompound) {
        nbt.putBoolean("IsGuard", isGuard)
        keyName?.let { nbt.putString("KeyName", it) }
        nbt.putLong("LastPatrolTime", lastPatrolTime)
    }
    
    fun readFromNbt(nbt: NbtCompound) {
        isGuard = if (nbt.contains("IsGuard")) nbt.getBoolean("IsGuard") as Boolean else false
        keyName = if (nbt.contains("KeyName")) nbt.getString("KeyName") as String? else null
        lastPatrolTime = if (nbt.contains("LastPatrolTime")) nbt.getLong("LastPatrolTime") as Long else 0L
    }
    
    companion object {
        private val logger = LoggerFactory.getLogger("GuardComponent")
        private val guardComponents = ConcurrentHashMap<UUID, GuardComponent>()
        
        /**
         * Gets the guard component for the specified entity.
         * Creates a new one if it doesn't exist.
         */
        fun get(entity: LivingEntity): GuardComponent {
            return guardComponents.computeIfAbsent(entity.uuid) { 
                GuardComponent().also {
                    logger.debug("Created new GuardComponent for entity ${entity.uuid}")
                }
            }
        }
        
        /**
         * Removes the guard component for the specified entity.
         * Called when an entity is removed from the world.
         */
        fun remove(entityUuid: UUID) {
            guardComponents.remove(entityUuid)?.let {
                logger.debug("Removed GuardComponent for entity $entityUuid")
            }
        }
        
        /**
         * Gets all currently guarded entities.
         */
        fun getAllGuards(): Map<UUID, GuardComponent> {
            return guardComponents.filter { it.value.isGuard }
        }
        
        /**
         * Saves guard data to NBT for a specific entity.
         */
        fun saveToNbt(entityUuid: UUID, nbt: NbtCompound) {
            guardComponents[entityUuid]?.let { component ->
                val guardNbt = NbtCompound()
                component.writeToNbt(guardNbt)
                nbt.put("GuardComponent", guardNbt)
            }
                }
        
        /**
         * Loads guard data from NBT for a specific entity.
         */
        fun loadFromNbt(entityUuid: UUID, nbt: NbtCompound) {
            if (nbt.contains("GuardComponent")) {
                val component = GuardComponent()
                val guardNbt = if (nbt.contains("GuardComponent")) nbt.getCompound("GuardComponent") as NbtCompound else NbtCompound()
                component.readFromNbt(guardNbt)
                guardComponents[entityUuid] = component
            }
        }
    }
} 