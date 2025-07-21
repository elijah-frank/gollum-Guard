# Golem Guard

[![Fabric API](https://img.shields.io/badge/modloader-Fabric-1976d2?style=flat-square&logo=fabricmc&logoColor=white)](https://fabricmc.net/)  
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.7-brightgreen?style=flat-square)](https://minecraft.net/)  
[![License](https://img.shields.io/badge/license-Unlicense-blue?style=flat-square)](LICENSE)

## Introduction

I developed **Golem Guard** as a server-only Fabric mod for Minecraft 1.21.7/1.21.8 to let players hire Iron and Snow Golems as personal guards. No client-side installation is required—anyone can join vanilla and immediately wield their golems.

I originally built this mod for my Minecraft server network, **BovisGL** (more info at [bovisgl.xyz](https://bovisgl.xyz)), because our Anarchy game mode has no griefing rules and I wanted a way for players to protect their stuff without burying everything underground.

## Features

- **Key-Based Hiring**  
  Right-click an Iron or Snow Golem with a renamed key item to convert it into your guard.

- **Authorization & Sharing**  
  Any player who has the correct key item with the matching name in their inventory will be ignored by your golems and not attacked. Share keys to let others be recognized as friendly.

- **Configurable Targeting**  
  Guards attack hostile mobs and any players who do not have the key in their inventory. All targeting options are adjustable via JSON.

- **Performance-Focused**  
  Adjustable tick rates and detection ranges keep server load minimal.

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
  "attackAllHostileMobs": true,
  "attackPlayers": true,
  "attackRange": 16.0,
  "tickRate": 20,
  "cooldownMs": 500,
  "checkBundles": true,
  "checkShulkerBoxes": false,
  "messages": {
    "golemHired": "§aGolem hired with key %key%.",
    "golemAlreadyHired": "§6Golem already hired .",
    "golemBelongsToOther": "§6This golem is already hired.",
    "instructionMessage": "§7Right-click with a renamed %item% to hire this golem.",
    "noKeyItem": "§cYou need a renamed %item% to hire golems.",
    "ironGolemDisabled": "§cIron golems cannot be hired.",
    "snowGolemDisabled": "§cSnow golems cannot be hired.",
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