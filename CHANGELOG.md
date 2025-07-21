# Changelog

## [Unreleased]

### Added
- **Smart Snow Golem Targeting**: Snow golems automatically switch targets when line-of-sight is blocked by terrain
- **Fire Snowball Effects**: Snowballs passing through fire or lava ignite targets, letting you create a proper artillery cannon with flaming snowballs
- If you want a proper laser cannon, you can now trap Guardians and hire them as guards. 
- **Guardian and Elder Guardian Support**: Can now hire Guardians and Elder Guardians as guards
- **Enhanced Guardian Combat**: Guardians in water use beam attacks with extended range instead of swimming to targets
- **Extended Guardian Range**: Water-based Guardians get 1.5x attack range for both land and water targets, making them great guards for your underwater base or future themed bases
- **Reusable Key Items**: Key items no longer consumed when hiring - remain in inventory for multiple uses
- **Line-of-Sight Checking**: Advanced raycast-based targeting for improved combat AI

### Improved
- **Guardian AI**: Prioritizes targets with clear line-of-sight for beam attacks
- **Targeting Logic**: More efficient entity filtering and distance calculations
- **Debug Logging**: Enhanced messages showing Guardian attack modes and target locations
- **Code Organization**: Cleaner comments focused on functionality rather than changes

### Configuration
- Added `allowGuardians` and `allowElderGuardians` options
- Added `guardianDisabled` and `elderGuardianDisabled` messages
- Added `defaultKeyName` for unnamed keys

### Technical
- Optimized line-of-sight calculations using Minecraft's raycast system
- Improved Guardian navigation control to prevent unwanted swimming
- Enhanced snow golem targeting timer system for blocked shots
- Removed deprecated mixins that were causing runtime errors 