# Z-FFA Core

[![Build](https://img.shields.io/github/actions/workflow/status/zraxgaming/ffa-plugin/build.yml?branch=main&label=build)](https://github.com/zraxgaming/ffa-plugin/actions)
[![Release](https://img.shields.io/github/v/release/zraxgaming/ffa-plugin?label=release)](https://github.com/zraxgaming/ffa-plugin/releases)
[![Minecraft](https://img.shields.io/badge/Minecraft-Paper%2FPurpur%201.21.x-2ea44f)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/zraxgaming/ffa-plugin)](LICENSE)
[![Donate](https://img.shields.io/badge/Donate-PayPal-blue?logo=paypal)](https://paypal.me/reemanabusal)

Professional FFA and 1v1 duel core for `xyz.zcraft.studios`, built for Paper/Purpur `1.21.x` with Java 21.

Originally built as a private ZCraft Studios network plugin, now documented for cleaner setup, easier config editing, and smoother server operations.

## Highlights

- FFA queue and live arena support
- Main hub menu and FFA arena browser
- 1v1 duel matchmaking by kit
- Ping-range matchmaking for fairer queues
- GUI-based admin kit editor
- Party queueing for duels and party FFA
- Kill effect and armor trim cosmetics
- Configurable FFA death messages
- Editable `messages.yml` for server-specific wording
- Configurable menus, centered queue layouts, and lobby items with placeholders
- Non-destructive default config updates on startup/reload
- Startup update checker with optional release jar download
- Optimized cosmetic menu selection updates
- SQLite and MySQL storage support
- PlaceholderAPI expansion support
- GitHub Actions build and release packaging

## Download

Build locally:

```bash
mvn -DskipTests package
```

The shaded jar is produced in `target/`.

GitHub releases automatically attach the packaged jar on version tags.

## Documentation

- [Setup](docs/SETUP.md)
- [Configuration](docs/CONFIGURATION.md)
- [Placeholders](docs/PLACEHOLDERS.md)
- [Kits](docs/KITS.md)
- [Performance](docs/PERFORMANCE.md)
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
| `/cosmetics` | Open cosmetics |
| `/killeffects` | Open kill effects |
| `/armortrims` | Open armor trims |
| `/ranked` | Open ranked queue kit selector |
| `/unranked` | Open unranked queue kit selector |
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
| `zf.cosmetic.*` | `op` | Access to every cosmetic |
| `zf.cosmetic.killeffect.*` | `op` | Access to every kill effect |
| `zf.cosmetic.armortrim.*` | `op` | Access to every armor trim |

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
  - cosmetics menu items
  - stats and leaderboard menus
- `cosmetics.yml`
  - kill effects
  - armor trims
  - player cosmetic selections
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
