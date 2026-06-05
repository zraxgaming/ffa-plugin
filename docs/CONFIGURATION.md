# Z-FFA Configuration

## Placeholder Rules

- `config.yml` and `menus.yml` use `%placeholder%`
- `messages.yml` uses `{placeholder}`

Common `%placeholder%` values:

| Placeholder | Meaning |
| --- | --- |
| `%player%` | Player name |
| `%elo%` | Current Elo |
| `%rank%` | Rank name |
| `%wins%` | Win count |
| `%losses%` | Loss count |
| `%kills%` | FFA kills |
| `%deaths%` | FFA deaths |
| `%streak%` | Current streak |
| `%status%` | Queue, match, FFA, or lobby status |
| `%online%` | Online players |
| `%queued%` | Total queued players |
| `%ffa_players%` | Players in FFA sessions |
| `%arenas%` | Total configured arenas |
| `%kit%` | Kit ID |
| `%kit_display%` | Kit display fallback |
| `%queue_size%` | Queue size for a kit |
| `%arena%` | Arena ID |
| `%arena_players%` | Players in one FFA arena |
| `%default_kit%` | First compatible FFA kit |
| `%position%` | Leaderboard position |

Common `{placeholder}` values:

| Placeholder | Meaning |
| --- | --- |
| `{player}` | Player name |
| `{target}` | Target player |
| `{leader}` | Party leader |
| `{kit}` | Kit ID |
| `{arena}` | Arena ID |
| `{type}` | `ranked` or `unranked` |
| `{reason}` | Match end reason |
| `{elo}` | Elo value |
| `{action}` | Action name |

PlaceholderAPI values:

```text
%zf_elo%
%zf_rank%
%zf_wins%
%zf_losses%
%zf_kills%
%zf_deaths%
%zf_kdr%
%zf_winrate%
%zf_streak%
%zf_status%
%zf_queued%
%zf_ffa_players%
```

## Lobby Items

Lobby items live in `config.yml -> lobby-items`.

Supported actions:

```text
OPEN_MAIN
OPEN_KITS
OPEN_RANKED
OPEN_UNRANKED_KITS
OPEN_UNRANKED
OPEN_STATS
OPEN_LEADERBOARD
OPEN_RANKS
OPEN_PARTY
OPEN_EVENT
LEAVE_QUEUE
```

There is no default `OPEN_FFA` lobby item. Add one only if you intentionally want an arena browser item.

## Menus

Useful menu settings:

```yaml
settings:
  close-menu-on-inert-click: true
  menu-refresh-seconds: 0

menus:
  kit-selector:
    center-items: true
    item-slots: [10, 11, 12, 13, 14, 15, 16, 20, 21, 22, 23, 24]
```

When `close-menu-on-inert-click` is enabled, filler panes, background slots, and no-action items close the menu instead of doing nothing.

Keep `menu-refresh-seconds` at `0` for best performance. Set it to `30` or higher only if live open-menu numbers are needed.

## Kits

Kits live in `kits.yml`.

```yaml
kits:
  nodebuff:
    display: "<gradient:red:gold>No Debuff</gradient>"
    icon: SPLASH_POTION
    settings:
      allow-regen: true
      allow-hunger: false
      speed-multiplier: 1.0
      max-health: 20.0
    items:
      - "DIAMOND_SWORD:1"
      - "ENDER_PEARL:16"
      - "SPLASH_POTION:healing:28"
    armor:
      helmet: "DIAMOND_HELMET:1"
      chestplate: "DIAMOND_CHESTPLATE:1"
      leggings: "DIAMOND_LEGGINGS:1"
      boots: "DIAMOND_BOOTS:1"
    effects:
      - "SPEED:infinite:1"
```

Useful commands:

```text
/zffa kit create <name> [display]
/zffa kit save <name> [display]
/zffa kit seticon <name> <material>
/zffa kit delete <name>
/zffa kit list
/zffa kiteditor
```

## Arenas

Duels need `spawn1` and `spawn2`.

FFA needs at least one `ffa-spawns` entry and a compatible kit. If an arena has no kit list, all kits are allowed.

Useful commands:

```text
/zffa arena create <name>
/zffa arena <name> setspawn1
/zffa arena <name> setspawn2
/zffa arena <name> addffaspawn
/zffa arena <name> addkit <kit>
/zffa arena <name> enable
```

## Database

Use SQLite for local testing or small servers:

```yaml
settings:
  database-type: "SQLITE"
```

Use MySQL for larger servers or multiple backends:

```yaml
settings:
  database-type: "MYSQL"
  mysql:
    pool-size: 6
    minimum-idle: 1
```
