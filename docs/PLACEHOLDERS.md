# Z-FFA Placeholder Guide

This plugin uses placeholders in three places:

1. `config.yml` for lobby items and other configurable text.
2. `menus.yml` for GUI titles, lore, and item labels.
3. `messages.yml` for player-facing chat messages.

## Config Placeholders

### Lobby items

Used inside `config.yml` under `lobby-items`.

| Placeholder | Meaning |
| --- | --- |
| `%player%` | Player name |
| `%elo%` | Current Elo |
| `%rank%` | Rank name |
| `%wins%` | Win count |
| `%losses%` | Loss count |
| `%kills%` | FFA kills |
| `%deaths%` | FFA deaths |
| `%status%` | Queue/match status |
| `%streak%` | Current streak |
| `%vouchers%` | Streak vouchers |
| `%killboost%` | Kill boosts |

Example:

```yaml
lobby-items:
  stats:
    slot: 4
    material: PLAYER_HEAD
    name: "<gold>Your Stats</gold>"
    lore:
      - "<gray>Elo: <white>%elo%</white>"
      - "<gray>Rank: <white>%rank%</white>"
      - "<gray>Status: <white>%status%</white>"
    action: OPEN_STATS
```

## Menu Placeholders

Used inside `menus.yml`.

| Placeholder | Meaning |
| --- | --- |
| `%player%` | Player name |
| `%elo%` | Current Elo |
| `%rank%` | Rank name |
| `%wins%` | Win count |
| `%losses%` | Loss count |
| `%kills%` | FFA kills |
| `%deaths%` | FFA deaths |
| `%status%` | Queue/match status |
| `%kit%` | Kit ID |
| `%kit_display%` | Kit display name fallback |
| `%queue_size%` | Current queue size |
| `%position%` | Leaderboard position |
| `%rank_name%` | Rank name entry |
| `%min_elo%` | Minimum Elo for a rank |

## Message Placeholders

Used inside `messages.yml`.

Message placeholders use `{name}` format instead of percent signs.

| Placeholder | Example use |
| --- | --- |
| `{player}` | `{player} joined the party.` |
| `{target}` | `Invited {target}` |
| `{leader}` | `{leader} invited you.` |
| `{kit}` | `Queued for {kit}` |
| `{arena}` | `Joined {arena}` |
| `{type}` | `ranked` or `unranked` |
| `{reason}` | Match end reason |
| `{elo}` | Elo gain/loss |
| `{remaining}` | Remaining vouchers |
| `{attacker}` | Fight request sender |
| `{victim}` | Fight request target |
| `{limit}` | Party size limit |
| `{action}` | `accept` or `decline` |

## Messages File Example

```yaml
queue.joined: "<green>Queued for <white>{kit}</white> (<white>{type}</white>)."
party.invited-target: "<green>{leader} invited you to a party. Use <white>/party accept</white>."
duel.result: "<gray>Result: {reason}</gray>"
```

## Tips

- Keep MiniMessage formatting inside values.
- Use `{name}` placeholders in `messages.yml`, not `%name%`.
- Use `%name%` placeholders in `config.yml` and `menus.yml`.
- If a line is plain text, you can still add colors and gradients.


## Other (external plugins)

|placeholder |   Output|
%zf_elo%   | Player Elo rating.|
%zf_rank%   | Player rank tier.|
%zf_wins%   | Player wins.|
%zf_losses%  |  Player losses.|
%zf_kills%   | Player FFA kills.|
%zf_deaths%  |  Player FFA deaths.|
%zf_status%  |  Player status: Lobby, Queued: <kit>, FFA: <arena>, or In Match.|
