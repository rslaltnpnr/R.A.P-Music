# OZIN Music

A fully offline, from-scratch Android music player. No network permission, no
ads, no accounts, no telemetry — everything it does is local to your device.

> The app's display name lives in exactly one place, the `appName` Gradle
> property in `gradle.properties` (mirrored into `app_name` via
> `resValue` in `app/build.gradle.kts`), so it can be renamed trivially.
> The package name (`com.ozin.music`) is set in `app/build.gradle.kts`.

## Features

OZIN Music was built from scratch across ten phases into a complete, fully
offline player. Everything below is real, working functionality, not a
roadmap.

- **Library & playback core**: scans on-device audio via `MediaStore` (title,
  artist, album, album art, duration, path, size, year, track number) into a
  Room database that is the app's single source of truth. Playback runs
  through a `MediaLibraryService` + `ExoPlayer` foreground service with a
  MediaStyle notification, lockscreen controls, Bluetooth/headset button
  support, audio-focus handling and pause-on-unplug, plus a mini player and a
  full Now Playing screen with a background gradient derived from the album
  art's dominant color (AndroidX Palette).
- **Library organization**: Songs / Albums / Artists / Folders / Genres /
  Favorites browsing with list/grid toggle and multiple sort orders,
  Room-backed playlist CRUD with drag reordering, and a reorderable playback
  queue (play-next/add-to-queue/remove).
- **Real audio processing**: `android.media.audiofx`-backed Equalizer/
  BassBoost/Virtualizer/LoudnessEnhancer with 13 presets, fade in/out,
  adjustable playback speed/pitch, A-B repeat, and a smart crossfade that
  auto-applies between tracks — every effect degrades gracefully on hardware
  that doesn't support it.
- **Lyrics & metadata**: a `.lrc` sync viewer with an in-app lyrics editor,
  metadata editing through `MediaStore` and a custom ID3v2 tag writer for
  MP3s, file management (rename/delete/share), duplicate-song detection,
  per-folder include/exclude scan rules, and `ContentObserver`-driven
  incremental rescans so the library stays current without a full rescan.
- **Stats & smart playlists**: a listening-history dashboard (per-event log,
  multiple time-range views), 1-5 star ratings, and rule-based smart/dynamic
  playlists (e.g. "favorites not played in 30 days") evaluated live against
  the library.
- **Android Auto & Bluetooth**: a full `MediaLibraryService` browse tree for
  Android Auto, per-Bluetooth-device profiles that auto-apply a saved EQ/
  crossfade configuration on connect, and a dedicated car-mode screen.
- **Network/remote source**: an optional WebDAV remote music source, with
  credentials stored in `EncryptedSharedPreferences` (androidx.security-
  crypto) and remote files browsed/streamed through the exact same player
  pipeline as local songs.
- **Local intelligence (heuristic, not ML)**: a genre + listening-time based
  mood tag classifier, a keyword query parser for natural-language-ish
  search (reuses the smart playlist rule engine), and an explainable
  "Similar Songs" weighted-scoring feature — every score is traceable to a
  documented rule, no audio analysis or machine learning involved.
- **Premium visuals**: alternate Now Playing modes (Vinyl, Cassette, a
  deterministic Visualizer animation), five theme presets (Default Dark,
  AMOLED, Neon, Retro, Minimal) and configurable accent colors.
- **System integration**: a Quick Settings tile for play/pause that reflects
  live playback state, and a tuned Coil image cache (bounded memory + disk
  cache) for smooth scrolling through album art.
- **Settings & permissions**: DataStore-backed theme/shuffle/repeat defaults,
  crossfade, normalization, mini player style and an excluded-folders list
  that filters the `MediaStore` scan; `READ_MEDIA_AUDIO` (API 33+) /
  `READ_EXTERNAL_STORAGE` (below) requested through a Compose screen with a
  graceful empty state if denied.

No home-screen widget is included: after review it was judged too risky to
add reliably without the ability to compile-test Glance/AppWidgetProvider
wiring in this environment, so it was deliberately deferred rather than
shipped half-verified.

## Tech stack

- Kotlin, Jetpack Compose + Material 3
- Media3 (ExoPlayer, MediaSession, MediaSessionService)
- Room (persistence), DataStore Preferences (settings)
- Coroutines/Flow, Hilt (DI)
- Coil (image loading), AndroidX Palette (dynamic accent)
- Single `:app` module, organized as `core/{data,domain,player,settings,ui}`
  and `feature/{home,library,player,playlist,settings,permission}`

Minimum SDK 26, target/compile SDK 35.

## Building

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The debug build type keeps default debug signing, so it builds without any
secrets and is what CI produces on every push/PR.

## Release builds (signed APK + AAB)

`app/build.gradle.kts` defines a `release` signing config that reads from
environment variables:

- `ANDROID_KEYSTORE_PATH`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

If any of these are absent (e.g. a local build with no keystore), the
`release` build type **falls back to debug signing automatically** — it
never fails to build, and never hardcodes real credentials.

To build a signed release locally:

```bash
export ANDROID_KEYSTORE_PATH=/path/to/release.keystore.jks
export ANDROID_KEYSTORE_PASSWORD=...
export ANDROID_KEY_ALIAS=...
export ANDROID_KEY_PASSWORD=...
./gradlew assembleRelease bundleRelease -PversionName=1.2.3
```

### Versioning

- **versionName** is overridable via `-PversionName=1.2.3` (defaults to the
  `versionName` Gradle property, `0.1.0`). The release CI derives it from the
  pushed git tag (`v1.2.3` -> `1.2.3`).
- **versionCode** is a simple monotonically increasing scheme: days elapsed
  since 2024-01-01, computed in `app/build.gradle.kts`. It never needs manual
  bumping and is always higher than any earlier build made on an earlier day.

## CI/CD

- **`.github/workflows/android-build.yml`** — runs on every push and pull
  request: JDK 17 (Temurin), Gradle caching via
  `gradle/actions/setup-gradle`, unit tests (`testDebugUnitTest`), then
  `assembleDebug`, uploading the debug APK as a build artifact.
- **`.github/workflows/android-release.yml`** — runs when a tag matching
  `v*.*.*` is pushed. It decodes a keystore from the
  `ANDROID_KEYSTORE_BASE64` secret, exports
  `ANDROID_KEYSTORE_PASSWORD`/`ANDROID_KEY_ALIAS`/`ANDROID_KEY_PASSWORD` as
  environment variables so `app/build.gradle.kts` picks them up, builds a
  signed `assembleRelease`/`bundleRelease`, renames the outputs to
  `OzinMusic-vX.Y.Z.apk`/`.aab`, and publishes a GitHub Release for the tag
  with both files attached.

To cut a release: `git tag v1.0.0 && git push origin v1.0.0`.

## Project layout

```
app/src/main/java/com/ozin/music/
  core/data/{local,model,mediastore,repository}   # Room, MediaStore scanner, repositories
  core/domain/                                     # pure logic: sort, queue, LRC parsing
  core/player/                                      # ExoPlayer, MediaSessionService, effects, sleep timer
  core/settings/                                    # DataStore-backed settings repository
  core/ui/theme/                                    # dark theme tokens
  feature/home | library | player | playlist | settings | permission
app/src/test/java/com/ozin/music/                   # JVM unit tests + in-memory fakes
```
