# Golem Guard

[![Fabric API](https://img.shields.io/badge/modloader-Fabric-1976d2?style=flat-square&logo=fabricmc&logoColor=white)](https://fabricmc.net/)  
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.7-brightgreen?style=flat-square)](https://minecraft.net/)  
[![License](https://img.shields.io/badge/license-Unlicense-blue?style=flat-square)](LICENSE)

## Introduction

I developed **Golem Guard** as a server-only Fabric mod for Minecraft 1.21.7/1.21.8 to let players hire Iron and Snow Golems as personal guards. No client-side installation is required—anyone can join vanilla and immediately wield their golems.

I originally built this mod for my Minecraft server network, **BovisGL** (more info at [bovisgl.xyz](https://bovisgl.xyz)), because our Anarchy game mode has no griefing rules and I wanted a way for players to protect their stuff without burying everything underground.

## Features

- **Key-Based Hiring**  
  Right-click any supported golem with a renamed key item to hire them. Keys are reusable and stay in your inventory.

- **Hire Multiple Golem Types**  
  Recruit Iron Golems, Snow Golems, Guardians, and Elder Guardians as your personal guards.

- **Shareable Access**  
  Anyone with the correct key item is recognized as friendly by your guards. Give keys to friends to let them pass safely.

- **Advanced Combat AI**  
  - **Snow Golems**: Switch targets if their view is blocked, and now their snowball mechanics closely match Bedrock Edition—snowballs passing through fire or lava will ignite targets. This lets you build real working snow golem cannons, just like in Bedrock, for creative base defenses.
  - **Guardians & Elder Guardians**: Can be hired and trapped in tanks to defend your base with their laser beams. They now target both land and sea threats, so a trapped guardian will attack any enemy nearby, making it possible to create powerful laser defenses.
  - **Iron Golems**: Improved pathfinding and persistent targeting for better protection.

- **Configurable Everything**  
  All settings—attack ranges, target types, key items, messages, and performance—are fully customizable via JSON.

- **Performance Optimized**  
  Efficient line-of-sight checks, reduced tick rates, and smart targeting to minimize server impact.

## Installation

1. Download the latest `golem-guard.jar`
2. Ensure Fabric Loader 0.16.14+ and Fabric API 0.96.4+ are installed
3. Place `golem-guard.jar` (and Fabric API/Kotlin jars) into your server's `mods/` folder
4. Start the server—`config/golemguard.json` will be generated automatically

## Configuration

Open `config/golemguard.json` and adjust values as needed:

```json
{
  "keyItem": "minecraft:stick",
  "requireCustomName": true,
  "allowIronGolems": true,
  "allowSnowGolems": true,
  "allowGuardians": true,
  "allowElderGuardians": true,
  "attackAllHostileMobs": true,
  "attackPlayers": true,
  "attackRange": 16.0,
  "tickRate": 20,
  "cooldownMs": 500,
  "checkBundles": true,
  "checkShulkerBoxes": false,
  "defaultKeyName": "DefaultKey",
  "messages": {
    "golemHired": "§aGolem hired with key %key%.",
    "golemAlreadyHired": "§6Golem already hired.",
    "instructionMessage": "§7Right-click with a renamed %item% to hire this golem.",
    "noKeyItem": "§cYou need a renamed %item% to hire golems.",
    "ironGolemDisabled": "§cIron golems cannot be hired.",
    "snowGolemDisabled": "§cSnow golems cannot be hired.",
    "guardianDisabled": "§cGuardians cannot be hired.",
    "elderGuardianDisabled": "§cElder guardians cannot be hired.",
    "requiresCustomName": "§cThe %item% needs a custom name to hire golems."
  },
  "enableDebugLogging": false
}
```

## License

This project is released into the **public domain** using the [Unlicense](https://unlicense.org).

**What this means:**
- ✅ Copy, modify, distribute, sell - do anything with this code
- ✅ No copyright restrictions whatsoever  
- ✅ No attribution required (though appreciated)
- ✅ Commercial use allowed
- ❌ No warranties or guarantees

See the [LICENSE](LICENSE) file for full details. 