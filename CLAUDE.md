# Breathe In — Android MVP

## What this app does
Breathe In interrupts doom scrolling. While the user is scrolling short-form feeds (Instagram Reels, YouTube Shorts, TikTok) or any other app they pick, the app interrupts **mid-session** with a full-screen breathing animation drawn over that app. The user then either keeps scrolling or stops and does a short, wholesome check-in ("Something worth sharing with your family?").

This is a feasibility MVP. Priorities: prove the interruption works mid-scroll, prove we can read usage data. Keep it simple; no backend, no network calls.

## Current scope (scaffold)
Built: app shell, permissions, usage tracking, per-app session monitoring, Reels/Shorts detection (accessibility service), the Monitored apps page (which apps + limit per app), and the breathing overlay.
Not yet: check-in, event log database, notification mode, visual design. Leave clear TODOs and empty interfaces where these will plug in.

## Tech stack
- Kotlin, Jetpack Compose, Material 3 (default theme for now)
- Min SDK 26, target/compile latest stable SDK
- Gradle Kotlin DSL with a version catalog (`gradle/libs.versions.toml`)
- No DI framework: manual dependency injection via a simple `AppContainer` in the app module
- Settings in DataStore (Preferences)

## Module structure
Modular so two developers can work in parallel without touching each other's files. Feature modules never depend on each other; they talk through interfaces in `:core`.

```
:app                  Application class, AppContainer (wires everything), MainActivity, navigation
:core:model           Plain data classes: AppUsage, AppInfo, MonitoredApp, SessionState, ScrollFeature, ShortFormApp, InterruptionRequest, InterruptionOutcome
:core:common          Interfaces shared across features (see Contracts), ShortFormApps list + constants (package names)
:core:permissions     PermissionChecker: hasUsageAccess(), canDrawOverlays(), hasNotificationPermission(), hasAccessibilityService(), and Intents to open each settings page
:core:settings        SettingsRepository (DataStore): monitored apps + limit per app, monitoring on/off
:feature:usage        UsageRepository implementation using UsageStatsManager / UsageEvents
:feature:scrolldetect AccessibilityService that detects Reels / Shorts on screen by view ID (FeatureMarkers)
:feature:monitor      Foreground service + SessionTracker (one per monitored app; times continuous sessions, fires interruptions)
:feature:overlay      Overlay implementation: WindowManager + ComposeView + breathing animation
:feature:apps         Monitored apps page: pick apps and a limit for each
:feature:onboarding   Permission onboarding screen
:feature:debug        Debug screen: permission status, today's per-app usage, live sessions, detected feed, view-ID capture, "test overlay" button
```

## Contracts (in :core:common)
- `UsageRepository`
  - `fun currentForegroundPackage(): String?`
  - `fun todayUsage(): List<AppUsage>` (package, label, minutes)
  - `fun todayMinutesFor(packageName: String): Int`
  - `fun launchableApps(): List<AppInfo>`, `fun labelFor(packageName: String): String`
- `ScrollFeatureDetector`
  - `val isConnected: StateFlow<Boolean>`, `val activeFeature: StateFlow<ScrollFeature?>`
  - `fun requestViewIdCapture()` + `val lastViewIdCapture` (debug aid for keeping view IDs current)
- `MonitorController` — `isRunning`, `sessions: StateFlow<List<SessionState>>`, `start()`, `stop()`
- `InterruptionTrigger`
  - `fun show(request: InterruptionRequest, onOutcome: (InterruptionOutcome) -> Unit)`
  - `fun dismiss()`
- `InterruptionOutcome` = `KeepScrolling` | `ImDone`

`:feature:monitor` depends only on these interfaces. `:feature:overlay` implements `InterruptionTrigger`, `:feature:scrolldetect` implements `ScrollFeatureDetector`. `:app` wires them together in `AppContainer`.

