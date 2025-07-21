package golem.guard

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * Comprehensive configuration data class for JSON serialization
 */
data class ConfigData(
    val keyItem: String = "minecraft:stick",
    val requireCustomName: Boolean = true,
    val allowIronGolems: Boolean = true,
    val allowSnowGolems: Boolean = true,
    val allowGuardians: Boolean = true,
    val allowElderGuardians: Boolean = true,
    val attackAllHostileMobs: Boolean = true,
    val attackPlayers: Boolean = true,
    val attackRange: Double = 16.0,
    val tickRate: Int = 20,
    val cooldownMs: Long = 500,
    val checkBundles: Boolean = true,
    val checkShulkerBoxes: Boolean = false,
    val defaultKeyName: String = "DefaultKey",
    val messages: MessageConfig = MessageConfig(),
    val enableDebugLogging: Boolean = false
)

/**
 * Message configuration for all mod messages
 */
data class MessageConfig(
    val golemHired: String = "§aGolem hired with key %key%.",
    val golemAlreadyHired: String = "§6Golem already hired.",
    val instructionMessage: String = "§7Right-click with a renamed %item% to hire this golem.",
    val noKeyItem: String = "§cYou need a renamed %item% to hire golems.",
    val ironGolemDisabled: String = "§cIron golems cannot be hired.",
    val snowGolemDisabled: String = "§cSnow golems cannot be hired.",
    val guardianDisabled: String = "§cGuardians cannot be hired.",
    val elderGuardianDisabled: String = "§cElder guardians cannot be hired.",
    val requiresCustomName: String = "§cThe %item% needs a custom name to hire golems."
)

object Config {
    private val logger = LoggerFactory.getLogger("GolemGuardConfig")
    
    var keyItem: Item = Items.STICK
    var requireCustomName = true
    var allowIronGolems = true
    var allowSnowGolems = true
    var allowGuardians = true
    var allowElderGuardians = true
    var attackAllHostileMobs = true
    var attackPlayers = true
    var attackRange = 16.0
    var tickRate = 20
    var cooldownMs = 500L
    var checkBundles = true
    var checkShulkerBoxes = false
    var defaultKeyName = "DefaultKey"
    var messages = MessageConfig()
    var enableDebugLogging = false

    private val configFile = Path.of("config", "golemguard.json")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun load() {
        logger.info("Loading Guard My Stuff configuration...")
        
        try {
            // Create config directory if it doesn't exist
            Files.createDirectories(configFile.parent)
            
            val configData = if (Files.exists(configFile)) {
                // Load existing config
                val jsonContent = Files.readString(configFile)
                gson.fromJson(jsonContent, ConfigData::class.java)
            } else {
                // Load bundled default config and copy it to user config
                val defaultConfig = loadBundledDefaults()
                val jsonContent = gson.toJson(defaultConfig)
                Files.writeString(configFile, jsonContent)
                logger.info("Created default configuration at ${configFile.toAbsolutePath()}")
                defaultConfig
            }
            
            // Apply loaded configuration
            applyConfig(configData)
            
            logger.info("Configuration loaded successfully")
            logConfigSummary()
            
        } catch (e: Exception) {
            logger.error("Failed to load configuration, using defaults", e)
            useDefaults()
        }
    }
    
    private fun applyConfig(configData: ConfigData) {
        val keyItemId = configData.keyItem
        keyItem = Registries.ITEM.get(Identifier.tryParse(keyItemId)) ?: Items.STICK
        requireCustomName = configData.requireCustomName
        allowIronGolems = configData.allowIronGolems
        allowSnowGolems = configData.allowSnowGolems
        allowGuardians = configData.allowGuardians
        allowElderGuardians = configData.allowElderGuardians
        attackAllHostileMobs = configData.attackAllHostileMobs
        attackPlayers = configData.attackPlayers
        attackRange = configData.attackRange.coerceIn(1.0, 64.0)
        tickRate = configData.tickRate.coerceIn(1, 100)
        cooldownMs = configData.cooldownMs.coerceIn(100L, 5000L)
        checkBundles = configData.checkBundles
        checkShulkerBoxes = configData.checkShulkerBoxes
        defaultKeyName = configData.defaultKeyName
        messages = configData.messages
        enableDebugLogging = configData.enableDebugLogging
    }
    
    private fun logConfigSummary() {
        logger.info("=== Golem Guard Configuration ===")
        logger.info("Key item: ${Registries.ITEM.getId(keyItem)}")
        logger.info("Require custom name: $requireCustomName")
        logger.info("Allow iron golems: $allowIronGolems")
        logger.info("Allow snow golems: $allowSnowGolems")
        logger.info("Allow guardians: $allowGuardians")
        logger.info("Allow elder guardians: $allowElderGuardians")
        logger.info("Attack all hostile mobs: $attackAllHostileMobs")
        logger.info("Attack players: $attackPlayers")
        logger.info("Attack range: ${attackRange} blocks")
        logger.info("Tick rate: every $tickRate ticks")
        logger.info("Interaction cooldown: ${cooldownMs}ms")
        logger.info("Check bundles: $checkBundles")
        logger.info("Check shulker boxes: $checkShulkerBoxes")
        logger.info("Debug logging: $enableDebugLogging")
        logger.info("===================================")
    }
    
    private fun loadBundledDefaults(): ConfigData {
        return try {
            // Load the bundled default config from resources
            val resourceStream = this::class.java.getResourceAsStream("/config.json")
            if (resourceStream != null) {
                val jsonContent = resourceStream.bufferedReader().readText()
                gson.fromJson(jsonContent, ConfigData::class.java)
            } else {
                logger.warn("Bundled default config not found, using hardcoded defaults")
                ConfigData()
            }
        } catch (e: Exception) {
            logger.error("Failed to load bundled default config, using hardcoded defaults", e)
            ConfigData()
        }
    }
    
    private fun useDefaults() {
        val defaultConfig = loadBundledDefaults()
        applyConfig(defaultConfig)
    }
    
    /**
     * Get a formatted message with placeholders replaced
     */
    fun getMessage(messageType: String, vararg replacements: Pair<String, String>): String {
        val template = when (messageType) {
            "hired" -> messages.golemHired
            "alreadyHired" -> messages.golemAlreadyHired
            "instruction" -> messages.instructionMessage
            "noKeyItem" -> messages.noKeyItem
            "ironDisabled" -> messages.ironGolemDisabled
            "snowDisabled" -> messages.snowGolemDisabled
            "guardianDisabled" -> messages.guardianDisabled
            "elderGuardianDisabled" -> messages.elderGuardianDisabled
            "requiresName" -> messages.requiresCustomName
            else -> messageType
        }
        
        var result = template
        // Replace %item% with key item name
        result = result.replace("%item%", keyItem.name.string)
        
        // Replace custom placeholders
        for ((placeholder, value) in replacements) {
            result = result.replace("%$placeholder%", value)
        }
        
        return result
    }
} 