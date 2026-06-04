# Platform Strategy

Z-FFA ships as version-targeted plugin bundles. Each bundle supports a full-backend install and a split proxy/backend install.

## Current Target

- Release targets: Minecraft `1.20`, `1.21`, `1.26.0`, and `1.26.1`.
- Full backend artifact: one backend jar that owns queues, duels, FFA, menus, stats, storage, and PlaceholderAPI.
- Split install artifacts: one backend jar plus Velocity and Bungee/Waterfall proxy coordinator jars.
- Bukkit reads `plugin.yml` on backend servers.
- Velocity reads the generated Velocity plugin metadata.
- BungeeCord/Waterfall reads `bungee.yml` on the proxy.
- Put backend jars only on backend servers and proxy jars only on proxies.
- Compile against Java 17 and an older Paper API in the supported band.
- Runtime target: Spigot-compatible backend servers for the selected target version, including Spigot, Paper, Purpur, and Folia-compatible forks where their Bukkit scheduler compatibility layer supports classic plugins.
- Avoid direct calls to version-sensitive APIs unless they are guarded by compatibility helpers.
- Keep Folia as a future scheduler-adapter project, not part of the default jar until it is genuinely supported.

## Deployment Modes

### Standalone

The default mode. Use the jar from `backend-full/`. One backend server handles queues, duels, FFA sessions, parties, menus, profiles, placeholders, and storage.

```yaml
settings:
  proxy:
    mode: "standalone"
```

### Backend With Proxy Coordinator

Use this on Velocity, BungeeCord, or Waterfall networks where the proxy should handle global queue routing and backend selection. Use the proxy jar on the proxy and the split backend jar on each backend. The backend still owns arena execution, kit application, combat rules, and match lifecycle.

```yaml
settings:
  proxy:
    mode: "backend"
    server-id: "ffa-1"
    channel: "zffa:main"
    report-capacity: true
    route-queues: true
```

The backend sends batched capacity reports:

```text
capacity-bulk|<server-id>|<kit>,<ready-arenas>,<free-arenas>,<local-queued>[;...]
```

When queue routing is enabled, it can ask a proxy coordinator to own queue decisions:

```text
queue-join|<server-id>|<uuid>|<name>|<kit>|<ranked>
party-queue-join|<server-id>|<leader-uuid>|<member-uuids>|<kit>|<ranked>
queue-leave|<server-id>|<uuid>
```

## Compatibility Rules

- Build only the named target lines, not every patch release inside them.
- Prefer Bukkit/Paper APIs available since `1.20`.
- For changed APIs, use a compatibility helper instead of raising the baseline.
- Keep newer materials or potion names config-friendly and fallback safely when a server does not know them.
- Keep the proxy option optional so single-server installs do not need a proxy.

## Future Work

- Expand backend-proxy messages for richer cross-server parties and player routing.
- Add scheduler adapters before claiming Folia support.
