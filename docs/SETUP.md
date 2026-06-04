# Z-FFA Setup Guide

## 1. Download

Download the latest ZIP bundle from the GitHub release page. It contains the plugin jar, changelog, license, and docs.

## 2. Install

For a single backend server, place the matching jar from `backend-full/` in the server `plugins` folder:

- Spigot: `*-backend-full-spigot.jar`
- Paper: `*-backend-full-paper.jar`
- Purpur: `*-backend-full-purpur.jar`
- Folia: `*-backend-full-folia.jar`

For a network, use the `split-install/` jars:

- place the matching split backend jar on each FFA backend
- place the Velocity proxy jar on Velocity, or the Bungee proxy jar on Bungee/Waterfall

Split backend choices:

- Spigot: `*-backend-split-spigot.jar`
- Paper: `*-backend-split-paper.jar`
- Purpur: `*-backend-split-purpur.jar`
- Folia: `*-backend-split-folia.jar`

## 3. Start Once

Run the backend server once so Z-FFA can generate:

```text
plugins/Z-FFA/config.yml
plugins/Z-FFA/messages.yml
plugins/Z-FFA/menus.yml
plugins/Z-FFA/arenas.yml
plugins/Z-FFA/kits.yml
```

Run the proxy once so the proxy coordinator can generate its own config:

```text
Velocity: plugins/zffa/zffa-proxy.properties
Bungee/Waterfall: plugins/Z-FFA/zffa-proxy.properties
```

## 4. Configure The Basics

- Set lobby items in `config.yml`
- Edit menu text in `menus.yml`
- Edit chat text in `messages.yml`
- Create kits in `kits.yml`
- Create arenas in `arenas.yml`

For placeholder syntax, see [PLACEHOLDERS.md](PLACEHOLDERS.md).

## 5. Choose Deployment Mode

Standalone backend:

```yaml
settings:
  proxy:
    mode: "standalone"
```

Standalone mode is the normal one-server setup. The backend owns queues, menus, matches, FFA sessions, profiles, kits, arenas, and storage.

Proxy-assisted backend:

```yaml
settings:
  proxy:
    mode: "backend"
    server-id: "ffa-1"
    route-queues: true
    report-capacity: true
```

Use proxy-assisted mode when Velocity, BungeeCord, or Waterfall should own global queues and send players to the least busy FFA backend.

Proxy coordinator config:

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

Keep `channel` the same in the backend `settings.proxy.channel` and the proxy `zffa-proxy.properties`. Keep `queue.require-free-arena=true` if you want the proxy to wait until a backend reports free arena capacity before it starts a match.

## 6. Set Up Worlds

If you use Multiverse-Core, create or import the world before setting any locations:

```text
/mv create ffa_world normal
```

## 7. Test The Core Flow

- `/zffa setlobby`
- `/zffa kit create nodebuff <gradient:red:gold>No Debuff</gradient>`
- `/zffa arena create arena1`
- `/zffa arena arena1 setspawn1`
- `/zffa arena arena1 setspawn2`
- `/zffa arena arena1 addffaspawn`
- `/ranked`
- `/unranked`
- `/ffamenu`
- `/ffaarenas`
- `/duel <player>`
- `/zffa manage`
- `/zffa kiteditor`

## 8. Party Testing

```text
/party create
/party invite PlayerName
/party accept
/party duel nodebuff
```

## 9. Don't Forget

- Use `zf.player` for normal players.
- Use `zf.admin` for setup.
- Add `zf.kit.<kit>` if you want kit-specific access.
- Add `zf.viparena` for VIP FFA arenas.
- Check `/zffa reload` after editing config files.
- Leave `settings.auto-update-configs` enabled if you want new default config keys added automatically after updates.
