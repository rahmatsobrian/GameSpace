# Roadmap

The original brief for this app was exhaustive — a full Game
Space/Booster app (game library, floating overlay with 60+ tools,
booster, monitoring, backup/restore, full test suite) to production
quality. That's real scope: months of work, not one sitting. This
document is the backlog it turned into, plus the platform facts that
change how a few specific items get built.

Phases are ordered by dependency, not by feature importance — later
phases build on data/abstractions earlier ones establish.

## Phase 0 — Foundation (done)

- Gradle project, Hilt, Compose/Material3, navigation shell.
- Privilege layer: root (libsu, provider-agnostic across Magisk/KernelSU/
  APatch) + Shizuku + public-API-only, unified behind `PrivilegeRepository`.
- System Access screen.

## Phase 1 — Home & game library (done)

- Room: `GameEntity` (soft-delete via `isDismissed` — see README's design
  decisions for why a hard delete would break rescanning), `GameDao`,
  `AppDatabase`.
- Installed-app scan via `PackageManager` (`<queries>` manifest entry for
  Android 11+ package visibility), `CATEGORY_GAME` heuristic for
  auto-detection, manual add/remove for everything the heuristic misses.
  Favorite, search, grid/list view — all real and working.
- **Revised from the original plan below:** "recently played" and
  "statistik bermain" turned out not to need Phase 3's floating-overlay
  service after all. `UsageStatsManager` (gated behind its own special
  "Usage access" grant, independent of root/Shizuku) gives real
  last-played timestamps and an approximate playtime figure for any app,
  retrospectively, without anything needing to run continuously. Moved
  here from wherever it was implicitly assumed to belong once that became
  clear during implementation — `core/usage/UsageAccessSource.kt`.
