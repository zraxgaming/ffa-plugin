# Z-FFA v1.3.1 Updates

This page summarizes the current v1.3.1 branch state and the docs now included with the project.

## Main Improvements

- Separate `messages.yml` for editable player-facing text.
- Better menu and lobby-item handling.
- Standalone leave commands:
  - `/leave`
  - `/leavequeue`
  - `/leaveparty`
- Menu refresh support with a configurable interval.
- Better post-match cleanup back to the lobby.
- GitHub Actions build workflow for packaging the shaded jar.
- Cleaner docs for setup, configuration, placeholders, kits, and performance.

## What Was Cleaned Up

- Messages are now documented with the correct root-level YAML format.
- Placeholder usage is separated by file type:
  - `%placeholders%` for `config.yml` and `menus.yml`
  - `{placeholders}` for `messages.yml`
- README now points to the right docs and release flow.

## Related Docs

- [SETUP.md](SETUP.md)
- [CONFIGURATION.md](CONFIGURATION.md)
- [PLACEHOLDERS.md](PLACEHOLDERS.md)
- [KITS.md](KITS.md)
- [PERFORMANCE.md](PERFORMANCE.md)
