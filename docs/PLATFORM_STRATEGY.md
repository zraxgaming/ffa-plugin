# Platform Strategy

Z-FFA should stay simple to ship: one plugin jar, with config deciding whether it runs fully standalone on Paper/Purpur or cooperates with the bundled Velocity, BungeeCord, or Waterfall proxy coordinator.

## Current Target

- One artifact: `z-ffa-core`.
- Bukkit reads `plugin.yml` on backend servers.
- Velocity reads the generated Velocity plugin metadata.
- BungeeCord/Waterfall reads `bungee.yml` on the proxy.
- Put the same jar on the proxy and on every FFA backend when proxy-assisted routing is enabled.
- Compile against Java 17 and an older Paper API in the supported band.
- Runtime target: Paper/Purpur `1.19` through current releases.
- Avoid direct calls to version-sensitive APIs unless they are guarded by compatibility helpers.
- Keep Folia as a future scheduler-adapter project, not part of the default jar until it is genuinely supported.

## Deployment Modes

### Standalone

The default mode. One backend server handles queues, duels, FFA sessions, parties, menus, profiles, and storage.

```yaml
settings:
  proxy:
    mode: "standalone"
```

### Backend With Proxy Coordinator

Use this on Velocity, BungeeCord, or Waterfall networks where the proxy should handle global queue routing and backend selection. The backend still owns arena execution, kit application, combat rules, and match lifecycle.

```yaml
settings:
  proxy:
    mode: "backend"
    server-id: "ffa-1"
    channel: "zffa:main"
    report-capacity: true
    route-queues: true
```

The backend sends capacity reports per kit:

```text
capacity|<server-id>|<kit>|<ready-arenas>|<free-arenas>|<local-queued>
```

When queue routing is enabled, it can ask a proxy coordinator to own queue decisions:

```text
queue-join|<server-id>|<uuid>|<name>|<kit>|<ranked>
party-queue-join|<server-id>|<leader-uuid>|<member-uuids>|<kit>|<ranked>
queue-leave|<server-id>|<uuid>
```

## Compatibility Rules

- Do not make one jar per sub-version.
- Prefer Paper/Purpur APIs available since `1.19`.
- For changed APIs, use a compatibility helper instead of raising the baseline.
- Keep newer materials or potion names config-friendly and fallback safely when a server does not know them.
- Keep the proxy option optional so single-server installs do not need a proxy.

## Future Work

- Expand backend-proxy messages for richer cross-server parties and player routing.
- Add scheduler adapters before claiming Folia support.
