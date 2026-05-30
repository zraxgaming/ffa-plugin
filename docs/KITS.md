# Z-FFA Kits Guide

Kits are stored in `kits.yml`.

## Structure

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
    armor:
      helmet: "DIAMOND_HELMET:1"
      chestplate: "DIAMOND_CHESTPLATE:1"
      leggings: "DIAMOND_LEGGINGS:1"
      boots: "DIAMOND_BOOTS:1"
    effects:
      - "SPEED:infinite:1"
```

## Item Format

- `MATERIAL:AMOUNT`
- Potions: `SPLASH_POTION:healing:28`

## Effects Format

- `EFFECT_NAME:duration:amplifier`
- Use `infinite` for no expiration

## Useful Commands

```text
/zffa kit create <name> [display]
/zffa kit save <name> [display]
/zffa kit seticon <name> <material>
/zffa kit delete <name>
/zffa kit list
```

## Tips

- Keep kit IDs lowercase.
- Use simple, readable display names.
- Check `docs/PLACEHOLDERS.md` for placeholder guidance if you reuse kit names in menus.
- Run `/zffa reload` after editing `kits.yml`.
