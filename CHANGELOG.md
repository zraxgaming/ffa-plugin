# Changelog

All notable changes to Z-FFA are documented here.

## [Unreleased]

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
