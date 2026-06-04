# Z-FFA Performance Guide

## What's Already Optimized

- Profiles are cached with Caffeine.
- Database work runs asynchronously.
- Menus use cached templates and lightweight placeholder replacement.
- Ranked and unranked kit menus can center items without extra per-click work.
- Filler, empty, and no-action menu clicks close Z-FFA menus to prevent click spam.
- Queue processing runs on a repeating tick task.
- Lobby items are validated before being given to players.
- Match cleanup returns players to the lobby with async teleporting.

## Recommended Config

```yaml
settings:
  cache-expire-minutes: 20
  autosave-minutes: 5
  menu-refresh-seconds: 0
  database-type: "MYSQL"
```

Open-menu refreshes are disabled by default. For small servers that want live queue/profile numbers inside menus, set `menu-refresh-seconds` to `30` or higher.

## Proxy-Assisted Mode

For Bungee/Waterfall network installs, put the same jar on the proxy and each backend, then enable backend mode so the proxy coordinator can own global queues and server routing while this backend keeps combat, arena cleanup, kits, menus, and storage local:

```yaml
settings:
  proxy:
    mode: "backend"
    server-id: "ffa-1"
    route-queues: true
    report-capacity: true
```

This reduces backend queue work on larger networks and lets the proxy choose the least busy FFA backend from capacity reports.

## When To Use MySQL

Use MySQL when:

- you expect multiple concurrent matches
- you want better long-term persistence
- you run the server for more than a few test users

Use SQLite when:

- you are testing locally
- you want the simplest setup
- you have a small server

## Menu Refresh

Menus can refresh on a timer so queue size, status, and profile-driven text stay current while a player keeps a menu open. This costs repeated item rebuilds, so leave it disabled unless you need live GUI data.

The default is:

```yaml
settings:
  menu-refresh-seconds: 0
```

## Signs To Watch For

- menu opens feel slow
- database errors appear in console
- queue joins take too long
- players return from matches without lobby items

## Extra Tuning

If the server is larger:

- raise `mysql.pool-size`
- increase `cache-expire-minutes`
- avoid very large menu lore blocks
- prefer `center-items: true` or fixed `item-slots` instead of oversized decorative menus
- keep kit definitions clean and focused

## Debugging Performance

Turn on debug logs only while testing:

```yaml
settings:
  debug-enabled: true
```

Then watch for repeated warnings around menus, arenas, or storage.
