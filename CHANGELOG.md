# Changelog

All notable changes to Z-FFA are documented here.

## [Unreleased]

- Added ping-range matchmaking inspired by practice-core queue systems.
- Added configurable FFA death messages with broadcast/actionbar toggles.
- Added PlaceholderAPI values for KDR, win rate, queued players, FFA players, and selected cosmetics.
- Added non-destructive config default updates for generated YAML files.
- Added a one-time startup update checker with optional release jar download into Bukkit's update folder.
- Added admin management and GUI kit editor menus.
- Fixed kit save/delete/icon/setting commands writing to the wrong `kits.yml` path.
- Fixed `/cosmetics` command metadata and aliases.

## [1.3.6] - 2026-05-31

- Added a configurable main hub menu with quick access to ranked, unranked, FFA arenas, cosmetics, and stats.
- Added an FFA arena browser menu with join actions, arena status, player counts, VIP state, and default kit placeholders.
- Added direct player commands for common menus: `/ffamenu`, `/ffaarenas`, `/ffastats`, `/ffatop`, `/ffaranks`, `/cosmetics`, `/killeffects`, `/armortrims`, `/ranked`, and `/unranked`.
- Added `/ffa` subcommands for the new menu flows, including `menu`, `ffa`, `arenas`, `cosmetics`, `killeffects`, `armortrims`, `ranks`, and `party`.
- Centered ranked and unranked queue menu items by default, with configurable item slots.
- Optimized cosmetic selections so clicks update the selected menu items without reopening the inventory or rebuilding the full menu.
- Batched cosmetic selection saves to reduce click-time file I/O.
- Added more default kill effects and armor trims.
- Added `zf.cosmetic.*` as a shortcut permission for all cosmetics.
- Improved menu protection for filler clicks and double-click item collection.

## [1.3.2 - 1.3.5]

- Added a roadmap/TODO doc for future ideas like an armour trims shop and a custom kit editor.
- Added a root-level `messages.yml` so server owners can edit queue, duel, party, leave, and admin text without code changes.
- Added standalone leave commands: `/leave`, `/leavequeue`, and `/leaveparty`.
- Added 5-second menu refresh support with a configurable interval.
- Fixed post-match cleanup so players are returned to the lobby with inventory cleared and lobby items restored.
- Locked lobby items so players cannot move, drop, or drag them around.
- Reworked GitHub Actions to build the shaded jar and attach it to releases.
- Added cleaner docs for setup, configuration, placeholders, kits, performance, and release notes.
- Refreshed the README with badges, donation link, and clearer project documentation.

## [1.3.1] - 2026-05-29

- bStats metrics integration for usage tracking.
- Lobby items reliably render for newly joined players.
- Command blocking now covers FFA as well as matches.
- Improved movement handling to reduce unnecessary work.
- Added separate kit configuration in `kits.yml`.
- Added performance and kit documentation.

## [1.3.0] - 2026-05-28

- Added party FFA flow.
- Prevented players from queueing multiple kits at once.
- Added rank progression configuration and GUI support.
- Added admin Elo management commands.
- Added player menu improvements.
- Added rank list GUI support.

## Notes

- Use [docs/SETUP.md](docs/SETUP.md) for setup.
- Use [docs/CONFIGURATION.md](docs/CONFIGURATION.md) for config details.
- Use [docs/PLACEHOLDERS.md](docs/PLACEHOLDERS.md) for placeholder syntax.
