# Z-FFA Core

Professional FFA and 1v1 duel core for `xyz.zcraft.studios`, built for Paper/Purpur `1.21.x` with Java 21.

**this was originaly a custom plugin built for zcraft network only**

**Current Version:** v1.3.1+ (See [UPDATES_v1.3.1.md](docs/UPDATES_v1.3.1.md) for latest improvements)

**License:** CC-BY-NC-SA 4.0 - [Attribution required for forks/modifications](LICENSE)

## 🚀 Recent Improvements

- ✅ **Fixed**: Lobby items rendering issue
- ✅ **Fixed**: Menu MSPT spikes
- ✅ **Optimized**: Performance & error handling
- ✅ **Added**: Separate `kits.yml` configuration file
- ✅ **Added**: Comprehensive performance & kits documentation
- 🔒 **Updated**: License to CC-BY-NC-SA 4.0 (attribution required)

See [docs/UPDATES_v1.3.1.md](docs/UPDATES_v1.3.1.md) for complete changelog.

## 📖 Documentation

- **[SETUP.md](docs/SETUP.md)** - Initial setup and configuration guide
- **[CONFIGURATION.md](docs/CONFIGURATION.md)** - Detailed config.yml options
- **[KITS.md](docs/KITS.md)** - Complete kit configuration guide (NEW!)
- **[PERFORMANCE.md](docs/PERFORMANCE.md)** - Performance tuning and optimization (NEW!)
- **[UPDATES_v1.3.1.md](docs/UPDATES_v1.3.1.md)** - Latest improvements and changelog

## 🆘 Quick Troubleshooting

| Issue | Solution |
|---|---|
| Lobby items not showing | Check debug logs: set `debug-enabled: true` in config.yml |
| Kits not loading | Verify `kits.yml` YAML syntax - use `yamllint` or online validators |
| Menu lag spikes | Increase `cache-expire-minutes` or switch to MySQL if using SQLite |
| Kit items disappear | Check player inventory is not full when joining |
| Database errors | Check MySQL connection, or switch to SQLite for testing |

See [PERFORMANCE.md](docs/PERFORMANCE.md) for complete troubleshooting.

- **bStats Integration** — Anonymous plugin metrics for usage analytics
- FFA-style kit queue system
- True FFA arenas with random respawns
- FFA kill/death stats
- VIP FFA arenas with `zf.viparena`
- Mutual-hit FFA fight requests to prevent free-hitting
- Kill reward healing and hunger refill
- 1v1 duel matchmaking by kit
- Admin kit creation from current inventory and armor
- Full Bukkit ItemStack kit saving, including enchants, names, potion metadata, and custom items
- Kit potion-effect saving
- Arena system with two spawn points per arena
- Arena-specific kit restrictions
- Runtime arena busy tracking so one arena cannot host two matches at once
- Global lobby support
- Async Paper teleporting with `Player#teleportAsync`
- Elo rating system with K-factor `32`
- Rank tiers based on Elo
- SQLite and MySQL storage support
- HikariCP connection pooling
- Async database load/save
- Caffeine profile cache
- 5-minute autosave by default
- MiniMessage support for RGB, gradients, and modern formatting
- Cached kit selector GUI
- Personal stats GUI
- Cached-profile leaderboard GUI
- Configurable DeluxeMenus-inspired menu file
- Configurable lobby inventory items for queue, stats, and leaderboard
- Separate admin and player command surfaces
- Branded startup and shutdown console banner
- Configurable match countdown
- Configurable match timeout
- Match command blocking with bypass list
- Build, break, drop, pickup, and outside-PvP protection
- Robust GUI protection against shift-click and drag spam
- Optimized duel queue tick loop and queue/status caching for lower MSPT
- Optional integration with Vault, LuckPerms, Essentials, PlaceholderAPI, and Multiverse-Core
- Configurable debug logging
- Streak reward/voucher protection system with admin commands
- Kill boost system with configurable reward commands
- PlaceholderAPI expansion with economy, group, and nickname placeholders
- Reload command for config and arenas

## Requirements

- Java 21+
- Paper or Purpur `1.21.x`
- Recommended target: `1.21.11`
- Optional: PlaceholderAPI
- Optional: LuckPerms
- Optional: Multiverse-Core