- DataStore for simple app-wide settings — **not done**, pushed to
  whichever phase first needs a persisted setting (nothing has needed one
  yet; Home's grid/list toggle is session-only for now).

## Phase 2 — Monitoring engine (done)

Standalone data layer (`data/monitoring/`), with a first real UI consumer
(`feature/devicestatus/`) — not yet wired into a floating overlay, which
is Phase 3's job.

- **RAM**: `ActivityManager.getMemoryInfo()` — device-wide, no root
  needed, exactly as planned.
- **Battery**: sticky `ACTION_BATTERY_CHANGED` intent via `BatteryManager`
  — no root needed, exactly as planned.
- **Thermal**: `PowerManager.getCurrentThermalStatus()` (API 29, coarse
  enum) plus `getThermalHeadroom()` (API 30+, continuous 0.0-1.0+ forecast
  figure) where available — richer than originally planned, no root
  needed either way.
- **CPU — revised from the original plan.** The original draft above
  assumed `/proc/stat` was "readable without root." That's wrong: regular
  apps have not been able to read `/proc/stat` since Android 8 (confirmed
  against current reports while building this phase, not assumed —
  multiple independent sources describe the same `EACCES` on a plain
  `File("/proc/stat").readText()` from an unprivileged app). Since this
  app's minSdk is 29, that restriction applies on every device it runs
  on. So: **CPU load has no real number without root or Shizuku.**
  `CpuSource` returns a locked/unavailable state on the public-only tier
  and a real percentage (computed from two `/proc/stat` samples read
  through `PrivilegeRepository.execPrivileged`) once either is granted —
  the privilege layer's first actual caller, which is exactly why it got
  built in Phase 0 before any feature needed it yet.
- **Not wired up: Android 16's CPU/GPU headroom API.** Real and citable —
  `ASystemHealth_getCpuHeadroom`/`getGpuHeadroom` exist in the NDK
  reference for API 36 — but only the C/NDK signature turned up during
  research, not a confirmed Java-facing class/method name, and guessing
  at one to write working code against isn't a trade worth making. Worth
  wiring up as an Android-16-only enhancement once you can confirm the
  Java API shape directly against `developer.android.com` in Android
  Studio (`SystemHealthManager` is the most likely home for it, but
  that's an educated guess, not a confirmed fact — verify before relying
  on it in code).
- **Not built yet: network (ping/jitter/packet loss/link info).** No
  passive OS counter gives these — they require actually sending probes
  (shelling out to the system `ping` binary works without root; the
  binary itself carries the raw-socket capability the calling app
  doesn't need). Parsing `ping`'s text output reliably across different
  device toolchains (toybox vs. busybox vs. iputils) is genuinely fiddly
  enough that it deserves its own focused pass rather than being folded
  into this one.

## Phase 3 — Floating Game Assistant (overlay shell)

**First slice done:** permission flow, the foreground service, and a
minimal draggable bubble are real and working.

- `core/overlay/OverlayPermissionSource.kt`: `Settings.canDrawOverlays()` /
  `ACTION_MANAGE_OVERLAY_PERMISSION`, same shape as usage access.
- `feature/overlay/OverlayService.kt`: `TYPE_APPLICATION_OVERLAY` window
  via `WindowManager`, typed `specialUse` per Android 14+'s mandatory
  FGS-type declaration (justification string is in the manifest
  `<property>` tag — that's what Play Console would review if this ever
  ships there), started/stopped explicitly from Device Status's toggle,
  never automatically.
- `feature/overlay/OverlayLifecycleOwner.kt` +
  `feature/overlay/OverlayBubbleContent.kt`: a `ComposeView` hosted
  outside any Activity needs its own manually-supplied
  Lifecycle/ViewModelStore/SavedStateRegistry owners to render at all —
  see that file's doc comment, it's flagged as the single least-certain
  piece of code in the project so far, more so than the libsu/Shizuku
  method-name uncertainty from Phase 0.
- The bubble itself: collapsed circle, drag to move (via `WindowManager.
  updateViewLayout` + a Compose drag gesture, not Compose's own offset —
  Compose doesn't control this window's screen position, only its
  content), expands to show the same four Phase 2 metrics compactly.

**Deliberately not in this slice** — real scope left for a follow-up,
not a promise already made and broken:
- Resize (only drag-to-move exists; the bubble/panel sizes are fixed)
- Opacity control
- Notch/foldable/tablet-specific layout adaptation, landscape-specific
  positioning
- Everything from the 60+-tool floating menu list beyond the four
  monitoring readouts already wired up — screen recorder, quick
  toggles, floating notes/calculator/browser, macros, blockers, etc.
  (Phase 5's list)
- A "only show while a game is actually running" auto-trigger — right
  now the user starts/stops it manually from Device Status, it doesn't
  yet watch for foreground-app changes to launch itself. That needs
  either `UsageStatsManager` polling (same access this app's usage-stats
  feature already has) or `AccessibilityService` — worth its own pass.

## Phase 4 — Booster (first slice done)

Four real, individually-permissioned controls — no bundled "Ultra Mode"
button pretending to be one thing when it's actually four unrelated
system settings with four different access requirements.

- **Brightness** (`Settings.System.SCREEN_BRIGHTNESS`): `WRITE_SETTINGS`
  tier — the classic, well-established special permission, granted via
  `ACTION_MANAGE_WRITE_SETTINGS`. No root needed.
- **Do Not Disturb** (`NotificationManager.setInterruptionFilter`):
  `ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS` tier — its own special
  permission, independent of everything else. No root needed.
- **Animation scale** and **refresh rate — revised from "should be
  straightforward" to "needs the privilege layer."** Both live in the
  `Settings.Global`/`Settings.System` tables via keys like
  `window_animation_scale` and `peak_refresh_rate`. The refresh-rate keys
  in particular are technically in the "System" table, which sounds like
  it should mean the same `WRITE_SETTINGS` permission as brightness — but
  every current, real-world source describing how to change them (XDA
  guides, ADB walkthroughs, community tools like SetEdit/System UI Tuner)
  consistently pairs them with `WRITE_SECURE_SETTINGS` instead, which a
  regular app cannot be granted through any Settings screen — only via
  `adb shell pm grant` or root. That's empirical, repeated, current
  evidence, not a one-off — trusted over the cleaner-sounding theoretical
  categorization. So both go through
  `PrivilegeRepository.execPrivileged("settings put ...")`, gated on root
  or Shizuku, exactly like `CpuSource` in Phase 2.
- **Not built yet in this slice**: RAM/cache "cleaning" and background-app
  management. Doing this honestly (see the RAM/Cache Cleaner note further
  below) needs a way to actually list running processes first, which
  itself needs root/Shizuku (`ps -A` via `execPrivileged`, parsed) — real
  additional work, not a quick add-on, so it's its own follow-up rather
  than a rushed version bolted onto this slice.
- **Not built yet**: named presets ("Battery Saver" / "Balanced" /
  "Ultra") bundling these four controls together. Worth adding as a thin
  convenience layer once the individual controls have actually been used
  for a while — bundling before that risks guessing at combinations
  nobody wants, rather than the individual pieces being ready and this
  just being a shortcut wrapper around them.

## Phase 5 — the long tail

Everything below was one big undifferentiated list originally. Splitting
it as pieces get built, in whatever order actually gets prioritized —
see each subsection for status.

### Blocker & privacy (first slice done)

- **Notification blocking**: `NotificationListenerService`, gated on
  "Notification access" (`ACTION_NOTIFICATION_LISTENER_SETTINGS`).
  System-bound automatically once granted — this app never starts/stops
  it. Blocks everything from every other app while active; no per-app
  blocklist yet (see Settings, below).
- **Call blocking**: `CallScreeningService` + `RoleManager.
  ROLE_CALL_SCREENING` (API 29, matches minSdk). Also system-invoked
  automatically once the role is held.
- **What happened to "SMS Blocker" as its own item**: it isn't one.
  Genuinely intercepting/dropping SMS before it reaches the user needs
  default-SMS-app status (`RoleManager.ROLE_SMS`), which means shipping a
  full SMS compose/view UI — wildly disproportionate for a game booster,
  and a different app in practice. What the notification blocker already
  does — hide the SMS/messaging app's notification while active — covers
  the actual use case ("don't get interrupted by texts while gaming")
  without that commitment. Revised from the original brief once this
  became clear during implementation, not assumed going in.
- **Not built yet**: per-app blocklist/allowlist UI (right now it's an
  all-or-nothing toggle), auto-activating blocking when a game session
  starts rather than a manual switch — same missing piece Phase 3's
  overlay auto-trigger needs, worth solving once for both.
- **Deliberately not attempted**: App Lock. The only real way to do this
  (watch foreground-app changes and overlay a lock prompt) needs
  `AccessibilityService`, which Google scrutinizes specifically for this
  use pattern — worth its own careful, focused pass rather than a
  bolt-on here.

### Settings + DataStore (first slice done)

- **DataStore** (`data/settings/`): one `Preferences` DataStore, one
  `AppSettings` model, wired into `MainActivity` — dark/light/system
  mode, AMOLED, and Dynamic Color now actually drive `GameSpaceTheme`
  instead of that composable's parameters sitting at their hardcoded
  defaults forever. Home's grid/list toggle now persists too, instead of
  resetting every time the app process restarts.
- **Onboarding skip.** System Access now marks itself complete in
  `SettingsRepository` when the user continues past it, and
  `MainActivity` picks Home as the real start destination on return
  visits instead of showing that screen every launch. Handled the
  DataStore-is-async-on-cold-start edge case explicitly — `MainViewModel.
  settings` starts `null` until the first real read lands, and
  `MainActivity` renders nothing for that one frame rather than
  momentarily deciding based on default values (which would have flashed
  onboarding at returning users before correcting itself).
- **Backup & Restore — game library only, not a full app backup.**
  JSON export/import via Storage Access Framework
  (`ActivityResultContracts.CreateDocument`/`OpenDocument` — a different,
  new-to-this-project pattern from the fire-and-forget-intent one every
  other permission source uses, needed because file picking genuinely
  requires the actual result `Uri`, not just a re-checkable state).
  Plain `org.json` for serialization rather than adding kotlinx.
  serialization for one simple data class. Scoped to the game library
  specifically (favorites, manual additions, dismissals) since that's
  the one piece of state that's genuinely irreplaceable user data —
  Booster/Blocker/theme preferences are device-local conveniences, not
  worth backing up.
- **Not built yet**: per-app notification blocklist (currently
  all-or-nothing), accent color picker beyond Dynamic Color on/off,
  language setting (the spec's own ask — several places in this codebase
  hardcode Indonesian strings specifically *because* there's no language
  switching to make resource extraction worthwhile yet; this is what
  would finally justify doing that extraction).

### More floating tools (first slice done)

Added as new pages inside the existing overlay bubble from Phase 3
(`feature/overlay/OverlayBubbleContent.kt` now has Monitor/Tools/
Timer-Stopwatch pages instead of just Monitor).

- **Quick toggles — WiFi, Bluetooth, rotation lock.** WiFi and Bluetooth
  go through the privilege layer (`svc wifi enable/disable`, `svc
  bluetooth enable/disable` via `execPrivileged`) when root or Shizuku is
  active — Android has blocked the direct app APIs for both (WiFi since
  API 29, Bluetooth's adapter state since API 33 without a `BLUETOOTH_
  CONNECT` runtime permission this app doesn't otherwise need). Without
  elevated access, both fall back to opening the relevant system screen
  (a compact `Settings.Panel` for WiFi, the full settings screen for
  Bluetooth — no confirmed `Settings.Panel` constant exists for Bluetooth
  the way it does for WiFi). Rotation lock is the one that's actually
  simple: it lives in `Settings.System`, so it reuses Booster's existing
  `WRITE_SETTINGS` permission with no root needed at all.
- **Timer & Stopwatch.** Straightforward `LaunchedEffect`-based ticking,
  no special permissions. State resets if you navigate away from the
  Timer/Stopwatch page and back — not hoisted further up than that page
  itself. Worth fixing if that turns out to matter; not worth guessing at
  up front.
- **Not built yet: Calculator.** Explicitly deferred rather than rushed —
  fitting a real button grid into a ~200dp floating panel without it
  feeling cramped is more UI work than the other tools in this slice, and
  the existing four tools already needed a real page/tab system built
  first. Same panel structure (`ToolsPage` → drill into a new page, same
  pattern as Timer/Stopwatch) is what a Calculator page would slot into.
- **Not built yet: screen recorder, screenshot (+ editor).** These need
  `MediaProjectionManager` consent, which is a fundamentally different
  pattern from every permission this app has built so far — it's
  requested via an Activity's `startActivityForResult`-style flow and
  hands back an actual `Intent`/result code needed to construct the
  projection, not a simple grant state re-checked on resume. Genuinely
  deserves its own focused pass rather than retrofitting `MainActivity`
  into that flow as an afterthought here.
- **Not built yet**: floating notes, floating browser (WebView-in-overlay
  is real extra weight/complexity), floating clipboard (Android 10+
  restricts clipboard *reads* to the focused window in most cases — a
  background overlay generally can't read it, worth confirming exactly
  what's possible before promising this one), macros (technically
  reachable now via `execPrivileged("input tap x y")` given the privilege
  layer already exists — worth building once there's a recording/editing
  UI for defining a macro, not just the raw capability), mini/floating
  window, widgets.

### Testing — not started

Unit tests for the repositories (`GameRepositoryImpl`,
`MonitoringRepositoryImpl`, `BoosterRepositoryImpl` are the most
logic-bearing and least device-dependent, so probably first), UI tests
for the Compose screens, instrumented tests for the parts that touch real
Android services (Room, the privilege layer, the blocker services).

---

## Platform-reality notes

Specific corrections from the original brief, so the reasoning is on
record next to the feature it affects instead of only in chat history.

**Performance Mode (Battery Saver/Balanced/Ultra) for the game itself.**
Android's `GameManager` API lets an app query and set its *own* game
mode, but setting another package's game mode is restricted to system
apps — that's how the OEM's own Game Dashboard/Game Space does it, and
it's not available to a regular third-party app. Non-root, this app's
"performance mode" realistically means device-wide levers it's actually
allowed to pull: refresh rate and brightness (`Settings.System`, via the
`WRITE_SETTINGS` special permission), DND, rotation lock — not reaching
into the game process itself. Root/Shizuku tier can go further (e.g.
writing scheduler/thermal-adjacent sysfs nodes some kernels expose), but
that's inherently device/kernel-dependent, not something to promise
generically.

**RAM/Cache "Cleaner."** Sandboxing means a regular app cannot free
another app's memory — this is *why* most commercial booster apps'
"Boost" buttons are close to placebo on modern Android. What's real
without root: trimming this app's own memory/cache, and surfacing
device-wide RAM info via `ActivityManager` so the user can see the
number, not fake a fix for it. With root/Shizuku, targeted process
management becomes genuinely possible — another reason Phase 2's
monitoring engine is built to consume the privilege layer instead of
assuming public APIs forever.

**Notification/Call/SMS blockers.** Built — see the Blocker & privacy
subsection under Phase 5 above for what's real, what "SMS Blocker"
actually turned into, and what's still deliberately missing. The
distribution note below is why the permissions involved were safe to
build against at all: Google Play restricts SMS/Call Log access to apps
whose *core* function is SMS/calling (i.e., default-handler apps), so a
game booster requesting them would likely fail Play review. Sideload/
GitHub distribution (this project's assumed default, consistent with
other Siroha releases) doesn't have that restriction — flag it if that
assumption is wrong, since notification access and call screening are
less restricted than raw SMS/Call Log would have been, but still worth
confirming against current Play policy before ever considering Play
distribution.

**GPU usage.** No public Android API exposes per-app or system-wide GPU
utilization to a regular app on Android 10-15. What's real: this app's
*own* frame timing via `Choreographer`/`FrameMetrics`. Reading the actual
game's GPU load the way OEM overlays do requires being a privileged
system component — out of reach even with root in most cases, since it's
often a vendor-specific interface, not a generic kernel one. Android 16
added a GPU headroom API alongside the CPU one mentioned in Phase 2 above
— real, but not wired up here yet for the same reason: only the NDK
signature was confirmed during research, not the Java API shape. GPU
readouts in Phase 3 should be labeled as an estimate or omitted rather
than presented as a hard number, unless a specific device/kernel exposes
something usable via the root tier, or the Android 16 headroom API gets
properly wired up as a version-gated enhancement.
