# F76 Checker

An Android checklist for **Fallout 76** items. Tick off the weapons you own and see your progress.

- **267 weapons** taken from the Fallout Wiki's [Fallout 76 weapons](https://fallout.fandom.com/wiki/Fallout_76_weapons) page, each with its wiki picture (cut weapons are left out)
- A checkbox on every item; checks are saved on the device
- **English / Español** toggle in the top bar. Spanish names come from the Spanish wiki (see below)
- Search (works with accents or without, in both languages), plus All / Owned / Missing filters, with progress shown for the whole list and for each section
- Long-press an item to open its wiki page
- **Auto-updater**: on launch, the app checks this repo's latest GitHub release and offers to download and install a newer APK

## Install

Download the latest `F76Checker-x.y.z.apk` from [Releases](../../releases/latest) and open it on your phone. Android will ask you to allow installs from that source. Later versions install from inside the app.

## Spanish names

Each Spanish name in `app/src/main/assets/weapons.json` has an `esSource` field:

| esSource | Meaning | Count |
|---|---|---|
| `list` | Name taken from the Spanish wiki list [Armas de Fallout 76](https://fallout.fandom.com/es/wiki/Armas_de_Fallout_76) | 84 |
| `article` | Name of the matching Spanish wiki article (found through the wiki's interlanguage links) | 32 |
| `translated` | The Spanish wiki has no entry yet, so the name is translated using the wiki's naming conventions | 151 |

The Spanish list page is still marked *En construcción*. Its melee and trap tables are empty and many unique weapons are missing. When it gets filled in, update `tools/es_names.py` and rebuild the JSON.

## Adding categories (armor, power armor, …)

1. Create `app/src/main/assets/<category>.json` in the same format as `weapons.json`
2. Add a `Category(...)` entry to `CATEGORIES` in `Data.kt`. Tabs appear automatically once there is more than one category
3. Add any new section labels to `Strings.sections`

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

`tools/` holds the scripts that produced `weapons.json`. They use the MediaWiki API: `scrape_en.py` parses the English list and resolves image URLs, `es_names.py` holds the Spanish names, and `build_json.py` writes the asset file.

Item names and images belong to the [Fallout Wiki](https://fallout.fandom.com) (CC BY-SA). Fallout is a trademark of Bethesda Softworks. This is an unofficial fan project.
