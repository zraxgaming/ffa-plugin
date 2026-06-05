# Z-FFA Proxy Install

Use proxy mode when Velocity, BungeeCord, or Waterfall should route players across multiple FFA backends.

## Jars

Put a split backend jar on each backend:

- Spigot: `*-backend-split-spigot.jar`
- Paper: `*-backend-split-paper.jar`
- Purpur: `*-backend-split-purpur.jar`
- Folia: `*-backend-split-folia.jar`

Put one proxy coordinator jar on the proxy:

- Velocity: `*-proxy-velocity.jar`
- Bungee/Waterfall: `*-proxy-bungee.jar`

Do not put proxy jars on backend servers. Do not put backend jars on the proxy.

## Backend Config

In each backend `plugins/Z-FFA/config.yml`:

```yaml
settings:
  proxy:
    mode: "backend"
    channel: "zffa:main"
    server-id: "ffa-1"
    report-capacity: true
    report-interval-seconds: 5
    route-queues: true
```

Use a unique `server-id` per backend.

## Proxy Config

The proxy creates `zffa-proxy.properties` on first start:

```text
Velocity: plugins/zffa/zffa-proxy.properties
Bungee/Waterfall: plugins/Z-FFA/zffa-proxy.properties
```

Recommended values:

```properties
enabled=true
channel=zffa:main
queue.enabled=true
queue.max-size-per-kit=500
queue.require-free-arena=true
queue.connect-delay-millis=1000
capacity.stale-seconds=15
server-selection.min-free-arenas=1
fallback-to-source=false
```

## How Routing Works

Backends report capacity per kit:

```text
capacity-bulk|<server-id>|<kit>,<ready-arenas>,<free-arenas>,<local-queued>[;...]
```

When queue routing is enabled, backends ask the proxy to own queue placement:

```text
queue-join|<server-id>|<uuid>|<name>|<kit>|<ranked>
party-queue-join|<server-id>|<leader-uuid>|<member-uuids>|<kit>|<ranked>
queue-leave|<server-id>|<uuid>
```

The proxy chooses a backend with free capacity, connects players there, and sends the backend a start request.

## Tuning

- `queue.max-size-per-kit` caps queued players per kit.
- `queue.require-free-arena=true` keeps players queued until capacity exists.
- `capacity.stale-seconds` removes backends that stopped reporting.
- `server-selection.min-free-arenas` controls how much capacity a backend must have before selection.
- `fallback-to-source=false` avoids starting matches on a source backend with unknown capacity.

## Troubleshooting

Check these first:

- Backend and proxy `channel` values match.
- Every backend has a unique `server-id`.
- Backend arenas are ready for the queued kit.
- Proxy jar is installed on the proxy only.
- Backend split jar is installed on backend servers only.
