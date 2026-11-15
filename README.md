# F76 Checker

An Android checklist for **Fallout 76** items. Tick off what you own and see your progress.

| Category | Contents | Source |
|---|---|---|
| Weapons | 267 weapons (cut weapons left out) | [Fallout 76 weapons](https://fallout.fandom.com/wiki/Fallout_76_weapons) |
| Power armor sets | 12 sets × 6 pieces = 72 | [Power armor (Fallout 76)](https://fallout.fandom.com/wiki/Power_armor_(Fallout_76)) |
| Armor sets | 18 sets × 6 pieces = 108 | [Fallout 76 armor and apparel → Armor](https://fallout.fandom.com/wiki/Fallout_76_armor_and_apparel) |

- **Home page** with a card for each category, overall progress, and progress per category
- **Side menu** (☰ button, or swipe from the left edge) with Home and every category
- A checkbox on every item; checks are saved on the device. On set pages each set shows its picture, a progress bar and a "whole set" checkbox that ticks or clears all 6 pieces at once
- **English / Español** toggle in the top bar. Spanish names come from the Spanish wiki (see below)
- **UI sounds** for checking, unchecking, completing a set, navigating, the menu, filters and the language switch. You can turn them off from the ⋮ menu
- Search (works with accents or without, in both languages), plus All / Owned / Missing filters
- Long-press an item or a set to open its wiki page
- **Auto-updater**: on launch, the app checks this repo's latest GitHub release and offers to download and install a newer APK

## Install

Download the latest `F76Checker-x.y.z.apk` from [Releases](../../releases/latest) and open it on your phone. Android will ask you to allow installs from that source. Later versions install from inside the app.

## Spanish names

Every item and set in the asset JSON files has an `esSource` field.

**Weapons** (`weapons.json`)

| esSource | Meaning | Count |
|---|---|---|
| `list` | Name taken from the Spanish wiki list [Armas de Fallout 76](https://fallout.fandom.com/es/wiki/Armas_de_Fallout_76) | 84 |
| `article` | Name of the matching Spanish wiki article (found through the wiki's interlanguage links) | 32 |
| `translated` | The Spanish wiki has no entry yet, so the name is translated using the wiki's naming conventions | 151 |

**Power armor and armor** (`power_armor.json`, `armor.json`): set and piece names follow the official Spanish names used by the Spanish wiki. The main sources are [Planos de Fallout 76/Armadura](https://fallout.fandom.com/es/wiki/Planos_de_Fallout_76/Armadura) and the Spanish power armor paint pages, for example *Servoarmadura Arpía* (Hellcat), *Servoarmadura del Sindicato* (Union), *Armadura de marine polar* (Arctic marine) and *Armadura con púas* (Thorn). The Spanish wiki has nothing yet on the **Vulcan** and **Strangler heart** power armors, so their names (*Servoarmadura Vulcano*, *Servoarmadura de corazón estrangulador*) are marked `translated`.

Leather armor and raider armor have no helmet in the game. The app still lists a helmet slot for them, so that every set has 6 pieces; in the JSON these entries carry `"note": "no-ingame-helmet"`.

## Adding a category

1. Create `app/src/main/assets/<category>.json` with `sections` and `items` arrays, in the same format as the existing files
2. Add a `Category(...)` entry to `CATEGORIES` in `Data.kt`. Set `isSets = true` for set-based categories. The category then shows up on the home page and in the side menu

## Building and releasing

The build needs JDK 17 and the Android SDK (API 34).

```bash
./gradlew assembleRelease
```

Release builds are signed with the key described in `keystore.properties`, which is git-ignored. **Every release must be signed with the same key**, or Android will refuse to install it as an update. Back up `keystore/release.jks` and `keystore.properties`.

To publish an update:

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`
2. `./gradlew assembleRelease`
3. `gh release create vX.Y.Z app/build/outputs/apk/release/F76Checker-X.Y.Z.apk --title "vX.Y.Z" --notes "..."`

The updater reads the `tag_name` (`vX.Y.Z`) and the first `.apk` asset of the latest release.

## Data scripts

The `tools/` folder holds the scripts that produced the app's data and sounds. The data scripts use the MediaWiki API.

| Script | What it does |
|---|---|
| `scrape_en.py` | Parses the English weapons list and resolves image URLs |
| `es_names.py` | Holds the Spanish weapon names |
| `build_json.py` | Writes `weapons.json` |
| `build_sets.py` | Writes `power_armor.json` and `armor.json`: set and piece names (EN/ES) and per-piece images |
| `gen_sounds.py` | Synthesizes the UI sound effects into `res/raw` (generated from scratch, no third-party audio) |

Item names and images belong to the [Fallout Wiki](https://fallout.fandom.com) (CC BY-SA). Fallout is a trademark of Bethesda Softworks. This is an unofficial fan project.
