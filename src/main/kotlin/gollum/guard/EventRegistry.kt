package gollum.guard

import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.SnowGolemEntity
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object EventRegistry {
    private val logger = LoggerFactory.getLogger("GolemGuardEventRegistry")
    
    // Cooldown system to prevent double-triggers (player UUID -> last interaction time)
    private val interactionCooldowns = ConcurrentHashMap<UUID, Long>()
    
    fun register() {
        logger.info("Registering event handlers...")
        
        UseEntityCallback.EVENT.register { player, world, hand, entity, _ ->
            // Only process on server side
            if (!world.isClient && (entity is IronGolemEntity || entity is SnowGolemEntity)) {
                
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
                        stack.getCustomName()?.string ?: "DefaultKey"
                    }
                    if (!comp.isGuard) {
                        // Convert the golem to a guard golem
                        comp.isGuard = true
                        comp.keyName = keyName
                        
                        // Consume the item (unless in creative mode)
                        if (!player.isCreative) {
                            stack.decrement(1)
                        }
                        
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
                        // Already a guard golem
                        player.sendMessage(
                            Text.literal(Config.getMessage("alreadyHired", "key" to (comp.keyName ?: "Unknown"))), 
                            false
                        )
                        return@register ActionResult.SUCCESS
                    }
                } else {
                    // Not holding valid key item - provide info about guard status
                    if (comp.isGuard) {
                        player.sendMessage(
                            Text.literal(Config.getMessage("belongsToOther")), 
                            false
                        )
                    } else {
                        val instruction = if (Config.requireCustomName) {
                            Config.getMessage("instruction")
                        } else {
                            Config.getMessage("noKeyItem")
                        }
                        player.sendMessage(
                            Text.literal(instruction), 
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