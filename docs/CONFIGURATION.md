# Z-FFA Configuration Reference

## Files

```text
config.yml  - database, protections, lobby items, kits
menus.yml   - GUI titles/items/lore
arenas.yml  - lobby, duel spawns, FFA spawns, kit restrictions
```

## Arenas

Duels require:

- `spawn1`
- `spawn2`

FFA requires:

- at least one `ffa-spawns` entry

Kit restrictions:

- Empty `kits` list means all kits are allowed.
- Non-empty `kits` list means only those kits are allowed.

## Multiverse-Core

Z-FFA uses normal Bukkit world names in saved locations. With Multiverse-Core:

1. Create or import the world.
2. Make sure the world is loaded.
3. Set Z-FFA locations in that world.
4. Avoid renaming the world unless you update `arenas.yml`.

## FFA Combat

```yaml
settings:
  ffa:
    require-mutual-hit: true
    fight-request-expire-seconds: 10
    kill-heal-hearts: 20.0
    refill-hunger-on-kill: true
```

When `require-mutual-hit` is enabled, the first hit sends a fight request and is cancelled. If the target hits back before the request expires, damage is allowed.

## VIP Arenas

Mark an arena VIP-only:

```text
/zffa arena arena1 vip true
```

Players need:

```text
zf.viparena
```

They can join the first ready VIP FFA arena with:

```text
/ffa viparena
```

## Placeholders

```text
%zf_elo%
%zf_rank%
%zf_wins%
%zf_losses%
%zf_kills%
%zf_deaths%
%zf_streak%
%zf_vouchers%
%zf_killboost%
%zf_status%
%zf_vault_balance%
%zf_vault_balance_formatted%
%zf_group%
%zf_nickname%
```

## Debug

Enable or disable debug output from the plugin:

```yaml
settings:
  debug-enabled: false
```

## Integration

Z-FFA detects and optionally integrates with the following plugins when installed:

- `PlaceholderAPI` for placeholder expansion
- `Vault` for economy balance placeholders
- `LuckPerms` for primary group placeholders
- `Essentials` for nickname placeholders
- `Multiverse-Core` for world support when used with multiple worlds

## Streak vouchers

Streak vouchers protect a player's streak from resetting when they lose. Give or set vouchers with admin commands, and enable protection in config.

```yaml
settings:
  streak:
    protection:
      enabled: true
```

Admin commands:

```text
/zffa voucher give <player> <amount>
/zffa voucher set <player> <amount>
/zffa voucher remove <player> <amount>
```

## Kill boost

Kill boosts are a consumable boost count that can trigger rewards on a kill. Configure reward commands and give boosts via admin commands.

```yaml
settings:
  kill-boost:
    enabled: false
    consume-on-kill: true
    reward-commands:
      - "give %player% gold_nugget 4"
```

Admin commands:

```text
/zffa killboost give <player> <amount>
/zffa killboost set <player> <amount>
/zffa killboost remove <player> <amount>
```

## GUI Protection

The plugin blocks shift-click and drag moves into Z-FFA menus to prevent accidental item movement and reduce server tick lag caused by GUI spam.
