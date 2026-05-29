# Z-FFA v1.3.1 - Updates & Improvements

## Major Changes

### 🎫 License Change
- **OLD**: MIT License (no attribution required)
- **NEW**: CC-BY-NC-SA 4.0 (Attribution + ShareAlike)
- **Impact**: Forks and modifications must credit original author and link to original repository
- **See**: [LICENSE](../LICENSE) for full terms

### 📁 Configuration Reorganization
- **NEW**: Separate `kits.yml` file for kit management
- **MOVED**: All kit definitions from `config.yml` to `kits.yml`
- **BENEFIT**: Easier to manage and edit kits without touching main config
- **DOCS**: See [KITS.md](./KITS.md) for complete guide

## 🐛 Bug Fixes

### Lobby Items Rendering Issue
**Problem**: Lobby items sometimes failed to render/display in player inventory
**Root Cause**: Null ItemMeta handling, missing fallback logic
**Solution**:
- Added null-safe ItemMeta creation with fallback
- Implemented try-catch error handling
- Added debug logging for troubleshooting
- Validate items before adding to inventory

**Files Changed**: 
- `GuiManager.java`: `giveLobbyItems()` method
- `LobbyItemListener.java`: Added null checks

### Menu MSPT Spikes
**Problem**: Opening menus sometimes caused temporary MSPT spikes
**Root Cause**: Repeated profile lookups and string replacements per item
**Solution**:
- Cache profile data within menu operation
- Optimize placeholder replacement logic
- Add error handling to prevent menu crashes
- Reduce redundant queue size calculations

**Files Changed**:
- `GuiManager.java`: Optimized `playerPlaceholders()` and `replace()`
- `GuiManager.java`: Added try-catch to `openKits()`

## ⚡ Performance Optimizations

### GUI/Menu Rendering
- **Cache reuse**: Player profiles now retrieved once per menu instead of per item
- **String optimization**: Placeholder replacement with early validation
- **Error resilience**: Try-catch blocks prevent menu crashes
- **Fallback safety**: Null materials default to STONE

### Code Quality
- **Null checks**: Added comprehensive null safety throughout
- **Fallback values**: Config values have sensible defaults
- **Error logging**: Better debug information for troubleshooting
- **Code modularity**: Extract complex operations into focused methods

### Database
- No changes needed - already optimized with:
  - HikariCP connection pooling
  - Prepared statement caching
  - Async operations
  - Caffeine profile caching

## 🛠️ Developer Improvements

### New Utility Class
**`ConfigUtils.java`**: Centralized configuration access with fallback logic
```java
// Safe access to config values
String value = ConfigUtils.getString(section, "key", "fallback");
Material mat = ConfigUtils.getMaterial("DIAMOND_SWORD", Material.STONE);
```

### Better Error Handling
- Try-catch blocks in all listener event handlers
- Null-safe data access throughout
- Debug logging for troubleshooting
- Graceful fallbacks instead of crashes

### Code Organization
- **InventoryListener**: Split into focused handler methods
  - `handleKitSelection()`
  - `handleDuelPlayer()`
  - `handleOpenStatsTarget()`

## 📚 New Documentation

### [PERFORMANCE.md](./PERFORMANCE.md)
Complete guide to performance tuning:
- Optimization explanations
- Configuration recommendations
- Bottleneck solutions
- Monitoring tips
- Production best practices

### [KITS.md](./KITS.md)
Comprehensive kit configuration guide:
- Full kit structure explanation
- Multiple working examples
- Material and effect references
- In-game kit management commands
- Troubleshooting guide
- Best practices

## 🔧 Configuration Changes

### New File: `kits.yml`
```yaml
kits:
  nodebuff:
    display: "<gradient:red:gold>No Debuff</gradient>"
    icon: POTION
    settings:
      allow-regen: true
      allow-hunger: false
      speed-multiplier: 1.0
      max-health: 20.0
    items:
      - "DIAMOND_SWORD:1"
      - "ENDER_PEARL:16"
      # ... more items
```

### Updated: `config.yml`
- ✅ Removed: `kits:` section (moved to kits.yml)
- ✅ No other changes needed
- ✅ All old configs still work

### Updated: `plugin.yml`
- ✅ Added description with license info
- ✅ Added GitHub repository link

## Migration Guide

### For Existing Servers
1. **Backup your configs** (`config.yml`, `kits.yml`)
2. **Update plugin** to v1.3.1+
3. **Delete old configs** (or plugin will save defaults)
4. **Customize kits** in new `kits.yml`
5. **Run `/zffa reload`**
6. **Test thoroughly** before production

### Kits Migration
If you had custom kits in `config.yml`:
1. Copy your `kits:` section
2. Move to new `kits.yml` file
3. Ensure proper indentation
4. Run `/zffa reload`
5. Verify with `/zffa kit list`

## 🎯 Known Limitations & Solutions

### Limitation: MySQL Connection Pool
**Impact**: Large servers (100+ players) may need more connections
**Solution**: Increase `mysql.pool-size` in config.yml
```yaml
mysql:
  pool-size: 20  # Adjust based on concurrency
```

### Limitation: Async Profile Loading
**Impact**: New players may have slight delay getting first match
**Solution**: Profiles are cached after first load - not an issue
**Expected**: < 100ms for profile load

## Testing Performed ✅

- [x] Lobby items render correctly in all cases
- [x] Menus open without MSPT spikes
- [x] Kit selection works smoothly
- [x] Error handling prevents crashes
- [x] Null ItemMeta handled gracefully
- [x] Config loading robust with fallbacks
- [x] Database operations remain async
- [x] Debug mode provides useful info

## What's Next?

### Planned for v1.4.0
- Queue size update throttling (reduce DB queries)
- Async leaderboard loading
- Performance metrics command
- Plugin command usage analytics

### Community Feedback Welcome
- Performance issues on your server?
- Config questions or suggestions?
- Please report with details!

## Summary of Benefits

| Improvement | Benefit |
|---|---|
| Separate kits.yml | Easier kit management |
| Lobby item fixes | Improved user experience |
| Menu optimization | Reduced lag spikes |
| Better error handling | Fewer crashes |
| New documentation | Easier setup & troubleshooting |
| License clarity | Better community respect |
| ConfigUtils | Easier future development |

## Support

- 📖 Read the docs: `docs/` folder
- 🐛 Report issues on GitHub
- 💬 Ask questions in Discord/forums
- 🔧 Check `/zffa reload` after config changes
