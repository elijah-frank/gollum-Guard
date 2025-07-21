package golem.guard

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object GolemGuardMod : ModInitializer {
    private val logger = LoggerFactory.getLogger("GolemGuardMod")

    override fun onInitialize() {
        logger.info("Golem Guard mod initializing...")
        Config.load()                   // load config, including keyItem
        EventRegistry.register()        // set up event handlers
        TickHandler.register()          // set up AI tick handler
        logger.info("Golem Guard mod initialized successfully!")
    }
} 