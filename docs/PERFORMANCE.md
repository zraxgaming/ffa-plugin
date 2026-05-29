# Z-FFA Performance & Optimization Guide

## Overview
This document outlines the optimizations made to the Z-FFA plugin to reduce MSPT spikes, improve database efficiency, and enhance overall performance.

## Key Optimizations Implemented

### 1. GUI/Menu MSPT Reduction ✓
- **Cached Player Profiles**: Profile lookups are now cached and reused within menu operations instead of querying multiple times
- **Reduced String Replacements**: Placeholder replacement is now more efficient with early validation
- **Error Handling**: Try-catch blocks prevent menu crashes and add fallback logic
- **Queue Size Caching**: Avoid recalculating queue sizes during kit selection

### 2. Lobby Items Rendering ✓
- **Null-Safe ItemMeta Handling**: Added proper null checks and fallback ItemFactory creation
- **Exception Handling**: Wrapped lobby item giving in try-catch with debug logging
- **Fallback Materials**: Air items and null materials now fallback to STONE
- **Validation**: Items are validated before being added to inventory

### 3. Database Optimizations ✓
- **HikariCP Connection Pooling**: Already configured with prepared statement caching
- **Async Profile Loading**: All database operations are async via CompletableFuture
- **Caffeine Caching**: 20-minute profile cache reduces unnecessary database hits
- **Dirty Flag Flushing**: Only modified profiles are saved to database on autosave

### 4. Fallback Logic Added ✓
- **Configuration Null Checks**: All config accesses have fallback values
- **Player Null Handling**: Fallbacks for null player names, status, etc.
- **Equipment Validation**: Armor pieces and items fallback to air if null
- **Menu Item Validation**: Empty lore lists fallback to single descriptive line

### 5. Code Modularity Improvements ✓
- **Extracted Methods**: Complex menu operations split into focused methods
  - `handleKitSelection()`: Kit selection logic
  - `handleDuelPlayer()`: Duel invitation logic
  - `handleOpenStatsTarget()`: Stats viewing logic
- **Reusable Utilities**: Error handling patterns standardized across listeners

## Configuration Recommendations

### For Performance Tuning

```yaml
# config.yml
settings:
  cache-expire-minutes: 20    # Increase to 30+ for larger servers
  autosave-minutes: 5         # Reduce to 3 for more frequent saves, increase for less DB load
  
  mysql:
    pool-size: 10             # Adjust based on server player count
                              # Rule: 2-4x max concurrent players
```

### Database Selection

- **SQLite**: Single-player/small servers (< 20 concurrent players)
- **MySQL**: Multi-player/large servers (> 20 concurrent players) with better load distribution

## Known Performance Bottlenecks & Solutions

### Issue: High MSPT When Opening Menus
**Solution**: Menus now cache profile data within the operation instead of querying repeatedly
**If Still Occurring**: 
- Check MySQL pool size and connection availability
- Verify database is on same network (low latency)
- Reduce concurrent menu opens (message players to wait)

### Issue: Lobby Items Not Rendering
**Solution**: Implemented robust null checking and ItemFactory fallbacks
**If Still Occurring**:
- Check `/debug` logs for specific item errors
- Validate material names in config.yml
- Ensure server has 1.21+ version

### Issue: General Plugin Lag Spikes
**Causes & Solutions**:
1. **Database Connection Issues**: Increase pool size or switch to better hosting
2. **Too Many Kits**: Simplify kit configurations (fewer potions/effects)
3. **Arena Size**: Reduce arena sizes or number of active matches
4. **PlaceholderAPI**: Disable if not heavily used

## Monitoring Performance

### Using Debug Mode
```yaml
# config.yml
settings:
  debug-enabled: true
```

Then monitor console for:
- `[DEBUG]` logs showing config/loading issues
- `[WARNING]` logs showing potential problems

### Measuring MSPT Impact
1. Use `/forge tps` or `/paper tps` (if available)
2. Queue opening = 1-2 MSPT increase
3. Match start = 1-2 MSPT increase
4. Normal operation = < 0.5 MSPT from plugin

## Best Practices

1. **For Production Servers**:
   - Use MySQL with adequate connection pool
   - Enable debug-enabled: false
   - Set autosave-minutes to 5 or higher
   - Monitor first week of operation

2. **For Development/Testing**:
   - Use SQLite for simplicity
   - Enable debug-enabled: true
   - Use shorter autosave-minutes for testing
   - Watch console logs closely

3. **For Large Servers**:
   - Increase cache-expire-minutes to 30-60
   - Use MySQL with pool-size: 20+
   - Consider separating FFA/Duel arenas to different arenas.yml sections
   - Monitor database query times regularly

## Future Optimization Opportunities

1. Lazy-load leaderboard (only top 10, not all profiles)
2. Batch database saves (combine multiple updates)
3. Async kit template building on startup
4. Queue size update throttling (cache queue sizes for 1 second)

## Troubleshooting Checklist

- [ ] Debug logs show no errors when opening menus
- [ ] Lobby items appear in player inventory on join
- [ ] Kit selector loads without null pointer exceptions
- [ ] Database connection stable (check MySQL/SQLite)
- [ ] Memory usage stable (no OOM errors)
- [ ] MSPT stays < 1ms for plugin operations
