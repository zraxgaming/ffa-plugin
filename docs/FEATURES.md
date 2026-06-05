# Z-FFA Features

## Core Gameplay

- Ranked and unranked 1v1 queues
- Kit-based matchmaking
- FFA sessions with arena spawns
- Party creation, invites, party duels, and party FFA
- Duel requests with accept/decline flow
- Elo, wins, losses, kills, deaths, KDR, win rate, and streak tracking

## Admin Tools

- GUI kit editor
- Kit create, save, delete, setting, and icon commands
- Arena create, spawn, kit restriction, VIP, enable, disable, delete, and info commands
- Reload command for config, messages, menus, kits, arenas, and GUI templates
- Debug toggle for focused troubleshooting

## Menus And Lobby Items

- Direct lobby items for ranked, unranked, party, stats, event, and leaderboard
- Main hub menu for servers that want a menu-first flow
- Kit selector menus with fixed item slots or centered placement
- Stats, ranks, leaderboard, party, player, management, and kit editor menus
- Filler/background/no-action clicks close menus by default

There is no separate default hotbar item just for arena browsing. Arena browser actions still exist for custom menus if a server owner explicitly configures them.

## Performance Defaults

- Open-menu refresh is disabled by default
- Database work runs asynchronously
- Profiles are cached
- Kit menus use templates
- Lobby-item and player-menu interactions have spam cooldowns
- Queue processing runs on a timer instead of click-time matching work
- Shaded backend jars strip dependency signature and Maven metadata files

## Scope

Z-FFA stays focused on FFA, duels, parties, kits, stats, and proxy queue routing.
