# Golem Guard

[![Fabric API](https://img.shields.io/badge/modloader-Fabric-1976d2?style=flat-square&logo=fabricmc&logoColor=white)](https://fabricmc.net/) 
[![Minecraft](https://img.shields.io/badge/minecraft-1.21.7-brightgreen?style=flat-square)](https://minecraft.net/) 
[![License](https://img.shields.io/badge/license-CC0%201.0-blue?style=flat-square)](LICENSE)

A server-side Fabric mod that allows players to hire Iron and Snow Golems as personal guards using configurable key items. **No client-side installation required!**

## ✨ Features

- 🤖 **Hire Iron & Snow Golems** - Convert any golem into a guard with a renamed key item
- 🔑 **Key-Based Access** - Only players with matching named items can control the golems
- 🛡️ **Base Protection** - Guard golems attack hostile mobs and unauthorized players
- ⚙️ **Highly Configurable** - Customize everything via JSON config
- 🗃️ **Bundle Support** - Hide keys in bundles for stealth
- 🌐 **Server-Only** - Works without any client-side mods
- ⚡ **Performance Optimized** - Configurable tick rates and smart targeting

## 🎮 How to Use

### Hiring a Golem

1. **Get a key item** (default: stick)
2. **Rename it** in an anvil (e.g., "MyBase")
3. **Right-click any Iron or Snow Golem** with the renamed item
4. **Success!** The golem is now hired and will guard the area

### Key System

- **Same key name** = Access to the same golems
- **Share keys** by giving players items with the same name
- **Hide keys** in bundles to keep them secret
- **Real-time recognition** - Get a key mid-combat and golems stop attacking instantly

### Guard Behavior

- ⚔️ **Attacks hostile mobs** (zombies, skeletons, creepers, etc.)
- 👥 **Attacks unauthorized players** (configurable)
- 🛡️ **Protects key holders** - Anyone with the matching key is safe
- 🏃 **16-block detection range** (configurable)
- 🔄 **Never despawns** - Guard golems are persistent

## 📦 Installation

### Server Installation

1. **Download** the latest release
2. **Install Fabric Loader** for Minecraft 1.21.7
3. **Install Fabric API** (required dependency)
4. **Install Fabric Language Kotlin** (required dependency)
5. **Place the mod JAR** in your server's `mods/` folder
6. **Start the server** - Config will be auto-generated

### Client Installation

**Not required!** Players can join and use the mod without installing anything.

## ⚙️ Configuration

The mod auto-generates `config/golemguard.json` with these options:

```json
{
  "keyItem": "minecraft:stick",           // What item to use as keys
  "requireCustomName": true,              // Must rename the key item
  "allowIronGolems": true,                // Can hire iron golems
  "allowSnowGolems": true,                // Can hire snow golems
  "attackAllHostileMobs": true,           // Attack all hostile mobs
  "attackPlayers": true,                  // Attack unauthorized players
  "attackRange": 16.0,                    // Detection range in blocks
  "tickRate": 20,                         // AI update frequency (ticks)
  "cooldownMs": 500,                      // Interaction cooldown (ms)
  "checkBundles": true,                   // Look for keys in bundles
  "checkShulkerBoxes": false,             // Look in shulker boxes (security risk!)
  "enableDebugLogging": false             // Show debug messages
}
```

### 📝 Message Customization

All player messages can be customized in the config:

```json
{
  "messages": {
    "golemHired": "§aGolem hired with key %key%.",
    "golemAlreadyHired": "§6Golem already hired as %key%.",
    "instructionMessage": "§7Right-click with a renamed %item% to hire this golem."
  }
}
```

**Placeholders:**
- `%key%` - The key name (e.g., "MyBase")
- `%item%` - The key item name (e.g., "Stick")

See [CONFIG.md](CONFIG.md) for complete configuration documentation.

## 🛠️ Development

### Building from Source

```bash
git clone https://github.com/yourusername/golem-guard.git
cd golem-guard
./gradlew build
```

The built JAR will be in `build/libs/`.

### Project Structure

```
src/main/kotlin/guard/my/stuff/guardmystuff/
├── GolemGuardMod.kt      # Main mod entry point
├── Config.kt             # Configuration system
├── EventRegistry.kt      # Player interaction events
├── TickHandler.kt        # AI and targeting logic
└── GuardComponent.kt     # Golem data storage
```

### Adding Features

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes** with tests
4. **Update documentation** if needed
5. **Submit a pull request**

## 🐛 Troubleshooting

### Common Issues

**Golems not responding?**
- Check if `attackPlayers`/`attackAllHostileMobs` are enabled
- Verify the key item name matches exactly
- Check server logs for errors

**Performance problems?**
- Increase `tickRate` in config (higher = less frequent checks)
- Reduce `attackRange` to limit detection area
- Enable `enableDebugLogging` to see what's happening

**Players can't hire golems?**
- Ensure they have the correct `keyItem` type
- Check that the item has a custom name (if `requireCustomName` is true)
- Verify golem types are enabled (`allowIronGolems`/`allowSnowGolems`)

### Getting Help

- 📋 **Check existing issues** for known problems
- 🐞 **Report bugs** with your config file and server logs  
- 💡 **Suggest features** via issues
- 💬 **Join discussions** for community support

## 📊 Version Compatibility

| Minecraft | Fabric Loader | Fabric API | Status |
|-----------|---------------|------------|--------|
| 1.21.7    | 0.16.14+      | 0.96.4+    | ✅ Supported |

## 🤝 Contributing

Contributions are welcome! Please:

1. **Fork** the repository
2. **Create a feature branch** (`git checkout -b feature/amazing-feature`)
3. **Make your changes** with tests
4. **Update documentation** if needed
5. **Submit a pull request**

### Contributors

- Initial implementation by the community

## 📄 License

This project is licensed under CC0 1.0 Universal (Public Domain) - see the [LICENSE](LICENSE) file for details.

This means you can copy, modify, distribute and perform the work, even for commercial purposes, all without asking permission.

## 🙏 Acknowledgments

- **Fabric Team** - For the amazing modding framework
- **Mojang** - For Minecraft
- **Community** - For feedback and suggestions

---

**Made with ❤️ for the Minecraft community** 