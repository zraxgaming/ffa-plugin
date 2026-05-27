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
%zf_status%
```
