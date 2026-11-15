# Kilonova

Personal fork of the Android launcher Supernova (https://github.com/tanujnotes/Supernova, AGPL-3.0).
Fork: https://github.com/tommahu-arch/Kilonova (`origin`); upstream is `tanujnotes/Supernova` (`upstream`).
Kilonova keeps one long-lived branch, `main` (also the GitHub default); upstream's active branch is `upstream/dev`.
Work is one short-lived branch per requirement, merged into `main` after testing, then deleted.

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
- `git switch main && git switch -c <branch>` per requirement; small commits.
- Stage with `git add -u` (not `-A`): untracked patch/scratch files must not be committed.
- Pull upstream later with `git fetch upstream && git merge upstream/dev`.
- `gh` quirks: `gh repo rename` needs `--repo`; `gh repo fork --remote` only from inside a clone.

## Release process
Releases: https://github.com/tommahu-arch/Kilonova/releases (Kilonova-<version>.apk). Current: v0.0.3.
Upstream's own tags (v0.1.7 ... v0.2.1) are in the repo; Kilonova's start at v0.0.1 and must not reuse them.
- Version lives in `app/build.gradle.kts`: `versionName` (0.0.N) and `versionCode`. versionCode must only go up
  (0.0.1 = 1, 0.0.2 = 22, 0.0.3 = 23). Upstream's What's new files `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
  already cover 2..21, so Kilonova's codes start at 22 and skip those; add `<versionCode>.txt` for every release
  (it ships in the APK as `assets/<versionCode>.txt`; a missing file leaves About > What's new empty).
- Signing: release key `~/.android/kilonova-release.jks`, passwords in the gitignored `keystore.properties` at the
  repo root. Never commit either. Every release must use this one key or installs can't update; back both up.
  Without the properties file `assembleRelease` produces an unsigned APK.
- Flow: branch from `main`, bump version + add the changelog, then `./gradlew testDebugUnitTest assembleRelease`
  (R8 shrinking is on, so the release build can differ from debug). Check with `apksigner verify --print-certs`
  (SHA-256 of the cert must be `132f71f6...0ecb6`) and `aapt2 dump badging` (package, versionCode/Name).
  Copy `app/build/outputs/apk/release/app-release.apk` to `~/Kilonova-<version>.apk`, install it with
  `adb install -r --user 0 ...`, and let Thomas test by hand before publishing.
- Publish only on Thomas's go-ahead: merge into `main` (`--no-ff`), annotated tag `v<version>` on the merge,
  `git push origin main v<version>`, then `gh release create v<version> ~/Kilonova-<version>.apk --repo
  tommahu-arch/Kilonova --title "Kilonova <version>" --notes ...`. Delete the merged branch.
- The debug build (`com.kilonova.launcher.debug`) and the release (`com.kilonova.launcher`) coexist on the phone.

## Requirements and status
Decisions were made under Thomas Mahu's authority; do not change scope without asking.

- **R1 Pixel-based capacity (on hold, 2026-11-14).** Items per axis = `floor(shortSide / (iconSize * zoom + minGap))`;
  `minGap` is a user setting in dp (default 8 dp is a guess). Replaces the hard limits in
  `data/GridLimits.kt` for desktop, dock, drawer, folders and the search page; the Labs "Big grid" toggle
  becomes unnecessary. Clamp stored values at layout time, never at storage time. Shortest screen side is
  the reference so counts are rotation-stable. No 90-degree icon rotation (idea dropped). The search page
  grid has not been inspected yet.
- **R2 Dock cap and overflow (on hold, 2026-11-14).** Cap is optional (none, or fixed N); effective limit = min(pixel
  capacity, N). If capacity falls below the icon count, add a dock page and move the extra icons there;
  nothing is lost.
- **R3 Dock drag and drop (done, verified on tablet, merged into `main`).** Insert and shift neighbours with a
  live preview, visible slot targets, nearest-slot snapping with hysteresis, large drop zone, clear
  feedback when the page is full. Code: `ui/LiveDragController.kt` (target finding, preview),
  `data/DockInsert.kt` (pure shift plan), `LauncherRepository.withDockGap/movedToDock`,
  `LauncherActivity.handleDropToDock/renderDock`. When a page is full the old swap still applies; R2
  replaces that with overflow to a new page.
- **R4 Notification rows in the icon menu (done, verified on tablet).** Wider column, up to 3 lines,
  swipe left or right to dismiss via `cancelNotification`. Code: `notification/SupernovaNotificationListener.kt`
  (`dismiss(key)`), `ui/SupernovaPopupMenu.kt` (`addNotification`, swipe, refit).
  Persistent/non-clearable notifications are already excluded from badges by `BadgeCounting`, so no hide filter.
- **Drawer icons (done, verified on device, merged into `main`).** App and Settings use Thomas's framed PNGs
  (`art/`, scaled into `mipmap-*dpi/ic_launcher_kilonova*.png`) as legacy, non-adaptive icons (Thomas's choice),
  so the frame shows exactly as drawn with the System icon shape; a chosen shape clips them, and themed mode
  uses a glyph generated from the picture. Upstream's adaptive `ic_launcher*` XMLs are left untouched.
- **R5 Samsung Finder as search engine (done, verified on device, merged into `main`).** Settings > Search
  gets a radio "Search engine": Kilonova (default) or Samsung Finder, the latter offered only when the phone has
  it (One UI 9+: `com.sec.android.app.launcher/.search.SearchActivity`, action SEARCH, extra `query`). With
  Finder picked the search preview and the Providers group hide, and only the desktop search bar tap and the
  Open search gesture go to Finder; Search-for-a-phrase gestures, the feed pull-down, the drawer and its search
  stay internal. Falls back to Kilonova's search if Finder can't start. Code: `search/SamsungFinder.kt`,
  `SearchEngine` in `data/SettingEnums.kt`, `LauncherActivity.openSamsungFinder`,
  `SettingsCatalogue.hiddenUnderFinder`.
- **Folder tap action (done, released in v0.0.2; search added in v0.0.3, installed on the phone, not yet hand-verified).**
  With "Swipe to open folder" on, the folder's Edit sheet shows a "Tap action" row above Swipe up: the app,
  shortcut or launcher action a tap launches; default is the first app in the folder, "Nothing" resets it.
  Reuses the swipe action picker. Code: `LauncherItem.tapAction/tapLabel`, `LauncherRepository.setFolderTapAction`,
  `ui/FolderEditSheet.kt` (`onEditTap`), `LauncherActivity.launchFolderTap` and `showSwipeActionPicker(tap = true)`.
  The picked pick is kept when the switch goes off, like the swipe-up action.
- **Releases.** v0.0.1 (versionCode 1): first release, R3, R4, R5, drawer icons. v0.0.2 (versionCode 22): folder tap action
  and What's new text. v0.0.3 (versionCode 23): search box in the action picker (`ui/ActionPickerSheet.kt`: filters the
  showing tab, text kept across tabs; also in swipe and gesture pickers) and debug builds signed with the release key
  (debug and release can swap installs; settings still don't sync, use Export/Import backup). All published on GitHub,
  signed with the same key.

## Gotchas learned
- One UI hands launchers (`LauncherActivityInfo.getIcon`) Kilonova's legacy icons already cut to its
  squircle, so `AppCatalog.activityIcon` loads Kilonova's own entries from its own resources instead.
- `SupernovaPopupMenu` is a `PopupWindow` sized once at `show()`; changing rows afterwards needs `refit()`.
- A drag moves only views; the model is written at the drop and `renderDock` rebuilds the row, so any
  view-side preview state must be reset there (see `translationX = 0f`).
- Dock ranks are `page * dockCount + slot`; code that shifts ranks must stay within one page.