Multiverse-Core works as a soft dependency. Create/import the world with Multiverse first, then set Z-FFA lobby, duel spawns, and FFA spawns in that loaded world.

## Installation

1. Build the plugin with Maven:

```bash
mvn -DskipTests package
```

2. Copy the final jar into your server `plugins` folder:

```text
target/z-ffa-core-1.3.1.jar
```

3. Start the server once to generate config files.

4. Configure `plugins/Z-FFA/config.yml`.

5. Set your lobby and arena spawns in-game.

## Commands

| Command | Permission | Description |
| --- | --- | --- |
### Player Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/ffa` | `zf.player` | Opens the ranked kit queue selector. |
| `/ffa join` | `zf.player` | Opens the ranked kit queue selector. |
| `/ffa queue` | `zf.player` | Opens the ranked kit queue selector. |
| `/ffa arena [arena] [kit]` | `zf.player` | Joins a live FFA arena. |
| `/ffa viparena` | `zf.viparena` | Joins the first configured VIP FFA arena. |
| `/ffa kit <kit>` | `zf.player` | Equips a kit directly. |
| `/ffa setspawn` | `ffa.setspawn` or `zf.admin` | Compatibility alias for setting lobby/spawn. |
| `/ffa setarena <name>` | `ffa.setarena` or `zf.admin` | Compatibility alias: creates arena and adds an FFA spawn. |
| `/ffa setviparena <name>` | `ffa.setviparena` or `zf.admin` | Compatibility alias: creates VIP arena and adds an FFA spawn. |
| `/ffa createkit <name> [display]` | `ffa.createkit` or `zf.admin` | Compatibility alias for creating a kit from inventory, armor, and effects. |
| `/ffa leave` | `zf.player` | Leaves the current queue or forfeits the current match. |
| `/ffa stats` | `zf.player` | Opens your personal stats GUI. |
| `/ffa top` | `zf.player` | Opens the cached leaderboard GUI. |
| `/ffa items` | `zf.player` | Refreshes your lobby selector items. |
| `/ffa spawn` | `zf.player` | Teleports to lobby, or forfeits first if in a match. |
| `/ffa status` | `zf.player` | Shows current queue/match status. |
| `/duel` | `zf.player` | Opens the ranked kit queue selector. |
| `/duel leave` | `zf.player` | Leaves the current queue or forfeits the current match. |
| `/duel stats` | `zf.player` | Opens your personal stats GUI. |
| `/party create` | `zf.player` | Creates a party. |
| `/party invite <player>` | `zf.player` | Invites a player. |
| `/party accept` | `zf.player` | Accepts a party invite. |
| `/party duel <kit>` | `zf.player` | Queues your party for party-vs-party duel. |
| `/party ffa [arena] [kit]` | `zf.player` | Sends your party into FFA together. |
| `/party leave` | `zf.player` | Leaves your party. |

### Admin Commands

