package golem.guard

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.SnowGolemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.BundleContentsComponent
import net.minecraft.component.type.ContainerComponent
import net.minecraft.block.ShulkerBoxBlock
import org.slf4j.LoggerFactory

object TickHandler {
    private val logger = LoggerFactory.getLogger("GolemGuardTickHandler")
    private var tickCounter = 0
    
    fun register() {
        logger.info("Registering tick handler for guard golem AI...")
        
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            
            // Only process every X ticks based on config (performance optimization)
            if (tickCounter % Config.tickRate != 0) {
                return@register
            }
            
            server.worlds.forEach { world ->
                world.iterateEntities().forEach { entity ->
                    // Check if this golem type is enabled
                    val isValidGolem = when (entity) {
                        is IronGolemEntity -> Config.allowIronGolems
                        is SnowGolemEntity -> Config.allowSnowGolems
                        else -> false
                    }
                    
                    if (isValidGolem && entity is LivingEntity) {
                        val comp = GuardComponent.get(entity)
                        if (comp.isGuard) {
                            // Ensure the golem stays persistent
                            when (entity) {
                                is IronGolemEntity -> entity.setPersistent()
                                is SnowGolemEntity -> entity.setPersistent()
                            }
                            
                            // Update last patrol time
                            comp.lastPatrolTime = world.time
                            
                            // Check if current target is still valid (important for real-time key checks)
                            val currentTarget = if (entity is IronGolemEntity) entity.target else if (entity is SnowGolemEntity) entity.target else null
                            if (currentTarget != null && !shouldAttack(currentTarget, comp)) {
                                // Current target is no longer valid (e.g., player got the key), stop attacking
                                if (entity is IronGolemEntity) {
                                    entity.target = null
                                } else if (entity is SnowGolemEntity) {
                                    entity.target = null
                                }
                                if (Config.enableDebugLogging) {
                                    logger.info("Guard golem '${comp.keyName}' stopped targeting ${currentTarget.name.string} (now whitelisted)")
                                }
                            }
                            
                            // Find nearby entities to potentially target (only if not already targeting)
                            if (currentTarget == null || !shouldAttack(currentTarget, comp)) {
                                val nearbyEntities = world.getOtherEntities(entity, entity.boundingBox.expand(Config.attackRange)) { 
                                    it is LivingEntity && it != entity 
                                }
                                
                                // Find the closest valid target
                                val target = nearbyEntities
                                    .filterIsInstance<LivingEntity>()
                                    .filter { shouldAttack(it, comp) }
                                    .minByOrNull { it.squaredDistanceTo(entity) }
                                
                                target?.let { 
                                    // Set the target for the golem to attack
                                    if (entity is IronGolemEntity) {
                                        entity.target = it
                                    } else if (entity is SnowGolemEntity) {
                                        entity.target = it
                                    }
                                    
                                    if (Config.enableDebugLogging) {
                                        logger.info("Guard golem '${comp.keyName}' targeting ${it.name.string}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        logger.info("Tick handler registered successfully")
    }
    
    /**
     * Determines if a guard golem should attack the given target entity.
     */
    private fun shouldAttack(target: LivingEntity, comp: GuardComponent): Boolean {
        val keyName = comp.keyName ?: return false
        
        // Attack hostile mobs if configured to do so
        if (target is HostileEntity && Config.attackAllHostileMobs) {
            return true // Attack all hostile mobs
        }
        
        // Attack unauthorized players if configured to do so
        if (target is PlayerEntity && Config.attackPlayers) {
            return !hasValidKeyItem(target, keyName)
        }
        
        return false
    }
    
    /**
     * Checks if a player has the valid key item (with matching name) in their inventory.
     * Supports bundles but not shulker boxes.
     */
    private fun hasValidKeyItem(player: PlayerEntity, keyName: String): Boolean {
        // Check main inventory
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (isValidKeyStack(stack, keyName)) {
                return true
            }
            
            // Check inside bundles if enabled
            if (Config.checkBundles && stack.item == Items.BUNDLE) {
                if (checkBundleForKey(stack, keyName)) {
                    return true
                }
            }
            
            // Check inside shulker boxes if enabled (security risk!)
            if (Config.checkShulkerBoxes && stack.item is net.minecraft.item.BlockItem) {
                val blockItem = stack.item as net.minecraft.item.BlockItem
                if (blockItem.block is ShulkerBoxBlock) {
                    if (checkShulkerBoxForKey(stack, keyName)) {
                        return true
                    }
                }
            }
        }
        
        // Check offhand
        val offhandStack = player.offHandStack
        if (isValidKeyStack(offhandStack, keyName)) {
            return true
        }
        
        // Check offhand bundle if enabled
        if (Config.checkBundles && offhandStack.item == Items.BUNDLE) {
            if (checkBundleForKey(offhandStack, keyName)) {
                return true
            }
        }
        
        // Check offhand shulker box if enabled
        if (Config.checkShulkerBoxes && offhandStack.item is net.minecraft.item.BlockItem) {
            val blockItem = offhandStack.item as net.minecraft.item.BlockItem
            if (blockItem.block is ShulkerBoxBlock) {
                if (checkShulkerBoxForKey(offhandStack, keyName)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Checks if a bundle contains the valid key item.
     */
    private fun checkBundleForKey(bundleStack: ItemStack, keyName: String): Boolean {
        val bundleContents = bundleStack.get(DataComponentTypes.BUNDLE_CONTENTS)
        if (bundleContents != null) {
            for (itemStack in bundleContents.iterate()) {
                if (isValidKeyStack(itemStack, keyName)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Checks if a shulker box contains the valid key item.
     */
    private fun checkShulkerBoxForKey(shulkerStack: ItemStack, keyName: String): Boolean {
        val containerContents = shulkerStack.get(DataComponentTypes.CONTAINER)
        if (containerContents != null) {
            for (itemStack in containerContents.iterateNonEmpty()) {
                if (isValidKeyStack(itemStack, keyName)) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Checks if an ItemStack is a valid key item with the correct name.
     */
    private fun isValidKeyStack(stack: ItemStack, keyName: String): Boolean {
        if (stack.item != Config.keyItem) return false
        if (stack.getCustomName() == null) return false
        return stack.getCustomName()!!.string == keyName
    }
} 