# Z-FFA Performance Guide

## What’s Already Optimized

- Profiles are cached with Caffeine.
- Database work runs asynchronously.
- Menus use cached templates and lightweight placeholder replacement.
- Queue processing runs on a repeating tick task.
- Lobby items are validated before being given to players.
- Match cleanup returns players to the lobby with async teleporting.

## Recommended Config

```yaml
settings:
  cache-expire-minutes: 20
  autosave-minutes: 5
  menu-refresh-seconds: 5
  database-type: "MYSQL"
```

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

Menus refresh on a timer so queue size, status, and profile-driven text can stay current.

The default is:

```yaml
settings:
  menu-refresh-seconds: 5
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
- keep kit definitions clean and focused

## Debugging Performance

Turn on debug logs only while testing:

```yaml
settings:
  debug-enabled: true
```

Then watch for repeated warnings around menus, arenas, or storage.
