# Z-FFA Configuration Reference

## Files

```text
config.yml    - database, protections, lobby items, match timing
messages.yml  - player-facing messages
menus.yml     - GUI titles, lore, item labels
arenas.yml    - lobby, duel spawns, FFA spawns, kit restrictions
kits.yml      - kit data
zffa-proxy.properties - Velocity/Bungee coordinator settings
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
- `OPEN_PARTY`
- `OPEN_EVENT`
- `LEAVE_QUEUE`

The default lobby items are direct actions for ranked, unranked, party, stats, event, and leaderboard. The optional `OPEN_MAIN` hub menu and `OPEN_FFA_ARENAS` browser still exist, but they are no longer placed in the default hotbar because they are redundant on small or not-yet-configured servers.

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

Useful menu settings:

```yaml
menus:
  kit-selector:
    center-items: true
    item-slots: [10, 11, 12, 13, 14, 15, 16]
```

If `item-slots` is omitted, Z-FFA centers entries row by row.

Dead menu clicks:

```yaml
settings:
  close-menu-on-inert-click: true
```

When this is enabled, filler panes, background slots, and menu items without an action close the menu instead of doing nothing.

## Arena Rules

Duels need:

- `spawn1`
- `spawn2`

FFA needs:

- at least one `ffa-spawns` entry
- at least one compatible kit

If the arena kit list is empty, all kits are allowed.

If no ready FFA arenas exist, the FFA arena browser closes and sends `ffa.no-ready-arenas` instead of opening an empty filler menu.

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

Menu refreshes are disabled by default:

```yaml
settings:
  menu-refresh-seconds: 0
```

Set this to `30` or higher only if you want open kit, FFA, and leaderboard menus to update while players keep them open.

## Proxy Coordinator

The Velocity and Bungee/Waterfall proxy jars generate `zffa-proxy.properties` on first proxy start.

```properties
enabled=true
channel=zffa:main
queue.enabled=true
queue.max-size-per-kit=500
queue.require-free-arena=true
queue.connect-delay-millis=1000
capacity.stale-seconds=15
server-selection.min-free-arenas=1
fallback-to-source=false
```

- `channel` must match backend `settings.proxy.channel`.
- `queue.enabled` lets the proxy accept or ignore backend queue requests.
- `queue.require-free-arena` keeps queued players waiting until a backend reports free arena capacity.
- `queue.max-size-per-kit` protects the proxy from unbounded queue growth.
- `capacity.stale-seconds` removes backend capacity reports that stopped updating.
- `server-selection.min-free-arenas` controls how much free capacity a backend must report before it can receive a new proxy-started duel.
- `fallback-to-source=false` prevents the proxy from starting a duel on the sender backend when no backend has reported capacity.

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
