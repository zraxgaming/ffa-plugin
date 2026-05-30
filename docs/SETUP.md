# Z-FFA Setup Guide

## 1. Build Or Download

Build locally:

```bash
mvn -DskipTests package
```

Or download the shaded jar from the GitHub release page.

## 2. Install

Place the shaded jar in your server `plugins` folder.

## 3. Start Once

Run the server once so Z-FFA can generate:

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

## 5. Set Up Worlds

If you use Multiverse-Core, create or import the world before setting any locations:

```text
/mv create ffa_world normal
```

## 6. Test The Core Flow

- `/zffa setlobby`
- `/zffa kit create nodebuff <gradient:red:gold>No Debuff</gradient>`
- `/zffa arena create arena1`
- `/zffa arena arena1 setspawn1`
- `/zffa arena arena1 setspawn2`
- `/zffa arena arena1 addffaspawn`
- `/ffa`
- `/duel <player>`

## 7. Party Testing

```text
/party create
/party invite PlayerName
/party accept
/party duel nodebuff
```

## 8. Don’t Forget

- Use `zf.player` for normal players.
- Use `zf.admin` for setup.
- Add `zf.kit.<kit>` if you want kit-specific access.
- Check `/zffa reload` after editing config files.
