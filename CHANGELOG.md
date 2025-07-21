# Changelog

## [Unreleased]

### Added
- **Guardian and Elder Guardian Support**: Can now hire Guardians and Elder Guardians as guards
- **Smart Snow Golem Targeting**: Snow golems automatically switch targets when line-of-sight is blocked by terrain
- **Enhanced Guardian Combat**: Guardians in water use beam attacks with extended range instead of swimming to targets
- **Fire Snowball Effects**: Snowballs passing through fire/lava ignite targets; soul fire sources create soul fire blocks
- **Reusable Key Items**: Key items no longer consumed when hiring - remain in inventory for multiple uses
- **Extended Guardian Range**: Water-based Guardians get 1.5x attack range for both land and water targets
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