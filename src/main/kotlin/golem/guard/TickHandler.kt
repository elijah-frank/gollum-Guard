package golem.guard

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.HostileEntity
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.SnowGolemEntity
import net.minecraft.entity.mob.GuardianEntity
import net.minecraft.entity.mob.ElderGuardianEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.BundleContentsComponent
import net.minecraft.component.type.ContainerComponent
import net.minecraft.block.ShulkerBoxBlock
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.util.hit.HitResult
import org.slf4j.LoggerFactory
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object TickHandler {
    private val logger = LoggerFactory.getLogger("GolemGuardTickHandler")
    private var tickCounter = 0
    
    // Track targeting duration for snow golems to detect blocked shots
    private val snowGolemTargetingTime = ConcurrentHashMap<UUID, Long>()
    private const val MAX_BLOCKED_TIME = 60L // Maximum ticks before checking line of sight
    
    fun register() {
        logger.info("Registering tick handler for guard golem AI...")
        
        ServerTickEvents.END_SERVER_TICK.register { server ->
            tickCounter++
            
            server.worlds.forEach { world ->
                world.iterateEntities().forEach { entity ->
                    // Check if this golem type is enabled
                    val isValidGolem = when (entity) {
                        is IronGolemEntity -> Config.allowIronGolems
                        is SnowGolemEntity -> Config.allowSnowGolems
                        is GuardianEntity -> Config.allowGuardians
                        is ElderGuardianEntity -> Config.allowElderGuardians
                        else -> false
                    }
                    
                    if (isValidGolem && entity is LivingEntity) {
                        val comp = GuardComponent.get(entity)
                        if (comp.isGuard) {
                            // Only process AI logic every 10 ticks (2x per second) - reduced frequency for performance
                            if (tickCounter % 10 == 0) {
                                // Ensure the golem stays persistent
                                when (entity) {
                                    is IronGolemEntity -> entity.setPersistent()
                                    is SnowGolemEntity -> entity.setPersistent()
                                    is GuardianEntity -> entity.setPersistent()
                                    is ElderGuardianEntity -> entity.setPersistent()
                                }
                                
                                // Update last patrol time
                                comp.lastPatrolTime = world.time
                                
                                // Get current target
                                val currentTarget = when (entity) {
                                    is IronGolemEntity -> entity.target
                                    is SnowGolemEntity -> entity.target
                                    is GuardianEntity -> entity.target
                                    is ElderGuardianEntity -> entity.target
                                    else -> null
                                }
                                
                                // Snow golem line-of-sight validation to prevent targeting blocked enemies
                                if (entity is SnowGolemEntity && currentTarget != null && currentTarget.isAlive) {
                                    val golemUuid = entity.uuid
                                    val currentTime = world.time
                                    
                                    val startTime = snowGolemTargetingTime.getOrPut(golemUuid) { currentTime }
                                    val timeSinceTargeting = currentTime - startTime
                                    
                                    if (timeSinceTargeting > MAX_BLOCKED_TIME) {
                                        if (!hasLineOfSight(entity, currentTarget, world)) {
                                            entity.target = null
                                            snowGolemTargetingTime.remove(golemUuid)
                                            if (Config.enableDebugLogging) {
                                                logger.info("Snow golem cleared blocked target: ${currentTarget.name.string}")
                                            }
                                        } else {
                                            snowGolemTargetingTime[golemUuid] = currentTime
                                        }
                                    }
                                } else if (entity is SnowGolemEntity && currentTarget == null) {
                                    snowGolemTargetingTime.remove(entity.uuid)
                                }
                                
                                // Find new targets if none exists
                                if (currentTarget == null) {
                                    val baseRadius = if (tickCounter % 40 == 0) Config.attackRange else Config.attackRange * 0.75
                                    val searchRadius = if ((entity is GuardianEntity || entity is ElderGuardianEntity) && entity.isTouchingWater) {
                                        baseRadius * 1.5 // Extended range for water-based Guardian beam attacks
                                    } else {
                                        baseRadius
                                    }
                                    
                                    val nearbyEntities = world.getOtherEntities(entity, entity.boundingBox.expand(searchRadius)) { 
                                        // Optimized filter: only check relevant entities, skip dead/invalid ones
                                        (it is PlayerEntity || it is HostileEntity) && it.isAlive && !it.isRemoved
                                    }
                                    
                                    // Early exit if no entities nearby
                                    if (nearbyEntities.isEmpty()) return@forEach
                                    
                                    // Separate players and hostile mobs for prioritization
                                    val potentialTargets = nearbyEntities.filterIsInstance<LivingEntity>()
                                        .filter { shouldAttack(it, comp) }
                                    
                                    // Early exit if no valid targets
                                    if (potentialTargets.isEmpty()) return@forEach
                                    
                                    // PRIORITIZE PLAYERS FIRST - they are the primary threat
                                    val playerTargets = potentialTargets.filterIsInstance<PlayerEntity>()
                                    val mobTargets = potentialTargets.filterIsInstance<HostileEntity>()
                                    
                                    val target = if ((entity is GuardianEntity || entity is ElderGuardianEntity) && entity.isTouchingWater) {
                                        // Prioritize targets with clear line of sight for beam attacks
                                        val targetsWithLineOfSight = (playerTargets + mobTargets).filter { targetEntity ->
                                            hasLineOfSight(entity, targetEntity, world)
                                        }
                                        
                                        when {
                                            targetsWithLineOfSight.any { it is PlayerEntity } -> {
                                                targetsWithLineOfSight.filterIsInstance<PlayerEntity>().minByOrNull { it.squaredDistanceTo(entity) }
                                            }
                                            targetsWithLineOfSight.isNotEmpty() -> {
                                                targetsWithLineOfSight.minByOrNull { it.squaredDistanceTo(entity) }
                                            }
                                            playerTargets.isNotEmpty() -> {
                                                playerTargets.minByOrNull { it.squaredDistanceTo(entity) }
                                            }
                                            else -> {
                                                mobTargets.minByOrNull { it.squaredDistanceTo(entity) }
                                            }
                                        }
                                    } else {
                                        when {
                                            playerTargets.isNotEmpty() -> playerTargets.minByOrNull { it.squaredDistanceTo(entity) }
                                            mobTargets.isNotEmpty() -> mobTargets.minByOrNull { it.squaredDistanceTo(entity) }
                                            else -> null
                                        }
                                    }
                                    
                                    target?.let { targetEntity ->
                                        // Set the target for the golem to attack
                                        when (entity) {
                                            is IronGolemEntity -> {
                                                entity.target = targetEntity
                                                // Improve pathfinding - set angry at player to make them more aggressive
                                                if (targetEntity is PlayerEntity) {
                                                    entity.setAngryAt(targetEntity.uuid)
                                                }
                                            }
                                            is SnowGolemEntity -> {
                                                entity.target = targetEntity
                                                snowGolemTargetingTime[entity.uuid] = world.time
                                            }
                                            is GuardianEntity -> {
                                                entity.target = targetEntity
                                                if (entity.isTouchingWater) {
                                                    entity.navigation.stop() // Use beam attacks instead of chasing
                                                }
                                            }
                                            is ElderGuardianEntity -> {
                                                entity.target = targetEntity
                                                if (entity.isTouchingWater) {
                                                    entity.navigation.stop() // Use beam attacks instead of chasing
                                                }
                                            }
                                        }
                                        
                                        if (Config.enableDebugLogging) {
                                            val targetType = if (targetEntity is PlayerEntity) "player" else "mob"
                                            val specialMsg = if ((entity is GuardianEntity || entity is ElderGuardianEntity) && entity.isTouchingWater) {
                                                val targetLocation = if (targetEntity.isTouchingWater) "in water" else "on land"
                                                " (beam attack: Guardian in water targeting $targetLocation)"
                                            } else ""
                                            logger.info("Guard golem '${comp.keyName}' targeting $targetType: ${targetEntity.name.string}$specialMsg")
                                        }
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
     * Check if the entity has a clear line of sight to its target
     */
    private fun hasLineOfSight(entity: LivingEntity, target: LivingEntity, world: net.minecraft.world.World): Boolean {
        // Get eye positions for more accurate line of sight
        val entityEyePos = Vec3d(
            entity.x,
            entity.y + entity.standingEyeHeight,
            entity.z
        )
        
        val targetEyePos = Vec3d(
            target.x,
            target.y + target.standingEyeHeight * 0.5, // Aim for center of target
            target.z
        )
        
        // Perform raycast to check for blocks in the way
        val raycastContext = RaycastContext(
            entityEyePos,
            targetEyePos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE,
            entity
        )
        
        val hitResult = world.raycast(raycastContext)
        
        // If we hit a block, there's no line of sight
        return hitResult.type == HitResult.Type.MISS
    }
    
    /**
     * Determines if a guard golem should attack the given target entity.
     * PRIORITY: Players without keys > Hostile mobs
     */
    private fun shouldAttack(target: LivingEntity, comp: GuardComponent): Boolean {
        val keyName = comp.keyName ?: return false
        
        // PRIORITY 1: Attack unauthorized players (highest priority)
        if (target is PlayerEntity && Config.attackPlayers) {
            val hasKey = hasValidKeyItem(target, keyName)
            // Only log when we find a valid target or when debugging is enabled
            if (Config.enableDebugLogging && !hasKey) {
                logger.info("Player ${target.name.string} has no key '$keyName' - will target")
            }
            return !hasKey
        }
        
        // PRIORITY 2: Attack hostile mobs (lower priority)
        if (target is HostileEntity && Config.attackAllHostileMobs) {
            // Don't spam log for every mob check
            return true
        }
        
        return false
    }
    
    /**
     * Checks if a player has the valid key item (with matching name) in their inventory.
     * Optimized with early exits for performance.
     */
    fun hasValidKeyItem(player: PlayerEntity, keyName: String): Boolean {
        // Check main inventory (most common location)
        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (isValidKeyStack(stack, keyName)) {
                return true // Early exit - found the key!
            }
        }
        
        // Check offhand (quick check)
        if (isValidKeyStack(player.offHandStack, keyName)) {
            return true
        }
        
        // Only check bundles/shulkers if enabled (expensive operations)
        if (Config.checkBundles || Config.checkShulkerBoxes) {
            for (i in 0 until player.inventory.size()) {
                val stack = player.inventory.getStack(i)
                
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
            
            // Check offhand containers
            val offhandStack = player.offHandStack
            if (Config.checkBundles && offhandStack.item == Items.BUNDLE) {
                if (checkBundleForKey(offhandStack, keyName)) {
                    return true
                }
            }
            
            if (Config.checkShulkerBoxes && offhandStack.item is net.minecraft.item.BlockItem) {
                val blockItem = offhandStack.item as net.minecraft.item.BlockItem
                if (blockItem.block is ShulkerBoxBlock) {
                    if (checkShulkerBoxForKey(offhandStack, keyName)) {
                        return true
                    }
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
        if (stack.item != Config.keyItem) {
            return false // Don't spam log for non-key items
        }
        
        // Handle the case where custom names are not required
        val itemKeyName = if (Config.requireCustomName) {
            // Must have custom name
            if (stack.getCustomName() == null) {
                return false
            }
            stack.getCustomName()!!.string
        } else {
            // Can have custom name or use default
            stack.getCustomName()?.string ?: Config.defaultKeyName
        }
        
        val matches = itemKeyName == keyName
        if (Config.enableDebugLogging && matches) {
            logger.info("Found matching key: '$itemKeyName'")
        }
        
        return matches
    }
} 