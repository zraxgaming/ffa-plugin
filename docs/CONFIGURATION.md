# Z-FFA Configuration Reference

## Files

```text
config.yml    - database, protections, lobby items, match timing
messages.yml  - player-facing messages
menus.yml     - GUI titles, lore, item labels
arenas.yml    - lobby, duel spawns, FFA spawns, kit restrictions
kits.yml      - kit data
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

- `OPEN_KITS`
- `OPEN_UNRANKED_KITS`
- `OPEN_STATS`
- `OPEN_LEADERBOARD`
- `OPEN_PARTY`
- `OPEN_EVENT`
- `LEAVE_QUEUE`

## Menu Files

`menus.yml` controls:

- kit selector layouts
- stats menu content
- leaderboard formatting
- party menu labels

## Arena Rules

Duels need:

- `spawn1`
- `spawn2`

FFA needs:

- at least one `ffa-spawns` entry

If the arena kit list is empty, all kits are allowed.

## Combat And Protection

```yaml
settings:
  ffa:
    require-mutual-hit: true
    fight-request-expire-seconds: 10
    kill-heal-hearts: 20.0
    refill-hunger-on-kill: true
```

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
