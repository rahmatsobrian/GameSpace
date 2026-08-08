# GameSpace (Siroha)

Android game-space/game-booster app — floating overlay assistant, per-game
monitoring, booster. Full feature list and rationale for how it's scoped
down from the original spec: see [`docs/ROADMAP.md`](docs/ROADMAP.md).

**Status: Phase 0-4 done, Phase 5 well underway.** Foundation, privilege
layer, game library, monitoring, floating overlay, and booster all have
working first slices. Within Phase 5: blocker & privacy is done, the
overlay has Quick Toggles and Timer/Stopwatch, and Settings + DataStore
now actually persists what used to reset every launch (theme, grid/list
view, onboarding) plus library backup/restore. See ROADMAP for exactly
what's deliberately deferred within each phase, and what's still left.

## What's actually working in this slice

- Gradle project (Kotlin DSL, version catalog), Hilt, Compose, Material 3
  wired up and building a real APK shape (namespace `com.siroha.gamespace`,
  minSdk 29 / Android 10, target/compileSdk 36 / Android 16).
- **Privilege layer** (`core/privilege/`): detects and requests root (via
  [libsu](https://github.com/topjohnwu/libsu) — works with su from Magisk,
  KernelSU, or APatch interchangeably) and Shizuku, exposed behind one
  `PrivilegeRepository` interface so every later feature depends on the
  interface, never on which tier is actually active. Root is preferred
  over Shizuku when both are granted.
- **System Access screen** (`feature/systemaccess/`): shows live
  root/Shizuku status, requests each, opens Shizuku or its releases page
  if it's missing/not running, and degrades cleanly to "public API only"
  if the user skips both. First thing shown on launch, one time.
- **Game library / Home** (`data/game/`, `feature/home/`): scans installed
  launchable apps for `CATEGORY_GAME`, lets the user add/remove anything
  else manually, persists library membership + favorites in Room, shows
  real app icons (decoded from `PackageManager`, no Coil needed for this),
  search, grid/list view, and — if the user grants Usage Access — real
  last-played and approximate playtime per game, no root required. Tap a
  card to actually launch the game.
- **Monitoring engine** (`data/monitoring/`, `feature/devicestatus/`):
  RAM/battery/thermal poll every 1.5s with zero special access needed.
  CPU is the interesting one — it genuinely has no public API on Android
  10-15 (confirmed during this phase, see ROADMAP), so it's the privilege
  layer's first real caller: locked on the public-only tier, a real
  percentage once root or Shizuku is granted. Reachable from Home's info
  icon; the CPU card's lock message links straight back to System Access.
- **Floating overlay — first slice** (`core/overlay/`, `feature/overlay/`,
  `data/quicktoggle/`): a real `WindowManager` overlay bubble, toggled
  on/off from Device Status. Three pages now: Monitor (the four metrics
  from Phase 2), Tools (WiFi/Bluetooth/rotation-lock quick toggles — real
  instant toggles with root/Shizuku, a settings-screen shortcut without),
  and Timer/Stopwatch. Drag the header to move; tap the title to
  expand/collapse. Still no resize, opacity, or notch/foldable-specific
  layout, and most of the 60+ tool list is still unbuilt — see ROADMAP
  for exactly what's deliberately deferred vs. what's real today.
- **Booster — first slice** (`core/settings/`, `data/booster/`,
  `feature/booster/`): brightness and Do Not Disturb work with no root
  (their own independent special permissions); animation scale and
  refresh rate need root or Shizuku — real-world evidence gathered while
  building this phase shows they need `WRITE_SECURE_SETTINGS`, not the
  `WRITE_SETTINGS` brightness uses, despite living in a similarly-named
  settings table. Four honest, separately-gated controls, not one
  "Ultra Mode" button hiding what it actually does.
- **Blocker & privacy — first slice** (`core/blocker/`,
  `feature/blocker/`): notification blocking (`NotificationListenerService`)
  and call blocking (`CallScreeningService` + `RoleManager`), both
  system-invoked automatically once their respective access is granted —
  this app never starts/stops either service itself, only flips what they
  do when invoked. "SMS Blocker" from the original spec turned out not to
  need its own feature — see ROADMAP for why blocking notifications
  already covers it without needing default-SMS-app status.
- **Settings, persistence, and backup — first slice** (`data/settings/`,
  `data/backup/`, `feature/settings/`): dark/light/system + AMOLED +
  Dynamic Color now actually drive `GameSpaceTheme` from a real
  `Preferences` DataStore instead of that composable's parameters sitting
  at hardcoded defaults; Home's grid/list choice persists too. System
  Access now marks itself complete so returning users land on Home
  instead of onboarding every launch. Game library export/import as JSON
  via Storage Access Framework — scoped to the library specifically
  (favorites, manual adds, dismissals), not a full app backup.
- Material 3 theme with Dynamic Color (Android 12+) and a branded fallback
  for older versions, plus an AMOLED true-black toggle — see
  *Design decisions* below for why the palette looks the way it does.

## Architecture

Single Gradle module (`:app`), Clean-Architecture-shaped through package
boundaries instead of true Gradle multi-module:

```
core/
  di/          Hilt modules
  privilege/   root / Shizuku / public-API abstraction (domain + data, together for now)
  usage/       PACKAGE_USAGE_STATS access — third kind of "more than default" access
  overlay/     SYSTEM_ALERT_WINDOW access — fourth kind
  settings/    WRITE_SETTINGS + Notification Policy access (fifth/sixth kinds) — NOT app settings, see data/settings below for that
  blocker/     Notification Listener access + CALL_SCREENING role (seventh/eighth kinds) + the SharedPreferences store both blocker services read
  theme/       Color.kt, Type.kt, Theme.kt
  navigation/  Screen routes + NavHost
  util/        small stuff with no other home yet (Drawable→ImageBitmap)
data/
  local/       Room: entity, DAO, database
  game/        domain model + repository for the game library
  monitoring/  RAM/battery/thermal/CPU sources + polling repository
  booster/     brightness/DND/animation-scale/refresh-rate repository
  quicktoggle/ WiFi/Bluetooth/rotation-lock repository, consumed by the overlay's Tools page
  settings/    app settings (theme, grid/list, onboarding) — DataStore-backed; unrelated to core/settings above despite the shared name
  backup/      game library JSON export/import
feature/
  systemaccess/  ViewModel + Compose screen
  home/          ViewModel + Compose screen
  devicestatus/  ViewModel + Compose screen
  overlay/       foreground Service + the ComposeView it hosts (no ViewModel — see design decisions)
  booster/       ViewModel + Compose screen
  blocker/       two system-bound Services (no start/stop) + ViewModel + Compose screen
  settings/      ViewModel + Compose screen (theme controls + backup/restore)
```

`data/` showed up this phase specifically because the game library is
domain data shared across features (Home now, the floating overlay and
booster later), which is a different shape of thing than `core/`'s
infrastructure (DI, theme, nav) or `feature/`'s screen-specific code.

Multi-module Gradle (separate `:core:privilege`, `:feature:home`, etc. as
their own modules with `api`/`implementation` boundaries enforced by the
build graph) is the more "enterprise" version of this and can be split out
later if the codebase grows enough to need the build-time isolation. For a
solo-maintained app it's mostly ceremony right now — the same dependency
direction (`feature` → `domain` → nothing; `data` → `domain`) is enforced
by package convention instead. `domain`/`data` aren't split into separate
packages yet either, for the same reason — `PrivilegeRepository` (interface)
and `PrivilegeRepositoryImpl` already follow the pattern so the eventual
split, if you want it, is a mechanical move, not a redesign.

## Design decisions

- **Root tier is provider-agnostic.** `RootPrivilegeSource` talks to
  whatever `su` libsu finds — no Magisk-specific or KernelSU-specific code
  path. If APatch behaves differently in practice, that's real signal to
  add a branch, not something guessed at up front.
- **Two "is this available" checks, not one.** Root can't be checked
  without prompting (obtaining the shell *is* the prompt) — the screen only
  asks when the user taps the button. Shizuku *can* be checked silently
  (`pingBinder` + `checkSelfPermission`), so its card shows accurate status
  immediately on screen load.
- **Color carries meaning, not just mood.** `core/theme/Color.kt` defines a
  semantic set (`MetricCpu`, `MetricGpu`, `MetricRam`, …) for the monitoring
  dashboard alongside the brand palette — hue maps to metric category, so
  once the dashboard exists a number's color tells you what it is before
  you read the label. Unused by anything yet (dashboard doesn't exist
  yet); defined now because it's a systemic decision.
- **Monospace is functional, not decorative.** `DataTextStyles` in
  `Type.kt` is for live numeric readouts specifically — fixed digit widths
  stop an FPS/ms counter's layout from jittering as the number changes.
- **System fonts only, no downloadable Google Font.** A custom display
  face is a reasonable later upgrade, but the certificate-hash resource
  array downloadable fonts need has to match Google's provider exactly,
  and this was written without a compiler to check it against. Add one via
  Android Studio's Resource Manager (Fonts → Downloadable) when you want
  it — that flow generates the cert array correctly for you.
- **AMOLED true-black is opt-in, not the dark-theme default.** A softer
  `#0B0D0E` is the default dark surface; flat `#000000` is a separate
  toggle. Keeps "dark mode" from reading as "black background, nothing
  else considered."
- **Library removal is soft-delete.** `GameEntity.isDismissed` marks a row
  removed rather than deleting it. A hard delete would make `rescan()`
  unable to tell "never seen this package" from "user removed it" — both
  look identical as a missing row — and would silently resurrect anything
  the user took out of their library the next time auto-scan ran. The
  manual add-game picker *can* revive a dismissed entry, since picking it
  again is an explicit user action, unlike a background rescan.
- **One usage-stats query for the whole library, not one per game.**
  `UsageStatsManager.queryUsageStats` returns system-wide results
  regardless of how narrowly you'd like to filter it, so querying inside a
  per-item loop would mean repeating the same expensive scan N times for
  no benefit — `UsageAccessSource.snapshotForAll()` batches it once.
- **No Coil.** App icons are the only image-loading need so far, and
  they're local (`PackageManager`, not network) — `Drawable→ImageBitmap`
  in `core/util/DrawableExt.kt` covers it in about a dozen lines. Worth
  revisiting once something actually needs network images.
- **Only the Material icon glyphs I'm confident ship in `material-icons-
  core`** (`Add`, `Close`, `Search`, `Refresh`, `Favorite`/`FavoriteBorder`,
  `CheckCircle`, `Warning`, `Info`) **are used anywhere.** The grid/list
  toggle on Home uses labeled `FilterChip`s instead of icons for exactly
  this reason — I wasn't confident a "grid view" glyph is in the core
  set, and pulling in `material-icons-extended` for one icon isn't a good
  trade.
- **CPU monitoring needed a real course-correction, not a patch.** The
  original Phase 2 plan (see ROADMAP) assumed `/proc/stat` was readable
  without root. Checking that against current sources before writing the
  code showed it's been blocked for regular apps since Android 8 — so
  `CpuSource` is built around the privilege layer from the start rather
  than "public API now, add a root path later." This is the payoff for
  building `core/privilege/` in Phase 0 before anything needed it yet.
- **The Android 16 CPU/GPU headroom API isn't wired up, on purpose.**
  It's real — confirmed via the NDK reference — but only the C function
  signature turned up during research, not the Java-facing class/method.
  Writing Kotlin against a guessed method name to make this phase look
  more complete would be exactly the kind of fabricated-but-plausible API
  call this project is trying to avoid. See ROADMAP for what to check
  before wiring it up.
- **The overlay bubble has no ViewModel, on purpose.** Every other screen
  uses `hiltViewModel()`, which needs a `ViewModelStoreOwner` that Compose
  Navigation or an Activity normally supplies. A Service-hosted
  `ComposeView` has neither by default — `OverlayLifecycleOwner` supplies
  a bare-bones one so the `ComposeView` itself renders, but whether
  `hiltViewModel()` specifically would resolve correctly through that
  wasn't something to gamble on. `OverlayService` injects
  `MonitoringRepository` directly instead and feeds it into the
  Composable as a plain parameter — simpler, and sidesteps a real unknown
  rather than writing code that assumes it away.
- **Drag moves the window, not a Composable offset.** The bubble's screen
  position is `WindowManager.LayoutParams.x/y`, which Compose has no
  authority over — `detectDragGestures` reports movement deltas that get
  applied to those params and pushed via `updateViewLayout`, not to a
  Compose `Modifier.offset`.
- **No bundled "performance mode" button.** Brightness, DND, animation
  scale, and refresh rate are four unrelated system settings with four
  different access requirements (two independent app-grantable special
  permissions, one root/Shizuku-gated pair). A single toggle bundling
  them would hide which parts work without any extra access and which
  don't — the individual `BoosterRepository` methods are there precisely
  so a future preset layer can compose them transparently instead of
  reimplementing anything.
- **Refresh rate and animation scale go through `execPrivileged`, not a
  direct `Settings.Global.putInt` call.** Real-world sources describing
  how people actually change these consistently need
  `WRITE_SECURE_SETTINGS`, which a normal app can never be granted
  through a Settings screen — only `adb shell pm grant` or root can do
  it. So this app doesn't try to hold that permission itself; it shells
  out `settings put ...` through whichever privileged tier is active,
  the same way `CpuSource` reads `/proc/stat`.
- **The blocker services read SharedPreferences, not DataStore.**
  `NotificationListenerService.onNotificationPosted` and
  `CallScreeningService.onScreenCall` are plain synchronous callbacks —
  there's no coroutine scope there to collect a Flow from. Everything
  else that gets built with persisted state going forward should
  probably default to DataStore per the original spec's own ask; this is
  a deliberate, narrow exception where the calling context genuinely
  requires a synchronous read.
- **"SMS Blocker" isn't a separate feature.** Real SMS interception needs
  default-SMS-app status — an entirely different, much bigger app in
  practice. Blocking notifications from every other app while active
  already covers "don't get interrupted by texts while gaming" without
  that commitment, so that's what ships instead of a half-real SMS
  feature bolted on for the sake of matching the original list item.
- **WiFi/Bluetooth toggles read and write through the shell, not their
  Java APIs.** `WifiManager.setWifiEnabled()` has done nothing on a
  targeted-API-29+ app since Android 10; `BluetoothAdapter`'s enable/
  disable met the same fate on API 33. `svc wifi enable/disable` and
  `svc bluetooth enable/disable` still work when run with root or
  Shizuku's shell-level access, so `QuickToggleRepositoryImpl` goes
  through `execPrivileged` for both — same pattern as `CpuSource` and
  Booster's animation-scale/refresh-rate controls. Bluetooth's *read*
  goes through the shell too (`settings get global bluetooth_on`)
  specifically to avoid needing the `BLUETOOTH_CONNECT` runtime
  permission API 31+ requires for `BluetoothAdapter.isEnabled` — a whole
  separate permission flow this app doesn't otherwise need anywhere.
- **The overlay's drag gesture only lives on the header row, not the
  whole expanded panel.** Once Tools/Timer added several more clickable
  rows inside the panel, wrapping all of them in the same
  `detectDragGestures` pointerInput as before risked swallowing their
  taps — nesting a drag detector around child `clickable`s is a real,
  known source of exactly that kind of bug in Compose, not something to
  assume resolves cleanly. A dedicated drag handle (the title bar)
  sidesteps the question instead of gambling on it.
- **`MainViewModel.settings` starts `null`, not a default `AppSettings`.**
  DataStore's first read is async — if `MainActivity` picked a
  destination the instant it composed, using `AppSettings()`'s defaults,
  a returning user (`onboardingCompleted = true` in the real, not-yet-
  loaded data) would see System Access flash before the real value
  arrived and corrected it. Rendering nothing for that one frame instead
  of guessing is what avoids that — see `MainActivity.GameSpaceApp`.
- **Backup is scoped to the game library, not a full app backup.**
  Favorites/manual-adds/dismissals are genuinely irreplaceable if lost;
  Booster/Blocker toggles and theme choices are device-local
  conveniences a fresh install loses nothing important by resetting.
  Backing those up too would mean more surface area for a restore to go
  subtly wrong, for not much real benefit.
- **`org.json`, not kotlinx.serialization, for the backup format.**
  `GameEntity` is four fields — adding a serialization library and its
  Gradle plugin for that is more machinery than the problem needs.
  Worth revisiting if the exported schema grows substantially.

## Building

1. Open the `GameSpace/` folder as a project in Android Studio (Meerkat
   2024.3.1+, needed for the Android 16 / API 36 SDK).
2. **Let Android Studio's Upgrade Assistant / version suggestions run
   first.** Every version in `gradle/libs.versions.toml` is a real,
   current-as-of-writing pin, not a placeholder — but this project was
   authored in a sandboxed environment with no network and no Android SDK,
   so nothing here has been through an actual Gradle sync or compile.
   Treat the first sync as the actual verification step, not a formality.
3. Gradle sync, then Run on a device/emulator running Android 10+.

### Specifically worth checking on first sync

- `dev.rikka.shizuku:api` / `:provider` version and repository — pinned
  from what's in the current Shizuku-API changelog, but if it doesn't
  resolve from `mavenCentral()`, JitPack is already added as a fallback in
  `settings.gradle.kts`.
- `RootPrivilegeSource` / `ShizukuPrivilegeSource`: each has a comment
  flagging the specific method(s) least certain to match the exact current
  library version verbatim (`Shell.getCachedShell()` / `Shell.Result.err`
  for libsu; `Shizuku.newProcess(...)`'s exact signature for Shizuku). The
  permission-request flows around them are the well-established, stable
  parts of each API.
- `ModalBottomSheet` (the add-game picker) needs `@OptIn(ExperimentalMaterial3Api::class)`
  — still marked experimental in Material3 as of whatever Compose BOM
  Android Studio resolves, which is normal, not a sign something's wrong.
- `AppOpsManager.checkOpNoThrow` in `UsageAccessSource` is the older,
  deprecated-but-functional overload — flagged in a comment as worth
  switching to `unsafeCheckOpNoThrow` once you can confirm its minimum API
  level against current docs; both work identically today.
- The `<queries>` block in the manifest is what makes the game scan see
  other apps at all on Android 11+ — if `rescan()` ever comes back empty
  on a real device, that block (not the Kotlin) is the first thing to
  check.
- `CpuSource` needs two consecutive `/proc/stat` samples before it
  reports a percentage (the first reading after gaining root/Shizuku
  access always shows "Mengukur…" — that's expected, not a bug, it just
  has nothing to diff against yet).
- **`OverlayLifecycleOwner` + the `ComposeView` in `OverlayService`
  is the one part of this whole project most worth testing on a real
  device before trusting it.** If the overlay bubble doesn't render (a
  blank/invisible window instead of the expected circle), start there —
  the individual APIs involved are each well-documented, but this exact
  combination for a Service-hosted overlay hasn't been build-verified.
- `BoosterRepositoryImpl.setRefreshRate`'s `settings delete` branch (used
  for resetting to Auto) is the one command in that file not
  double-checked against current docs the way `put`/`get` were — if
  "Auto" doesn't restore the device default but a fixed Hz value does
  work, that's the first thing to check.
- `BlockerCallScreeningService`'s exact `CallResponse.Builder` method set
  (`setDisallowCall`/`setRejectCall`/`setSkipCallLog`/`setSkipNotification`)
  is standard and long-stable, but wasn't cross-checked against a specific
  current API level the way the FGS-type property tag was — worth a quick
  look if blocked calls don't behave exactly as expected (e.g. still
  ringing before being rejected).
- Call screening specifically needs a real SIM/telephony-capable test
  device or emulator profile — it won't do anything meaningful on a
  Wi-Fi-only tablet or a bare AVD without telephony.
- The overlay header row's tap-to-collapse (on the title text) sharing
  space with the drag handle is the one gesture interaction not confirmed
  to disambiguate cleanly without a real device — see the design
  decisions above. Drag and the close button are unaffected either way.
- `svc wifi enable/disable` and `svc bluetooth enable/disable` are
  long-standing, well-known ADB shell commands, but weren't re-verified
  against current AOSP source this session the way the FGS-type property
  tag was — worth a quick manual `adb shell` check on whatever device
  this actually gets tested on before trusting the Quick Toggles page.
- `SettingsScreen`'s `ActivityResultContracts.CreateDocument`/
  `OpenDocument` launchers are a well-established, high-confidence
  pattern (unlike the MediaProjection flow deliberately not attempted —
  see ROADMAP), but export/import as a whole is still new code worth
  running once: create a library with a few games favorited, export,
  clear/reinstall, import, confirm favorites came back.
- If Home ever shows onboarding again for what should be a returning
  user, `MainViewModel.settings`/`SystemAccessViewModel.
  markOnboardingComplete` is where to look first — see the design
  decision on why that state starts `null`.

## Continuous Integration

`.github/workflows/android-build.yml` builds a debug APK on every push/PR
to `main`/`master`, plus a manual trigger from the Actions tab.

**Revised after the first real run failed.** Every `./gradlew` step
errored with `No such file or directory`. The build-log artifact only
captured output starting partway through the job at the time, so the
exact failing step wasn't directly visible in that one file — but
separately-verified evidence turned up an active, currently-unresolved
GitHub-hosted-runner issue: `sdkmanager` is missing/broken on current
runner images (`actions/runner-images#13674`). That fully explains the
cascade — an early step failing (without `if: always()`) silently skips
everything after it except the `if: always()` steps, which matched
exactly what the log showed. Two changes:

1. **Gradle is now installed by hand** (`curl` + `unzip` + `$GITHUB_PATH`)
   instead of through `gradle/actions/setup-gradle`. Not a claim that
   action is broken — a manual install just has fewer moving parts to be
   uncertain about, and every step of it is independently checkable in
   the log (`gradle --version`, `ls -la gradlew`) instead of trusting one
   action call to have done the right thing silently.
2. **The Android SDK step no longer assumes `sdkmanager` works.** It's
   wrapped in `continue-on-error: true` and checks `sdkmanager --version`
   first — GitHub's own runner-images docs show platform 36 already
   pre-installed on current Ubuntu images, so if `sdkmanager` itself is
   the broken part, the build can likely proceed on what's already
   there. If compileSdk 36 genuinely isn't available, the actual Gradle
   build step now surfaces that as its own specific, readable AGP error
   instead of this step silently swallowing the real one.

Every stage writes into `build-output.log` now, not just the final
build/test/lint steps, specifically so a failure anywhere in the
bootstrap chain shows up in the one downloadable artifact instead of
requiring a trip into the Actions UI to find it.

**What's actually verified vs. still a best guess:** the YAML itself
parses correctly and every embedded shell script passes `bash -n` —
real syntax validation, not just careful reading. What that *can't*
confirm: whether this specific runner environment behaves the way the
runner-images docs and the linked issue describe, or whether
`build-tools;36.0.0` is the exact right version string if `sdkmanager`
turns out to work fine after all. Both should be immediately visible in
the next run's log either way.

**Logging**: every Gradle invocation runs with `--stacktrace`, and
`assembleDebug`/`testDebugUnitTest`/`lintDebug` all append to the same
`build-output.log`. That log, the full `app/build/reports` directory
(lint + test HTML/XML reports), and — only on success — the built debug
APK are all uploaded as workflow artifacts, with `if: always()` on the
log/reports uploads specifically so they're still there when the build
fails, not just when it succeeds. The job summary tab also gets the last
60 log lines for a quick glance without downloading anything.

**Worth double-checking on the next run:**
- The exact `build-tools;36.0.0` version string — a reasonable guess,
  not confirmed against what's actually published.
- Whether `ANDROID_HOME`/`ANDROID_SDK_ROOT` point where the "Locate
  Android SDK" step assumes — its own log output will show this
  directly.
- The pinned action versions (`checkout@v6`, `setup-java@v5`,
  `upload-artifact@v4`) reflect current docs as of writing.

## Roadmap

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the full original feature
list, grouped into build phases, plus the platform-reality notes (what
the spec asked for vs. what's actually possible without root/Shizuku —
e.g. `GameManager` can't set another app's performance mode from a
regular app; that's OEM-system-app-only).