## Behaviour
1. **Usage tracking** — query `UsageEvents` for recent `ACTIVITY_RESUMED` / `ACTIVITY_PAUSED` events to determine the current foreground app. Use `queryAndAggregateUsageStats` (or events) for today's per-app minutes since midnight.
2. **Monitor service** — foreground service with a persistent low-priority notification ("Breathe In is looking out for you"). On Android 14+, declare `foregroundServiceType="specialUse"` with the required `FOREGROUND_SERVICE_SPECIAL_USE` permission and property. Poll every second with a coroutine (so short visits to a feed add up accurately).
3. **Session tracking** — one session per monitored app. For Instagram and YouTube, only time with Reels / Shorts on screen counts (detected by `:feature:scrolldetect`); for TikTok and any other app, time with the app in the foreground counts. Only that time adds up: Reels 5 s → home feed 5 s → Reels again counts as 5 s, not 10. Being away for more than 30 seconds ends the session and resets the total. When the session reaches that app's limit, call `InterruptionTrigger.show()` once.
   - **Feed detection** — an AccessibilityService limited to Instagram + YouTube looks for known view IDs (`FeatureMarkers`). They're internal to those apps and change with updates: use the debug screen's "Capture view IDs" to find new ones. Window queries run on a background thread (the service shares our UI's main thread).
4. **Overlay** — full-screen `ComposeView` added via `WindowManager` with `TYPE_APPLICATION_OVERLAY`. The ComposeView must be given a `LifecycleOwner` and `SavedStateRegistryOwner` (overlays have no Activity), otherwise Compose crashes.
5. **Breathing animation** — a black circle expands (breathe in, 5s), stays at full size (hold, 2s) and contracts (breathe out, 5s), 3 cycles (36s), with text "Breathe in" / "Hold" / "Breathe out". Show "<App> today: X min". The background is see-through: the app behind is blurred (Android 12+ `FLAG_BLUR_BEHIND`, when the device allows cross-window blur) under a light dark tint, or a stronger tint without blur. Buttons appear after the animation finishes. Tapping anywhere outside the circle, at any time, counts as Keep scrolling.
6. **Outcomes**
   - Keep scrolling → dismiss overlay, reset the session timer (next interruption after another full threshold).
   - I'm done (button reads "Take a break") → dismiss overlay, send the user to the home screen (`Intent.ACTION_MAIN` + `CATEGORY_HOME`). TODO hook for check-in.
7. **Limits** — set per app on the Monitored apps page. Default 10 minutes (debug builds: 1 minute). Debug builds also offer 10 s and 30 s for quick testing. Instagram, YouTube and TikTok are monitored by default.

## Permissions
- `PACKAGE_USAGE_STATS` (Usage Access, user grants in Settings)
- `SYSTEM_ALERT_WINDOW` (Display over other apps, user grants in Settings)
- `POST_NOTIFICATIONS` (Android 13+, runtime)
- `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`
- Accessibility service ("Breathe In feed detection", user enables in Settings > Accessibility). TODO before Play release: prominent disclosure + consent screen, and the Play accessibility declaration.
- Package visibility: `<queries>` for Instagram, YouTube, TikTok, plus launcher/home intents (to list and name apps)
If any permission is missing, return the user to onboarding; never crash.

## Conventions
- Base package: `com.breathein`, each module under its own sub-package (e.g. `com.breathein.feature.overlay`)
- Small files, one responsibility each
- Never commit `local.properties`, keystores, or build outputs

## Team workflow
- Two developers. Never commit directly to `main`.
- One branch per feature (`feature/overlay`, `feature/usage`, ...), merged via pull request.
- Keep PRs small. Pull `main` before starting new work.
- Ownership after the scaffold: Person A owns `:feature:usage`, `:feature:monitor`, `:core:*`; Person B owns `:feature:overlay`, `:feature:onboarding`, and later check-in.

## Testing
- Emulator with a **Google Play** system image so Instagram can be installed.
- Use the debug screen's "Test overlay" button to check the overlay without waiting.
- Set a 10 s limit on the Monitored apps page to test the full mid-scroll interruption quickly.
- Don't use `uiautomator dump` while testing detection: it pauses other accessibility services. Force-stopping Breathe In also turns its accessibility service off.

## Scaffold build task (do this first)
When asked to "build the scaffold", build everything in Current scope, in this order, and build the project after each step (`./gradlew assembleDebug` on macOS/Linux, `gradlew.bat assembleDebug` on Windows). Fix every error before moving on.

1. **Project setup** — if an Android project already exists in the repo, restructure it into the modules above rather than starting over. Create `settings.gradle.kts` including all modules, `gradle/libs.versions.toml`, and one `build.gradle.kts` per module. Use current stable versions of AGP, Kotlin (with the Compose compiler plugin), Compose BOM, Lifecycle, SavedState and DataStore. Make sure `.gitignore` covers `local.properties`, `.gradle/`, `build/`, `*.jks`, `*.keystore` and `.idea/` workspace files.
2. **:core:model** — data classes and the sealed `InterruptionOutcome`.
3. **:core:common** — the contracts and constants.
4. **:core:permissions** — `PermissionChecker` and settings Intents. Check Usage Access via `AppOpsManager` (`OPSTR_GET_USAGE_STATS`), overlays via `Settings.canDrawOverlays`.
5. **:core:settings** — `SettingsRepository` exposing `Flow`s for threshold and monitoring on/off. Debug default threshold 1 minute via `BuildConfig.DEBUG`.
6. **:feature:usage** — `UsageRepository` implementation.
7. **:feature:overlay** — overlay `InterruptionTrigger` with the breathing animation. Overlay must be `MATCH_PARENT`, focusable so the buttons work, and safely removable (guard against double add/remove).
8. **:feature:monitor** — foreground service + `SessionTracker`. Keep `SessionTracker` pure Kotlin (time passed in) so it is unit-testable; add unit tests for: session start, session ending after a 30s gap, firing once at threshold, reset after Keep scrolling.
9. **:feature:onboarding** — one screen listing the three permissions, each with status and a "Grant" button; "Start monitoring" enabled only when all are granted. Re-check permissions in `onResume`.
10. **:feature:debug** — permission status, today's top apps with minutes, current foreground app, live session seconds, threshold slider (1–30 min), start/stop monitoring, "Test overlay" button.
11. **:app** — `AppContainer`, `MainActivity` with simple navigation (onboarding if anything is missing, otherwise debug screen), manifest permissions, service declaration, and `<queries>`.

### Definition of done
- `assembleDebug` succeeds and unit tests pass.
- Fresh install → onboarding → all permissions granted → debug screen shows real usage minutes.
- "Test overlay" shows the breathing animation over any app; both buttons work.
- With monitoring on and a 1-minute threshold, scrolling Instagram for 1 minute triggers the overlay mid-scroll. "Keep scrolling" returns to Instagram and the next interruption comes 1 minute later. "I'm done" goes to the home screen.
- Finish with a short summary of what was built, anything left as TODO, and manual test steps.
