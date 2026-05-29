# Z-FFA Kits Configuration Guide

## Overview

Kits are now configured in a **separate `kits.yml` file** instead of in `config.yml`. This makes kit management easier and keeps configurations organized.

## File Location

```
plugins/Z-FFA/kits.yml
```

## Kit Structure

Each kit requires the following:

```yaml
kits:
  kit-id:                    # Unique identifier (lowercase, no spaces, hyphens OK)
    display: "<color>Name</color>"  # Display name for players (MiniMessage format)
    icon: MATERIAL            # GUI icon material name
    settings:                # Kit-specific settings
      allow-regen: true      # Natural regeneration allowed?
      allow-hunger: false    # Hunger damage allowed?
      speed-multiplier: 1.0  # Speed effect multiplier
      max-health: 20.0       # Max health in half-hearts
    items:                   # Items given to player
      - "ITEM_NAME:amount"
      - "POTION_TYPE:healing:amount"
    armor:                   # Armor pieces
      helmet: "DIAMOND_HELMET:1"
      chestplate: "DIAMOND_CHESTPLATE:1"
      leggings: "DIAMOND_LEGGINGS:1"
      boots: "DIAMOND_BOOTS:1"
    effects:                 # Potion effects (optional)
      - "SPEED:infinite:1"
      - "RESISTANCE:infinite:0"
```

## Examples

### Example 1: No Debuff Kit

```yaml
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
    - "SPLASH_POTION:healing:28"
    - "COOKED_BEEF:64"
  armor:
    helmet: "DIAMOND_HELMET:1"
    chestplate: "DIAMOND_CHESTPLATE:1"
    leggings: "DIAMOND_LEGGINGS:1"
    boots: "DIAMOND_BOOTS:1"
```

### Example 2: Speed/Strength Kit

```yaml
speedii:
  display: "<gradient:#00ff00:#ffff00>Speed II</gradient>"
  icon: FEATHER
  settings:
    allow-regen: false
    allow-hunger: true
    speed-multiplier: 1.5
    max-health: 20.0
  items:
    - "IRON_SWORD:1"
    - "ENDER_PEARL:32"
    - "COOKED_BEEF:64"
  armor:
    helmet: "IRON_HELMET:1"
    chestplate: "IRON_CHESTPLATE:1"
    leggings: "IRON_LEGGINGS:1"
    boots: "IRON_BOOTS:1"
  effects:
    - "SPEED:infinite:1"
    - "STRENGTH:infinite:0"
```

### Example 3: Tank Kit

```yaml
tank:
  display: "<gradient:#8B0000:#FF0000>Tank</gradient>"
  icon: SHIELD
  settings:
    allow-regen: false
    allow-hunger: false
    speed-multiplier: 0.9
    max-health: 30.0
  items:
    - "DIAMOND_SWORD:1"
    - "COOKED_BEEF:64"
  armor:
    helmet: "DIAMOND_HELMET:1"
    chestplate: "DIAMOND_CHESTPLATE:1"
    leggings: "DIAMOND_LEGGINGS:1"
    boots: "DIAMOND_BOOTS:1"
  effects:
    - "RESISTANCE:infinite:1"
```

## Material Names Reference

See: [Spigot Material Docs](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)

Common items:
- `DIAMOND_SWORD`, `IRON_SWORD`, `STONE_SWORD`
- `BOW`, `CROSSBOW`
- `ENDER_PEARL`, `SNOWBALL`
- `SHIELD`, `FISHING_ROD`
- `DIAMOND_HELMET`, `IRON_HELMET`, `LEATHER_HELMET`
- `COOKED_BEEF`, `COOKED_CHICKEN`, `BREAD`
- `SPLASH_POTION`, `LINGERING_POTION`

## MiniMessage Format (Display Names)

