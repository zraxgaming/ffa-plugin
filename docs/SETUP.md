# Z-FFA Setup Guide

## Install

Put the jar in your server plugins folder:

```text
target/z-ffa-core-1.3.1.jar
```

Start once to generate files:

```text
plugins/Z-FFA/config.yml
plugins/Z-FFA/menus.yml
plugins/Z-FFA/arenas.yml
```

## Multiverse

Create or import your world first:

```text
/mv create ffa_world normal
```

Then teleport there before setting Z-FFA locations.

## Create A Kit

Put items in your inventory and armor slots:

```text
/zffa kit create nodebuff <gradient:red:gold>No Debuff</gradient>
```

Optional:

```text
/zffa kit seticon nodebuff POTION
/zffa kit setting nodebuff allow-hunger false
```

Kits save inventory, armor, and active potion effects.

## Create An Arena

```text
/zffa arena create arena1
/zffa arena arena1 setspawn1
/zffa arena arena1 setspawn2
/zffa arena arena1 addkit nodebuff
```

## Add FFA Spawns

Stand at each respawn point:

```text
/zffa arena arena1 addffaspawn
```

Add at least 3-5 FFA spawns.

## Test

Duel queue:

```text
/duel
```

FFA:

```text
/ffa arena arena1 nodebuff
```

VIP FFA:

```text
/zffa arena arena1 vip true
/ffa viparena
```

Party duel:

```text
/party create
/party invite PlayerName
/party duel nodebuff
```

The other party queues the same kit. The plugin finds an empty arena connected to that kit, puts one party at `spawn1`, and the other party at `spawn2`.

Debug:

```text
/zffa arena arena1 info
```
