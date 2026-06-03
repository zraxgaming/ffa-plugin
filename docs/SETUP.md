# Z-FFA Setup Guide

## 1. Download

Download the latest ZIP bundle from the GitHub release page. It contains the plugin jar, changelog, license, and docs.

## 2. Install

For a single Paper/Purpur server, place the jar from the ZIP in the server `plugins` folder.

For a network, place the same jar on:

- each Paper/Purpur FFA backend
- the Velocity proxy `plugins` folder, or the Bungee/Waterfall proxy `plugins` folder

## 3. Start Once

Run the backend server once so Z-FFA can generate:

```text
plugins/Z-FFA/config.yml
plugins/Z-FFA/messages.yml
plugins/Z-FFA/menus.yml
plugins/Z-FFA/arenas.yml
plugins/Z-FFA/kits.yml
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
- `/ffamenu`
- `/ffaarenas`
- `/ranked`
- `/unranked`
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
