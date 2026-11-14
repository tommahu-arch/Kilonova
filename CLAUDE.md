# Kilonova

Personal fork of the Android launcher Supernova (https://github.com/tanujnotes/Supernova, AGPL-3.0).
Fork: https://github.com/tommahu-arch/Kilonova (`origin`); upstream is `tanujnotes/Supernova` (`upstream`).
Work happens on `dev` (upstream's active branch), one branch per requirement, merged after testing.

## Licence rules
- Stays AGPL-3.0. Keep upstream copyright/licence notices; mark modified files as modified.
- Rename is cosmetic: `applicationId` is `com.kilonova.launcher` (debug adds `.debug`), but the Kotlin
  namespace/package stays `com.supernova.launcher` and class names (`SupernovaApp`, `SupernovaPopupMenu`, ...)
  are unchanged on purpose, to keep upstream merges clean. Do not mass-rename.

## Target and constraints
- Test device: Samsung Galaxy Tab S12 Ultra, NOT rooted, Knox must not be tripped. No-root solutions only
  (no root-only APIs, no bootloader or system-partition changes, no signature-permission tricks).
- Debug and release builds show the same labels ("Kilonova", "Kilonova Settings"); they coexist via the
  `.debug` package suffix. Installing the renamed package is a new app: re-grant notification access.

## Stack
Kotlin, classic Android Views (no Compose), ~62k LOC. minSdk 28, targetSdk 36, compileSdk 37.
Gradle 9.7.1, AGP 9.4.1, Kotlin 2.4.20, JVM target 17.

## Build and test (Arch Linux)
- JDK 17, Android SDK at `~/android/sdk` (platform 37.0). `local.properties` points at it.
- Build: `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`
- Unit tests: `./gradlew testDebugUnitTest` (single: `--tests '*DockInsertTest*'`)
- Install: `adb install -r --user 0 app/build/outputs/apk/debug/app-debug.apk`. Without `--user 0` adb installs
  for every user, including One UI's Dual Messenger profile (user 95), which adds two extra profile icons
  (Kilonova and Kilonova Settings). Remove a stray copy with `adb shell pm uninstall --user 95 <package>`.
- Always build and run the unit tests before saying a change is done. Never claim a UI change works on the
  tablet; say what to check by hand.

## Git workflow
- `git switch dev && git switch -c <branch>` per requirement; small commits.
- Stage with `git add -u` (not `-A`): untracked patch/scratch files must not be committed.
- Pull upstream later with `git fetch upstream && git merge upstream/dev`.
- `gh` quirks: `gh repo rename` needs `--repo`; `gh repo fork --remote` only from inside a clone.

## Requirements and status
Decisions were made under Thomas Mahu's authority; do not change scope without asking.

- **R1 Pixel-based capacity (todo).** Items per axis = `floor(shortSide / (iconSize * zoom + minGap))`;
  `minGap` is a user setting in dp (default 8 dp is a guess). Replaces the hard limits in
  `data/GridLimits.kt` for desktop, dock, drawer, folders and the search page; the Labs "Big grid" toggle
  becomes unnecessary. Clamp stored values at layout time, never at storage time. Shortest screen side is
  the reference so counts are rotation-stable. No 90-degree icon rotation (idea dropped). The search page
  grid has not been inspected yet.
- **R2 Dock cap and overflow (todo).** Cap is optional (none, or fixed N); effective limit = min(pixel
  capacity, N). If capacity falls below the icon count, add a dock page and move the extra icons there;
  nothing is lost.
- **R3 Dock drag and drop (done, verified on tablet, merged into `dev`).** Insert and shift neighbours with a
  live preview, visible slot targets, nearest-slot snapping with hysteresis, large drop zone, clear
  feedback when the page is full. Code: `ui/LiveDragController.kt` (target finding, preview),
  `data/DockInsert.kt` (pure shift plan), `LauncherRepository.withDockGap/movedToDock`,
  `LauncherActivity.handleDropToDock/renderDock`. When a page is full the old swap still applies; R2
  replaces that with overflow to a new page.
- **R4 Notification rows in the icon menu (done, verified on tablet).** Wider column, up to 3 lines,
  swipe left or right to dismiss via `cancelNotification`. Code: `notification/SupernovaNotificationListener.kt`
  (`dismiss(key)`), `ui/SupernovaPopupMenu.kt` (`addNotification`, swipe, refit).
  Persistent/non-clearable notifications are already excluded from badges by `BadgeCounting`, so no hide filter.
- **Drawer icons (done, verified on device, merged into `dev`).** App and Settings use Thomas's framed PNGs
  (`art/`, scaled into `mipmap-*dpi/ic_launcher_kilonova*.png`) as legacy, non-adaptive icons (Thomas's choice),
  so the frame shows exactly as drawn with the System icon shape; a chosen shape clips them, and themed mode
  uses a glyph generated from the picture. Upstream's adaptive `ic_launcher*` XMLs are left untouched.

## Gotchas learned
- One UI hands launchers (`LauncherActivityInfo.getIcon`) Kilonova's legacy icons already cut to its
  squircle, so `AppCatalog.activityIcon` loads Kilonova's own entries from its own resources instead.
- `SupernovaPopupMenu` is a `PopupWindow` sized once at `show()`; changing rows afterwards needs `refit()`.
- A drag moves only views; the model is written at the drop and `renderDock` rebuilds the row, so any
  view-side preview state must be reset there (see `translationX = 0f`).
- Dock ranks are `page * dockCount + slot`; code that shifts ranks must stay within one page.
