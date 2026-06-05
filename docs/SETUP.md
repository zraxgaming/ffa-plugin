# Z-FFA Setup

## Install

For one server, put the matching `backend-full` jar in `plugins/`:

- Spigot: `*-backend-full-spigot.jar`
- Paper: `*-backend-full-paper.jar`
- Purpur: `*-backend-full-purpur.jar`
- Folia: `*-backend-full-folia.jar`

For a network, see [PROXY.md](PROXY.md).

Start the server once so Z-FFA generates:

```text
plugins/Z-FFA/config.yml
plugins/Z-FFA/messages.yml
plugins/Z-FFA/menus.yml
plugins/Z-FFA/arenas.yml
plugins/Z-FFA/kits.yml
```

## Basic Setup

Run these commands as an admin:

```text
/zffa setlobby
/zffa kit create nodebuff <gradient:red:gold>No Debuff</gradient>
/zffa arena create arena1
/zffa arena arena1 setspawn1
/zffa arena arena1 setspawn2
/zffa arena arena1 addffaspawn
/zffa arena arena1 addkit nodebuff
/zffa reload
```

Then test:

```text
/ranked
/unranked
/duel <player>
/party create
/party duel nodebuff
```

## Files To Edit

- `config.yml`: database, protection, lobby items, queue behavior
- `messages.yml`: player-facing messages
- `menus.yml`: GUI titles, items, lore, filler behavior
- `kits.yml`: kit items, armor, effects, settings
- `arenas.yml`: lobby, duel spawns, FFA spawns, allowed kits

After manual edits, run:

```text
/zffa reload
```

## Permissions

- `zf.player`: normal player access
- `zf.admin`: admin setup access
- `zf.kit.<kit>`: specific kit access
- `zf.kit.*`: all kit access
- `zf.viparena`: VIP FFA arena access

## Notes

- Default lobby items are direct actions for ranked, unranked, party, stats, event, and leaderboard.
- There is no separate default hotbar item just for browsing arenas.
- FFA arena joins still exist through commands or custom menus if you intentionally configure them.
- Keep `settings.menu-refresh-seconds: 0` unless live open-menu updates are required.