| Command | Permission | Description |
| --- | --- | --- |
| `/zffa reload` | `zf.admin` | Reloads config, kits, arenas, messages, menus, and GUI templates. |
| `/zffa setlobby` | `zf.admin` | Sets the global lobby to your current location. |
| `/zffa items` | `zf.admin` | Gives you the configured lobby items. |
| `/zffa kit create <name> [display]` | `zf.admin` | Saves a kit from your current inventory and armor. |
| `/zffa kit save <name> [display]` | `zf.admin` | Alias for kit creation/saving. |
| `/zffa kit list` | `zf.admin` | Lists loaded kits. |
| `/zffa kit delete <name>` | `zf.admin` | Deletes a kit. |
| `/zffa kit seticon <name> <material>` | `zf.admin` | Updates a kit GUI icon. |
| `/zffa kit setting <name> <setting> <value>` | `zf.admin` | Updates kit behavior. |
| `/zffa arena create <name>` | `zf.admin` | Creates a named arena. |
| `/zffa arena list` | `zf.admin` | Lists arenas and runtime state. |
| `/zffa arena <name> setspawn1` | `zf.admin` | Sets arena spawn 1 to your current location. |
| `/zffa arena <name> setspawn2` | `zf.admin` | Sets arena spawn 2 to your current location. |
| `/zffa arena <name> addffaspawn` | `zf.admin` | Adds your current location as an FFA respawn. |
| `/zffa arena <name> clearffaspawns` | `zf.admin` | Clears all FFA respawns. |
| `/zffa arena <name> addkit <kit>` | `zf.admin` | Allows a kit to use this arena. |
| `/zffa arena <name> removekit <kit>` | `zf.admin` | Removes a kit from this arena. |
| `/zffa arena <name> vip true\|false` | `zf.admin` | Toggles VIP-only access for an arena. |
| `/zffa arena <name> enable` | `zf.admin` | Enables an arena for matchmaking. |
| `/zffa arena <name> disable` | `zf.admin` | Disables an arena for matchmaking. |
| `/zffa arena <name> delete` | `zf.admin` | Deletes an arena if it is not busy. |
| `/zffa arena <name> info` | `zf.admin` | Shows ready, busy, and kit status for the arena. |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `zf.player` | `true` | Basic player access. |
| `zf.admin` | `op` | Admin setup and reload access. |
| `zf.kit.*` | `op` | Access to every kit. |
| `zf.kit.<kit>` | unset | Access to a specific kit. Example: `zf.kit.nodebuff`. |
| `zf.viparena` | `op` | Access to VIP FFA arenas. |
| `ffa.setspawn` | unset | Compatibility permission for `/ffa setspawn`. |
| `ffa.setarena` | unset | Compatibility permission for `/ffa setarena <name>`. |
| `ffa.setviparena` | unset | Compatibility permission for `/ffa setviparena <name>`. |
| `ffa.createkit` | unset | Compatibility permission for `/ffa createkit <name>`. |

## PlaceholderAPI Placeholders

Identifier: `zf`

| Placeholder | Output |
| --- | --- |
| `%zf_elo%` | Player Elo rating. |
| `%zf_rank%` | Player rank tier. |
| `%zf_wins%` | Player wins. |
| `%zf_losses%` | Player losses. |
| `%zf_kills%` | Player FFA kills. |
| `%zf_deaths%` | Player FFA deaths. |
| `%zf_status%` | Player status: `Lobby`, `Queued: <kit>`, `FFA: <arena>`, or `In Match`. |

## Elo System

Default Elo: `1000`

Elo changes use a K-factor of `32`:

```java
expectedScore = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
eloChange = 32 * (1.0 - expectedScore);
```

Rank tiers:

| Elo Range | Rank |
| --- | --- |
| `< 900` | Coal I |
| `900 - 999` | Coal II |
| `1000 - 1099` | Coal III |
| `1100 - 1249` | Iron |
| `1250 - 1449` | Gold |
| `1450 - 1699` | Diamond |
| `1700 - 1999` | Emerald |
| `2000+` | Netherstar |

## Configuration

Main config:

```text
plugins/Z-FFA/config.yml
```

Arena data:

```text
plugins/Z-FFA/arenas.yml
```

Default config structure:

```yaml
settings:
  prefix: "<gradient:#21d4fd:#b721ff><bold>Z-FFA</bold></gradient> <dark_gray>|</dark_gray> "
  default-spawn: "world, 0, 64, 0, 0, 0"
  database-type: "SQLITE" # SQLITE or MYSQL
  cache-expire-minutes: 20
  autosave-minutes: 5
  lobby-items-enabled: true
  match-countdown-seconds: 5
  match-timeout-minutes: 10
  block-commands-in-match: true
  mysql:
    host: "127.0.0.1"
    port: 3306
    database: "zffa"
    username: "root"
    password: ""
    pool-size: 10
```

Kit settings command:

```text
/zffa kit setting nodebuff allow-regen true
/zffa kit setting nodebuff allow-hunger false
/zffa kit setting nodebuff speed-multiplier 1.0
/zffa kit setting nodebuff max-health 20.0
```

## Lobby Items

Lobby items are configured in `config.yml` under `lobby-items`.

Example:

```yaml
lobby-items:
  queue-selector:
    slot: 0
    material: NETHER_STAR
    name: "<gradient:#21d4fd:#b721ff><bold>Ranked Queue</bold></gradient>"
    lore:
      - "<gray>Right-click to choose a kit queue."
      - "<dark_gray>Status: <white>%status%</white>"
    action: OPEN_KITS
```

Supported actions:

- `OPEN_KITS`
- `OPEN_STATS`
- `OPEN_LEADERBOARD`
- `LEAVE_QUEUE`

