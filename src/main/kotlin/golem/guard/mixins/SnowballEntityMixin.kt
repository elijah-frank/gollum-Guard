package golem.guard.mixins

import net.minecraft.entity.LivingEntity
import net.minecraft.entity.projectile.thrown.SnowballEntity
import net.minecraft.util.hit.EntityHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.block.Blocks
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(SnowballEntity::class)
class SnowballEntityMixin {
    
    @Inject(method = ["method_7454"], at = [At("TAIL")])
    private fun onSnowballHit(entityHitResult: EntityHitResult, info: CallbackInfo) {
        val snowball = this as SnowballEntity
        val hitEntity = entityHitResult.entity
        
        if (hitEntity is LivingEntity) {
            val world = snowball.world
            var hasFireSource = false
            var isSoulFire = false
            
            // Get snowball's current position and velocity to trace back its path
            val currentPos = snowball.pos
            val velocity = snowball.velocity
            
            // Trace back along the trajectory for up to 10 blocks or until we find fire
            val maxDistance = 10.0
            val stepSize = 0.5 // Check every half block
            val steps = (maxDistance / stepSize).toInt()
            
            for (i in 0..steps) {
                val t = i * stepSize
                val checkX = currentPos.x - velocity.x * t
                val checkY = currentPos.y - velocity.y * t
                val checkZ = currentPos.z - velocity.z * t
                val checkPos = BlockPos.ofFloored(checkX, checkY, checkZ)
                
                val block = world.getBlockState(checkPos).block
                
                // Check for regular fire sources
                if (block == Blocks.FIRE || block == Blocks.LAVA || 
                    block == Blocks.MAGMA_BLOCK) {
                    hasFireSource = true
                }
                
                // Check for soul fire sources
                if (block == Blocks.SOUL_FIRE || block == Blocks.SOUL_TORCH || 
                    block == Blocks.SOUL_WALL_TORCH || block == Blocks.SOUL_LANTERN) {
                    hasFireSource = true
                    isSoulFire = true
                }
                
                // Stop early if we found soul fire (highest priority)
                if (hasFireSource && isSoulFire) break
            }
            
            if (hasFireSource) {
                if (isSoulFire) {
                    // SOUL FIRE: Place soul fire block under target (natural soul fire damage)
                    val targetPos = BlockPos.ofFloored(hitEntity.pos)
                    val belowPos = targetPos.down()
                    
                    // Only place if the block below is solid and the space above it is air
                    if (world.getBlockState(belowPos).isSolidBlock(world, belowPos) && 
                        world.getBlockState(targetPos).isAir) {
                        world.setBlockState(targetPos, Blocks.SOUL_FIRE.defaultState)
                    }
                } else {
                    // Regular fire snowball - set entity on fire normally
                    hitEntity.setOnFireFor(5.0f) // 5 seconds of regular fire
                }
            }
        }
    }
} 