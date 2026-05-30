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
- 1v1 duel matchmaking by kit
- Party queueing for duels and party FFA
- Editable `messages.yml` for server-specific wording
- Configurable menus and lobby items with placeholders
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
| `/duel` | Send or accept duel requests |
| `/party` | Manage parties |
| `/leave` | Leave queue, match, or FFA |
| `/leavequeue` | Leave queue only |
| `/leaveparty` | Leave party only |
| `/zffa` | Admin setup and reload tools |

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
  - menu titles
  - filler items
  - kit selector layout
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
