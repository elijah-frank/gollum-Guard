package golem.guard

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.SnowGolemEntity
import net.minecraft.entity.mob.GuardianEntity
import net.minecraft.entity.mob.ElderGuardianEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.entity.LivingEntity
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EventRegistry {
    private val logger = LoggerFactory.getLogger("GolemGuardEventRegistry")
    
    // Cooldown system to prevent double-triggers (player UUID -> last interaction time)
    private val interactionCooldowns = ConcurrentHashMap<UUID, Long>()
    
    /**
     * Called when a player's inventory changes - check if they got a key and notify nearby golems
     */
    fun checkPlayerKeyAndNotifyGolems(player: ServerPlayerEntity) {
        if (!Config.attackPlayers) return // No point checking if golems don't attack players
        
        val world = player.world
        // Use smaller radius for performance - only check very nearby golems
        val nearbyGolems = world.getOtherEntities(player, player.boundingBox.expand(Config.attackRange)) { entity ->
            // Quick filter: only iron/snow golems and guardians that are alive
            (entity is IronGolemEntity || entity is SnowGolemEntity || entity is GuardianEntity || entity is ElderGuardianEntity) && entity.isAlive && !entity.isRemoved
        }
        
        // Early exit if no golems nearby
        if (nearbyGolems.isEmpty()) return
        
        nearbyGolems.forEach { entity ->
            val comp = GuardComponent.get(entity as LivingEntity)
            if (comp.isGuard && comp.keyName != null) {
                // Check if this golem is targeting this player
                val currentTarget = when (entity) {
                    is IronGolemEntity -> entity.target
                    is SnowGolemEntity -> entity.target
                    is GuardianEntity -> entity.target
                    is ElderGuardianEntity -> entity.target
                    else -> null
                }
                
                if (currentTarget == player) {
                    // Check if player now has the correct key
                    if (TickHandler.hasValidKeyItem(player, comp.keyName!!)) {
                        // Player now has the key, stop targeting them
                        when (entity) {
                            is IronGolemEntity -> {
                                entity.target = null
                                entity.setAngryAt(null)
                            }
                            is SnowGolemEntity -> {
                                entity.target = null
                            }
                            is GuardianEntity -> {
                                entity.target = null
                            }
                            is ElderGuardianEntity -> {
                                entity.target = null
                            }
                        }
                        
                        if (Config.enableDebugLogging) {
                            logger.info("Golem '${comp.keyName}' stopped targeting ${player.name.string} due to inventory change")
                        }
                    }
                }
            }
        }
    }
    
    fun register() {
        logger.info("Registering event handlers...")
        
        // Register inventory change checking (every 20 ticks = 1 time per second for performance)
        ServerTickEvents.END_SERVER_TICK.register { server ->
            // Only check every 20 ticks for performance (reduced from 10)
            if (server.ticks % 20 == 0) {
                // Only check players if attack players is enabled
                if (Config.attackPlayers) {
                    server.playerManager.playerList.forEach { player ->
                        checkPlayerKeyAndNotifyGolems(player)
                    }
                }
            }
        }
        
        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            // Only process on server side
            if (!world.isClient && (entity is IronGolemEntity || entity is SnowGolemEntity || entity is GuardianEntity || entity is ElderGuardianEntity)) {
                
                // Check if this golem type is allowed
                when (entity) {
                    is IronGolemEntity -> {
                        if (!Config.allowIronGolems) {
                            player.sendMessage(
                                Text.literal(Config.getMessage("ironDisabled")), 
                                false
                            )
                            return@register ActionResult.SUCCESS
                        }
                    }
                    is SnowGolemEntity -> {
                        if (!Config.allowSnowGolems) {
                            player.sendMessage(
                                Text.literal(Config.getMessage("snowDisabled")), 
                                false
                            )
                            return@register ActionResult.SUCCESS
                        }
                    }
                    is GuardianEntity -> {
                        if (!Config.allowGuardians) {
                            player.sendMessage(
                                Text.literal(Config.getMessage("guardianDisabled")), 
                                false
                            )
                            return@register ActionResult.SUCCESS
                        }
                    }
                    is ElderGuardianEntity -> {
                        if (!Config.allowElderGuardians) {
                            player.sendMessage(
                                Text.literal(Config.getMessage("elderGuardianDisabled")), 
                                false
                            )
                            return@register ActionResult.SUCCESS
                        }
                    }
                }
                
                // Check cooldown to prevent double-triggers
                val currentTime = System.currentTimeMillis()
                val lastInteraction = interactionCooldowns[player.uuid] ?: 0L
                
                if (currentTime - lastInteraction < Config.cooldownMs) {
                    return@register ActionResult.SUCCESS // Still in cooldown, ignore
                }
                
                interactionCooldowns[player.uuid] = currentTime
                
                val stack = player.getStackInHand(hand)
                val comp = GuardComponent.get(entity)
                
                // Check if player is holding the configured key item
                if (stack.item == Config.keyItem) {
                    // Check custom name requirement
                    if (Config.requireCustomName && stack.getCustomName() == null) {
                        player.sendMessage(
                            Text.literal(Config.getMessage("requiresName")), 
                            false
                        )
                        return@register ActionResult.SUCCESS
                    }
                    
                    // If we don't require custom names but there isn't one, use a default
                    val keyName = if (Config.requireCustomName) {
                        stack.getCustomName()!!.string
                    } else {
                        stack.getCustomName()?.string ?: Config.defaultKeyName
                    }
                    if (!comp.isGuard) {
                        // Convert the golem to a guard golem
                        comp.isGuard = true
                        comp.keyName = keyName
                        
                        // Key item remains in inventory for reuse
                        
                        // Set the golem as persistent so it doesn't despawn
                        entity.setPersistent()
                        
                        // Notify the player with configured message
                        player.sendMessage(
                            Text.literal(Config.getMessage("hired", "key" to keyName)), 
                            false
                        )
                        
                        if (Config.enableDebugLogging) {
                            logger.info("Player ${player.name.string} hired a ${entity.javaClass.simpleName} with key '$keyName'")
                        }
                        
                        return@register ActionResult.SUCCESS
                    } else {
                        // Already a guard golem - just say it's already hired
                        player.sendMessage(
                            Text.literal(Config.getMessage("golemAlreadyHired")), 
                            false
                        )
                        return@register ActionResult.SUCCESS
                    }
                } else {
                    // Not holding valid key item - provide info about guard status
                    if (comp.isGuard) {
                        // Golem is hired, show instruction message
                        player.sendMessage(
                            Text.literal(Config.getMessage("instructionMessage")), 
                            false
                        )
                    } else {
                        // Golem not hired, show instruction message
                        player.sendMessage(
                            Text.literal(Config.getMessage("instructionMessage")), 
                            false
                        )
                    }
                    return@register ActionResult.SUCCESS
                }
            }
            ActionResult.PASS
        }
        
        logger.info("Event handlers registered successfully")
    }
} 