Supported placeholders:

- `%player%`
- `%elo%`
- `%rank%`
- `%wins%`
- `%losses%`
- `%kills%`
- `%deaths%`
- `%status%`

Players receive configured lobby items on join and after returning from a match.

## Menus

Menus are configured in:

```text
plugins/Z-FFA/menus.yml
```

The format is DeluxeMenus-inspired: each menu has a title, size, item material, name, lore, and placeholders.

Configurable menus:

- `kit-selector`
- `stats`
- `leaderboard`

Supported menu placeholders:

- `%player%`
- `%elo%`
- `%rank%`
- `%wins%`
- `%losses%`
- `%kills%`
- `%deaths%`
- `%status%`
- `%kit%`
- `%kit_display%`
- `%queue_size%`
- `%position%`

## Storage

Set the storage engine in `config.yml`:

```yaml
settings:
  database-type: "SQLITE"
```

Valid values:

- `SQLITE`
- `MYSQL`

SQLite stores data locally in:

```text
plugins/Z-FFA/zffa.db
```

MySQL uses the credentials under:

```yaml
settings:
  mysql:
```

All profile loading and saving is performed asynchronously through HikariCP.

## Kit Configuration

Kits are configured in `config.yml` under `kits`.

The recommended way to create a kit is in-game:

```text
/zffa kit create nodebuff <gradient:red:gold>No Debuff</gradient>
```

That command saves:

- Your inventory contents
- Your armor
- Your active potion effects
- Item names
- Enchants
- Potion metadata
- Custom item metadata supported by Bukkit serialization

You can overwrite a kit by running the same command again with the same kit name.

Example:

```yaml
kits:
  nodebuff:
    display: "<gradient:red:gold>No Debuff</gradient>"
    icon: POTION
    settings:
      allow-regen: true
      allow-hunger: false
      speed-multiplier: 1.0
      max-health: 20.0
    items:
      - "DIAMOND_SWORD:1"
      - "ENDER_PEARL:16"
      - "SPLASH_POTION:healing:28"
      - "COOKED_BEEF:64"
    armor:
      helmet: "DIAMOND_HELMET:1"
      chestplate: "DIAMOND_CHESTPLATE:1"
      leggings: "DIAMOND_LEGGINGS:1"
      boots: "DIAMOND_BOOTS:1"
```

Newly saved kits use Bukkit ItemStack serialization under `inventory`, so the generated config may look more detailed than the legacy example above. That is intentional.

Kit permission format:

```text
zf.kit.<kit-id>
```

For the example above:

```text
zf.kit.nodebuff
```

## Arena Setup

Set a global lobby:

```text
/zffa setlobby
```

Create an arena:

```text
/zffa arena create arena1
```

Set arena spawns:

```text
/zffa arena arena1 setspawn1
/zffa arena arena1 setspawn2
```

Add FFA respawns:

```text
/zffa arena arena1 addffaspawn
/zffa arena arena1 addffaspawn
/zffa arena arena1 addffaspawn
```

Assign allowed kits to the arena:

```text
/zffa arena arena1 addkit nodebuff
```

If an arena has no kits assigned, it supports all kits. If it has one or more kits assigned, only those kits can use it.

Make an arena VIP-only:

```text
/zffa arena arena1 vip true
```

Check arena status:

```text
/zffa arena arena1 info
```

Disable or delete arenas:

```text
/zffa arena arena1 disable
/zffa arena arena1 enable
/zffa arena arena1 delete
```

An arena is usable only after both spawn points are set.

The plugin keeps runtime busy state for every arena. When a match starts, the arena is claimed. When the match ends, the arena is released. Queues only start matches in arenas that are ready, not busy, and compatible with the selected kit.

For FFA mode, an arena needs at least one FFA spawn. Add multiple spawns for better gameplay.

## FFA Mode

Join a live FFA arena:

```text
/ffa arena arena1 nodebuff
```

Short form:

```text
/ffa arena
```

If no arena is provided, the first ready FFA arena is used. If no kit is provided, the first compatible kit is used.

FFA behavior:

- Players receive the selected kit.
- Players teleport to a random FFA spawn.
- If mutual-hit protection is enabled, the first hit sends a fight request and does no damage.
- If the target hits back within the configured window, combat begins.
- Lethal damage awards kill/death stats.
- Void damage counts as a death.
- Killers are healed and can have hunger refilled.
- Killer and victim receive action-bar feedback.
- Victims are re-kitted and sent to another random FFA spawn.
- `/ffa leave` exits FFA.

## Multiverse-Core

Z-FFA stores locations by Bukkit world name. With Multiverse-Core, create or import the world before setting locations:

```text
/mv create ffa_world normal
```

Then stand in that world and run:

```text
/zffa setlobby
/zffa arena create arena1
/zffa arena arena1 setspawn1
/zffa arena arena1 setspawn2
/zffa arena arena1 addffaspawn
```

Do not rename the Multiverse world after setup unless you update `arenas.yml`.

Arena data is stored in:

```text
plugins/Z-FFA/arenas.yml
```

## Matchmaking

Matchmaking is kit-specific.

Flow:

1. Player opens `/ffa`.
2. Player clicks a kit.
3. Player is added to that kit queue.
4. Every second, queues are checked.
5. When two players are available, the plugin claims a free arena.
6. The arena must support the selected kit.
7. Both players receive the selected kit.
8. Both players are teleported with `teleportAsync`.

Current matching behavior:

- First available player vs first available player
- Same-kit queue only
- Arena must not be busy
- Arena must support the queued kit

If no ready arena is connected to the selected kit, the player gets a setup error. If all compatible arenas are busy, the player gets a clean “try again in a moment” message.

If a party leader clicks a kit in the selector, the whole party queues for party duel. Party members cannot queue the party themselves.

## Parties

Party commands:

```text
/party create
/party invite <player>
/party accept
/party list
/party duel <kit>
/party ffa [arena] [kit]
/party leave
/party disband
```

Party duel:

- Party leader queues with `/party duel <kit>` or clicks a kit in the selector.
- The plugin waits for another party queued for the same kit.
- It finds a ready, empty arena connected to that kit.
- Party 1 spawns at `spawn1`.
- Party 2 spawns at `spawn2`.
- The fight ends when every player on one side is eliminated.
- Every winner gets a win and Elo gain.
- Every loser gets a loss and Elo loss.

Party FFA:

- Party leader runs `/party ffa [arena] [kit]`.
- Every online free party member joins that FFA arena with the selected kit.

Future Elo-range matching can be added inside `QueueManager`.

## Performance Notes

- Player profiles are cached with Caffeine.
- Database I/O runs off the main thread.
- Profile saves happen on quit and autosave.
- The plugin does not save to the database on every hit, kill, or GUI click.
- Arena teleports use Paper async teleporting.
- Kit selector inventory is cached and cloned per open so queue counts can update cheaply.

## Win/Loss Detection

During an active match:

- Lethal damage ends the match before the player fully dies.
- Void damage counts as a loss.
- Disconnecting during a match forfeits.
- `/ffa leave` or `/duel leave` forfeits.
- The winner receives a win and Elo gain.
- The loser receives a loss and Elo loss.
- Both players are returned to the lobby and receive configured lobby items.

## Match Protection

Configurable protections are included in `config.yml`:

- Block breaking during matches
- Block placing during matches
- Item dropping during matches
- Item pickup during matches
- Preventing PvP unless both players are in the same active match
- Freezing players during the countdown
- Blocking commands during matches except configured bypasses

FFA combat settings:

```yaml
settings:
  ffa:
    require-mutual-hit: true
    fight-request-expire-seconds: 10
    kill-heal-hearts: 20.0
    refill-hunger-on-kill: true
```

## Building From Source

Install:

- JDK 21+
- Maven 3.9+

Build:

```bash
mvn -DskipTests package
```

Output:

```text
target/z-ffa-core-1.0.0.jar
```

## Recommended Testing

Use Spark on a test server:

```text
/spark profiler
```

Watch for:

- Low main-thread plugin cost
- Database work appearing off-thread
- No teleport-related chunk stalls during duel starts

## Current Notes

- The leaderboard GUI uses currently cached profiles, not a full database top query.
- Placeholder values are available for loaded/cached players.
- `PlaceholderAPI` is optional. If it is not installed, the plugin still runs.
- LuckPerms is optional because permissions use standard Bukkit permission checks.
Menu config:

```text
plugins/Z-FFA/menus.yml
```