Format guide: [Adventure Docs](https://docs.adventure.kyori.net/minimessage/format.html)

Examples:
```
<red>Red Text</red>
<gradient:red:blue>Gradient Text</gradient>
<bold>Bold Text</bold>
<italic>Italic Text</italic>
<underlined>Underlined</underlined>
<strikethrough>Strikethrough</strikethrough>
```

## Item Format

Items use a legacy format: `MATERIAL:AMOUNT`

For potions: `SPLASH_POTION:healing:amount`

Examples:
- `DIAMOND_SWORD:1` → 1 diamond sword
- `ENDER_PEARL:16` → 16 ender pearls
- `COOKED_BEEF:64` → 64 cooked beef
- `SPLASH_POTION:healing:28` → 28 healing splash potions

## Potion Effects Format

Format: `EFFECT_NAME:duration:amplifier`

Where:
- `EFFECT_NAME` = Effect type (see list below)
- `duration` = Duration in ticks (or `infinite` for infinite)
- `amplifier` = Effect level (0 = I, 1 = II, 2 = III, etc)

Common effects:
- `SPEED` - Speed effect
- `STRENGTH` - Strength effect
- `RESISTANCE` - Damage resistance
- `HEALTH_BOOST` - Extra health
- `HASTE` - Faster mining/attacking
- `JUMP_BOOST` - Higher jumping
- `REGENERATION` - Health regeneration

Example:
```yaml
effects:
  - "SPEED:infinite:1"        # Speed II, infinite duration
  - "STRENGTH:infinite:0"     # Strength I, infinite duration
  - "JUMP_BOOST:infinite:1"   # Jump Boost II, infinite
```

## Settings Explained

### allow-regen
- `true`: Players can heal with abilities/items
- `false`: Natural regeneration disabled

### allow-hunger
- `true`: Hunger bar can deplete and cause damage
- `false`: Players won't take hunger damage

### speed-multiplier
- `1.0`: Normal walking speed
- `1.5`: 50% faster
- `0.9`: 10% slower

### max-health
- `20.0`: 10 hearts (default)
- `30.0`: 15 hearts
- `40.0`: 20 hearts (maximum)

## Managing Kits In-Game

### Create a Kit from Inventory
```
/zffa kit create <name> [display]
```
Creates a kit from your current inventory and armor.

### Set Kit Icon
```
/zffa kit seticon <name> <material>
```
Changes the GUI icon for a kit.

### Delete a Kit
```
/zffa kit delete <name>
```
Permanently removes a kit.

### List All Kits
```
/zffa kit list
```
Shows all loaded kits.

## Troubleshooting

### Kit Not Loading
1. Check console for errors
2. Verify YAML syntax (no duplicate `: ` colons)
3. Ensure material names are valid (check docs)
4. Run `/zffa reload` to reload

### Kit Icon Wrong
1. Use valid Material name from [Spigot docs](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html)
2. Use `/zffa kit seticon <name> <material>` to fix

### Items Not Given
1. Check item format: `MATERIAL:AMOUNT`
2. Verify inventory isn't full when joining
3. Check player permissions: `zf.kit.<kitname>` or `zf.kit.*`

### Display Name Not Showing Colors
1. Ensure format is valid MiniMessage syntax
2. Check for typos in color names
3. Remember format: `<color>text</color>`

## Best Practices

1. **Keep Kit IDs Simple**: Use lowercase, no spaces (e.g., `nodebuff`, `speed-ii`, `tank`)
2. **Balance Health**: Most kits should be 20.0 (10 hearts), tank kits can be 30.0
3. **Use Meaningful Names**: Players should instantly know what kit does
4. **Test Thoroughly**: Create kit, join queue, test gameplay before enabling on production
5. **Version Migrations**: Keep old kits.yml as backup before major changes

## Advanced Tips

### Custom Display Names Per Kit
Use MiniMessage gradients for unique kits:
```yaml
display: "<gradient:#FF0000:#00FF00>Rainbow Kit</gradient>"
```

### Multi-Effect Kits
Combine multiple effects for unique gameplay:
```yaml
effects:
  - "SPEED:infinite:1"
  - "STRENGTH:infinite:0"
  - "RESISTANCE:infinite:0"
  - "JUMP_BOOST:infinite:1"
```

### Arena-Specific Kits
Create different kit variations for different arenas:
- `nodebuff` (all arenas)
- `nodebuff-nether` (nether arena only)
- `ultra-nodebuff` (tournament mode)

## Migration from Old config.yml

If you had kits in `config.yml`:

1. Backup your configs
2. Copy kits section to new `kits.yml`
3. Ensure format matches above structure
4. Remove kits section from `config.yml`
5. Run `/zffa reload`
6. Verify kits load with `/zffa kit list`
