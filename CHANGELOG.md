# Changelog

All notable changes to the Z-FFA plugin are documented in this file.

## [1.3.1] - 2026-05-29
- New: bStats metrics integration (plugin ID: 31638) for usage tracking and analytics.
- Fix: Lobby items now reliably display for newly joined players by increasing spawn delay from 10 ticks to 20 ticks, ensuring client is fully ready. ([src/main/java/xyz/zcraft/studios/zffa/listener/PlayerConnectionListener.java](src/main/java/xyz/zcraft/studios/zffa/listener/PlayerConnectionListener.java))
- Fix: Players can no longer execute commands while in FFA mode; extended command blocking logic to cover FFA scenarios in addition to matches. ([src/main/java/xyz/zcraft/studios/zffa/listener/ProtectionListener.java](src/main/java/xyz/zcraft/studios/zffa/listener/ProtectionListener.java))
- Optimization: Improved move event handler in `ProtectionListener` — early block movement check return to reduce CPU overhead. ([src/main/java/xyz/zcraft/studios/zffa/listener/ProtectionListener.java](src/main/java/xyz/zcraft/studios/zffa/listener/ProtectionListener.java))
- Code cleanup: Minor refactoring in `LobbyItemListener` using var for improved readability. ([src/main/java/xyz/zcraft/studios/zffa/listener/LobbyItemListener.java](src/main/java/xyz/zcraft/studios/zffa/listener/LobbyItemListener.java))
- License: Added MIT License file for open-source distribution.
- Build: Maven shade plugin now relocates bStats to prevent conflicts with other plugins.

## [1.3.0] - 2026-05-28
- New: Party FFA flow — party leaders can select a kit and queue the whole party into an FFA arena (party splitting / team-safe match support). ([src/main/java/xyz/zcraft/studios/zffa/party/PartyManager.java](src/main/java/xyz/zcraft/studios/zffa/party/PartyManager.java), [src/main/java/xyz/zcraft/studios/zffa/ffa/FfaManager.java](src/main/java/xyz/zcraft/studios/zffa/ffa/FfaManager.java), [src/main/java/xyz/zcraft/studios/zffa/duel/QueueManager.java](src/main/java/xyz/zcraft/studios/zffa/duel/QueueManager.java), [src/main/java/xyz/zcraft/studios/zffa/listener/InventoryListener.java](src/main/java/xyz/zcraft/studios/zffa/listener/InventoryListener.java), [src/main/java/xyz/zcraft/studios/zffa/gui/GuiManager.java](src/main/java/xyz/zcraft/studios/zffa/gui/GuiManager.java))
- Fix: Prevent players from queuing multiple kits or joining both ranked and unranked simultaneously via single-queue tracking (`queuedKit`). ([src/main/java/xyz/zcraft/studios/zffa/duel/QueueManager.java](src/main/java/xyz/zcraft/studios/zffa/duel/QueueManager.java))
- New: Party size permission handling — permissions like `zf.party.X` control allowed party sizes. ([src/main/java/xyz/zcraft/studios/zffa/party/PartyManager.java](src/main/java/xyz/zcraft/studios/zffa/party/PartyManager.java))
- New: Rank progression system driven by config (`ranks`) and a `RankManager` with configurable names, thresholds and display material; added in-menu rank list. ([src/main/java/xyz/zcraft/studios/zffa/profile/RankManager.java](src/main/java/xyz/zcraft/studios/zffa/profile/RankManager.java), [src/main/java/xyz/zcraft/studios/zffa/gui/GuiManager.java](src/main/java/xyz/zcraft/studios/zffa/gui/GuiManager.java), [src/main/resources/config.yml](src/main/resources/config.yml), [src/main/resources/menus.yml](src/main/resources/menus.yml))
- Fix: Unranked duel requests no longer affect Elo (unranked logic preserved in `MatchManager`). ([src/main/java/xyz/zcraft/studios/zffa/duel/MatchManager.java](src/main/java/xyz/zcraft/studios/zffa/duel/MatchManager.java))
- New: Admin Elo management commands: `/zffa elo give|set|remove <player> <amount>`. ([src/main/java/xyz/zcraft/studios/zffa/command/ZffaAdminCommand.java](src/main/java/xyz/zcraft/studios/zffa/command/ZffaAdminCommand.java), [src/main/java/xyz/zcraft/studios/zffa/profile/PlayerProfile.java](src/main/java/xyz/zcraft/studios/zffa/profile/PlayerProfile.java))
- New: Player menu improvements — view player stats and quickly open a duel kit selector. ([src/main/java/xyz/zcraft/studios/zffa/gui/GuiManager.java](src/main/java/xyz/zcraft/studios/zffa/gui/GuiManager.java), [src/main/java/xyz/zcraft/studios/zffa/listener/PlayerInteractionListener.java](src/main/java/xyz/zcraft/studios/zffa/listener/PlayerInteractionListener.java))
- New: `OPEN_RANKS` menu and `GuiType.RANKS` view to show configurable ranks. ([src/main/java/xyz/zcraft/studios/zffa/gui/GuiType.java](src/main/java/xyz/zcraft/studios/zffa/gui/GuiType.java))
- Fixes: Multiple compile-time and runtime fixes introduced while implementing the features (missing imports, generic handling, misplaced/missing braces). Key fixed files:
  - `GuiManager.java` (syntax fixes and menu wiring)
  - `InventoryListener.java` (action handling & imports)
  - `ProtectionListener.java` (UUID usage for match checks)
  - `RankManager.java` (safe parsing of config map list)

## [1.2.8] - (previous)
- Packaged artifacts available: `target/z-ffa-core-1.2.8.jar` and `target/original-z-ffa-core-1.2.8.jar` during prior builds.
- (Contains earlier matchmaking, GUI, and FFA features prior to the 1.3.0 changes.)

## Notes
- Build: Project version bumped to `1.3.0` in `pom.xml`.
- Menus: `menus.yml` provides configurable layouts, filler items and kit item displays. Consider tuning `menus.kit-selector` and `menus.ranks` to customize UI presentation.
- Performance: There are plans to add configurable menu refresh intervals and to optimize repeated heavy operations; keep an eye on `settings.match-countdown-seconds` and arena concurrency if MSPT spikes occur.

If you want, I can:
- Add a dedicated lobby item for `OPEN_RANKS` (lobby-items config),
- Add menu refresh timers (e.g. 0.5s/0.75s) with a config option,
- Produce a more detailed per-file diff summary for the changes in this release.
