# Z-FFA Core

[![Build](https://img.shields.io/github/actions/workflow/status/zraxgaming/ffa-plugin/build.yml?branch=main&label=build)](https://github.com/zraxgaming/ffa-plugin/actions)
[![Release](https://img.shields.io/github/v/release/zraxgaming/ffa-plugin?label=release)](https://github.com/zraxgaming/ffa-plugin/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20%20%7C%201.21%20%7C%201.26.x-2ea44f)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/zraxgaming/ffa-plugin)](LICENSE)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue?logo=paypal)](https://www.paypal.com/ncp/payment/ZXN242B97VA24)

Professional FFA and 1v1 duel core for `xyz.zcraft.studios`, packaged as version-targeted backend and proxy install bundles on Java 17+.

Originally built as a private ZCraft Studios network plugin, now documented for cleaner setup, easier config editing, and smoother server operations.

## Highlights

- FFA queue and live arena support
- Main hub menu and FFA arena browser
- 1v1 duel matchmaking by kit
- Ping-range matchmaking for fairer queues
- GUI-based admin kit editor
- Party queueing for duels and party FFA
- Configurable FFA death messages
- Editable `messages.yml` for server-specific wording
- Configurable menus, centered queue layouts, and lobby items with placeholders
- Non-destructive default config updates on startup/reload
- Startup update checker with optional release jar download
- SQLite and MySQL storage support
- PlaceholderAPI expansion support
- Optional proxy-assisted backend mode for network queue routing and capacity reports
- GitHub Actions build matrix for Minecraft `1.20`, `1.21`, `1.26.0`, and `1.26.1`

## Download

GitHub Actions uploads one ZIP per Minecraft target. Each ZIP intentionally contains only plugin jars, the license, and a short `README.txt` that points back to this repository for full docs.

Each target ZIP has two install shapes:

- `backend-full/` - one backend plugin jar that runs menus, queues, FFA, duels, kits, stats, storage, and PlaceholderAPI on the backend.
- `split-install/` - a backend jar for the FFA servers plus proxy coordinator jars for Velocity and Bungee/Waterfall.

Full documentation stays in this repository under [`docs/`](docs/).

## Deployment Modes

### Standalone Backend

This is the default mode. Put the jar from `backend-full/` in a Spigot-compatible backend server, configure arenas and kits, and the backend handles queues, duels, FFA, menus, profiles, storage, and placeholders.

```yaml
settings:
  proxy:
    mode: "standalone"
```

### Backend With Proxy Coordinator

Use this for Velocity, BungeeCord, or Waterfall networks where the proxy should handle global queue routing while each backend runs arenas, combat, kits, and local match lifecycle. Put the jar from `split-install/backend/` on each backend and the matching jar from `split-install/proxy/` on the proxy.

```yaml
settings:
  proxy:
    mode: "backend"
    server-id: "ffa-1"
    channel: "zffa:main"
    report-capacity: true
    route-queues: true
```

In this mode the backend publishes per-kit arena capacity and can ask the bundled proxy coordinator to own queue decisions. If `route-queues` is `false`, the backend only reports capacity and keeps local queue behavior.

## Documentation

- [Setup](docs/SETUP.md)
- [Configuration](docs/CONFIGURATION.md)
- [Placeholders](docs/PLACEHOLDERS.md)
- [Kits](docs/KITS.md)
- [Performance](docs/PERFORMANCE.md)
- [Platform strategy](docs/PLATFORM_STRATEGY.md)
- [Roadmap](docs/TODO.md)
- [Release notes](CHANGELOG.md)

## Commands

| Command | Purpose |
| --- | --- |
| `/ffa` | Open queue and FFA options |
| `/ffamenu` | Open the main Z-FFA hub menu |
| `/ffaarenas` | Browse joinable FFA arenas |
| `/ffastats` | Open your stats menu |
| `/ffatop` | Open the leaderboard |
| `/ffaranks` | Open rank progression |
| `/duel` | Send or accept duel requests |
| `/party` | Manage parties |
| `/ranked` | Open ranked queue kit selector |
| `/unranked` | Open unranked queue kit selector |
| `/streak [player]` | View your streak or another online player's streak |
| `/leave` | Leave queue, match, or FFA |
| `/leavequeue` | Leave queue only |
| `/leaveparty` | Leave party only |
| `/zffa` | Admin setup and reload tools |
| `/zffa manage` | Open admin management menu |
| `/zffa kiteditor` | Open GUI kit editor |

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `zf.player` | `true` | Basic player access |
| `zf.admin` | `op` | Admin commands and reloads |
| `zf.kit.*` | `op` | Access to every kit |
| `zf.kit.<kit>` | unset | Access to a specific kit |
| `zf.viparena` | `op` | Access to VIP FFA arenas |

## What Can Be Customized

- `config.yml`
  - lobby items
  - queue timing
  - match timeout
  - database
  - protection
- `menus.yml`
  - main hub menu
  - FFA arena browser
  - menu titles
  - filler items
  - kit selector layout
  - centered item placement
  - stats and leaderboard menus
- `messages.yml`
  - queue join/leave text
  - duel messages
  - party messages
  - admin messages

## Placeholder Support

Z-FFA exposes a PlaceholderAPI expansion with player stats, queue status, rank, and economy-related values.

For config placeholders, see [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md).

## Donate

If you want to support development:

[![Donate via PayPal](https://img.shields.io/badge/Donate%20via%20PayPal-PayPal-blue?logo=paypal)](https://paypal.me/reemanabusal)

## Notes

- PlaceholderAPI, Vault, LuckPerms, Essentials, and Multiverse-Core are optional soft dependencies.
- The plugin uses async database work and cached profiles to reduce main-thread load.
- Lobby items are protected from movement/dropping and menus are refreshed on a timer.
