# Z-FFA Configuration Reference

## Files

```text
config.yml    - database, protections, lobby items, match timing
messages.yml  - player-facing messages
menus.yml     - GUI titles, lore, item labels
arenas.yml    - lobby, duel spawns, FFA spawns, kit restrictions
kits.yml      - kit data
cosmetics.yml - kill effects and player selections
```

## Placeholder Rules

- `config.yml` and `menus.yml` use `%placeholder%`
- `messages.yml` uses `{placeholder}`

See [PLACEHOLDERS.md](PLACEHOLDERS.md) for the full list.

## Messages

`messages.yml` is editable, so server owners can change queue, duel, party, admin, and leave text without touching code.

Example:

```yaml
queue.joined: "<green>Queued for <white>{kit}</white> (<white>{type}</white>)."
leave.nothing: "<gray>Nothing to leave."
```

## Lobby Items

Lobby items are configured under `config.yml -> lobby-items`.

Supported actions:

- `OPEN_MAIN`
- `OPEN_KITS`
- `OPEN_RANKED`
- `OPEN_UNRANKED_KITS`
- `OPEN_UNRANKED`
- `OPEN_FFA_ARENAS`
- `OPEN_STATS`
- `OPEN_LEADERBOARD`
- `OPEN_RANKS`
- `OPEN_COSMETICS`
- `OPEN_KILL_EFFECTS`
- `OPEN_PARTY`
- `OPEN_EVENT`
- `LEAVE_QUEUE`

## Menu Files

`menus.yml` controls:

- main hub menu
- admin management menu
- GUI kit editor menu
- FFA arena browser
- kit selector layouts
- centered queue item placement
- stats menu content
- leaderboard formatting
- party menu labels
- kill effects

Useful menu settings:

```yaml
menus:
  kit-selector:
    center-items: true
    item-slots: [10, 11, 12, 13, 14, 15, 16]
```

If `item-slots` is omitted, Z-FFA centers entries row by row.

## Cosmetics

`cosmetics.yml` controls kill effects.

Kill effect example:

```yaml
kill-effects:
  flame:
    display: "<red>Flame Ring</red>"
    icon: BLAZE_POWDER
    particle: FLAME
    sound: ENTITY_BLAZE_SHOOT
    count: 30
```

Permissions:

- `zf.cosmetic.*`
- `zf.cosmetic.killeffect.*`
- `zf.cosmetic.killeffect.<id>`

## Arena Rules

Duels need:

- `spawn1`
- `spawn2`

FFA needs:

- at least one `ffa-spawns` entry
- at least one compatible kit

If the arena kit list is empty, all kits are allowed.

## Combat And Protection

```yaml
settings:
  auto-update-configs: true
  update-check:
    enabled: true
    auto-download: true
    url: "https://api.github.com/repos/zraxgaming/ffa-plugin/releases/latest"
  queue:
    ping-range:
      enabled: true
      max-difference: 80
      bypass-after-seconds: 30
  ffa:
    death-messages:
      enabled: true
      broadcast: true
      actionbar: true
    require-mutual-hit: true
    fight-request-expire-seconds: 10
    kill-heal-hearts: 20.0
    refill-hunger-on-kill: true
```

`auto-update-configs` only adds missing defaults. Existing values and player data are not overwritten.
`update-check.auto-download` downloads the latest release jar into Bukkit's configured update folder. It does not replace files while the server is running; restart the server to apply the downloaded update.

```yaml
settings:
  block-commands-in-match: true
  blocked-match-commands-bypass:
    - "/ffa leave"
    - "/duel leave"
    - "/msg"
    - "/r"
```

## Database

Use SQLite for light testing or MySQL for larger servers.

```yaml
settings:
  database-type: "SQLITE"
```

## Reloading

After editing configs:

```text
/zffa reload
```

This reloads config, messages, menus, kits, arenas, and GUI templates.
