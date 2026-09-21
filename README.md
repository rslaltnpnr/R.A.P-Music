# OZIN Music

A fully offline, from-scratch Android music player. No network permission, no
ads, no accounts, no telemetry — everything it does is local to your device.

> The app's display name lives in exactly one place, the `appName` Gradle
> property in `gradle.properties` (mirrored into `app_name` via
> `resValue` in `app/build.gradle.kts`), so it can be renamed trivially.
> The package name (`com.ozin.music`) is set in `app/build.gradle.kts`.

## Features

- **Library**: scans on-device audio via `MediaStore` (title, artist, album,
  album art, duration, path, size, year, track number) into a Room database
  that is the app's single source of truth.
- **Home**: time-of-day greeting, recently played, recently added, most
  played, favorites and playlists — all backed by real Room queries.
- **Library browser**: Songs / Albums / Artists / Folders / Genres /
  Favorites, list/grid toggle, and sort by title, date added, most played,
  duration, artist or album.
- **Now Playing**: large album art, seek bar, shuffle/repeat, favorite,
  add-to-playlist, queue sheet, and a background gradient derived from the
  album art's dominant color via the AndroidX Palette API.
- **Background playback**: a `MediaSessionService` + `ExoPlayer` foreground
  service with a MediaStyle notification, lockscreen controls, Bluetooth/
  headset button support, audio-focus handling and pause-on-unplug — all via
  Media3's standard wiring.
- **Queue**: reorderable, with play-next/add-to-queue/remove.
- **Playlists**: Room-backed CRUD with reordering.
- **Favorites** and **listening stats** (play count, last played), both
  persisted in Room and feeding the Home/Library screens.
- **Search**: filters songs/artists/albums/playlists as you type.
- **Equalizer**: real `android.media.audiofx` Equalizer/BassBoost/
  Virtualizer/LoudnessEnhancer attached to ExoPlayer's audio session, with
  presets; every effect is created defensively so unsupported hardware never
  crashes playback.
- **Sleep timer**: fixed durations, "end of track", volume fade-out.
- **Lyrics**: looks for a same-name `.lrc` file next to the audio file and
  displays it synced to playback position.
- **Settings**: DataStore-backed theme/shuffle/repeat defaults, crossfade,
  normalization, mini player style and an excluded-folders list that filters
  the MediaStore scan.
- **Permissions**: `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE`
  (below), requested through a Compose screen with a graceful empty state if
  denied.

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
