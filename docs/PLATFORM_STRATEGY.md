# Platform Strategy

This document defines the practical target shape for Z-FFA across Minecraft versions, server forks, and proxy/network deployments.

## Current Version Reality

Checked on 2026-06-02:

- Minecraft Java Edition `26.1.2` is the current public release line after Mojang's 2026 version-numbering change.
- Minecraft Java Edition `1.21.11` was the last `1.x` release line before the 2026 numbering change.
- Current Paper builds require a newer Java runtime than older Minecraft lines; older targets still need Java 17 or Java 21 depending on version.
- Folia is not "just Paper with more threads." Folia compatibility requires region-safe scheduling and avoiding unsafe global player/world operations.

## Do Not Build 25 Separate Codebases

The right model is shared core code plus small adapters, not copy-pasted jars.

Recommended artifacts:

1. `zffa-backend-modern`
   - Paper/Purpur `1.20.5` through latest.
   - Main supported backend jar.
2. `zffa-backend-legacy`
   - Paper/Purpur/Spigot `1.18.2` through `1.20.4`.
   - Avoids newer API calls and newer materials.
3. `zffa-backend-folia`
   - Folia-compatible backend jar.
   - Uses region/entity/global schedulers through an adapter.
4. `zffa-proxy-velocity`
   - Network queue/routing/stat sync for Velocity.
5. `zffa-proxy-bungee`
   - Network queue/routing/stat sync for BungeeCord/Waterfall-style networks if still needed.
6. `zffa-standalone`
   - The current all-in-one backend behavior for single-server installs.

This gives broad coverage with about 5-6 jars, not 25. Extra packaging variants can be produced by Maven profiles without changing source logic.

## Version Bands

Use broad version bands instead of every sub-version:

| Band | Minecraft | Runtime | Notes |
| --- | --- | --- | --- |
| Legacy 1 | `1.18.2` | Java 17 | Lowest realistic target. Avoid modern item/API assumptions. |
| Legacy 2 | `1.19.4` | Java 17 | Transitional combat/material compatibility. |
| Stable 1 | `1.20.4` | Java 17 | Last line before several modern API shifts. |
| Stable 2 | `1.20.5`-`1.21.1` | Java 21 | Modern item/data APIs begin to matter more. |
| Modern | `1.21.4`-`1.21.11` | Java 21 | Current pre-2026 line. |
| Latest | `26.1+` | Java 25+ for Paper | New version numbering and newer runtime expectation. |

## Platform Matrix

| Platform | Support Plan |
| --- | --- |
| Paper | Primary target. |
| Purpur | Supported through Paper API compatibility. |
| Spigot | Legacy compatibility only where API gaps are manageable. Do not make Spigot the primary API. |
| Folia | Separate adapter/jar. Requires scheduler rewrite. |
| Velocity | Primary proxy target. |
| Bungee/Waterfall | Optional proxy target for older networks. |

## Proxy Split

Z-FFA should support three deployment modes:

1. Standalone backend
   - Current mode.
   - One server handles queue, duels, FFA, profiles, menus, and storage.

2. Backend plus proxy
   - Proxy owns network queues, server routing, cross-server party state, and global player status.
   - Backend owns arena execution, kit application, combat rules, and local match lifecycle.
   - Both talk through plugin messages or Redis-style pub/sub.

3. Proxy-only coordination with multiple backends
   - Proxy selects a backend arena server.
   - Backend reports capacity, active arenas, match state, and player finish events.
   - Storage is centralized so stats remain consistent.

## Shared Module Layout

Future Maven shape:

```text
zffa-parent/
  zffa-api/                 shared DTOs, messages, platform-neutral services
  zffa-core/                queue, profiles, kits, ranks, and match logic
  zffa-platform-bukkit/     Bukkit/Paper/Purpur/Spigot adapter
  zffa-platform-folia/      Folia scheduler/world adapter
  zffa-proxy-common/        proxy protocol and shared network queue code
  zffa-proxy-velocity/      Velocity plugin
  zffa-proxy-bungee/        Bungee plugin
  zffa-standalone/          backend-only packaged jar
```

## Optimization Priorities

1. Replace direct scheduler calls with a `PlatformScheduler` adapter.
2. Stop refreshing static menus on a timer.
3. Keep profile saves async and batched.
4. Move global/network queues to proxy mode when enabled.
5. Use backend capacity reports instead of scanning all arenas from the proxy.
6. Keep combat execution on the backend that owns the player and arena.
7. Avoid Spigot-only support work that weakens Paper/Folia safety.

## First Implementation Phases

Phase 1:

- Introduce scheduler/platform adapter interfaces.
- Reduce menu refresh cost.
- Keep current standalone jar working.

Phase 2:

- Split shared core from Bukkit-specific code.
- Add Maven modules and profiles.
- Produce legacy/modern backend builds.

Phase 3:

- Add Velocity proxy plugin.
- Add backend-proxy messaging.
- Proxy owns network queues and backend server selection.

Phase 4:

- Add Folia backend adapter.
- Replace unsafe global scheduling/player access.

Phase 5:

- Add optional Bungee plugin only if the network actually needs it.
