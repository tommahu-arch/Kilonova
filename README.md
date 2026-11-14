# Kilonova Launcher

A personal fork of [Supernova Launcher](https://github.com/tanujnotes/Supernova) by tanujnotes: an
infinitely customizable, offline Android homescreen.

## What differs from Supernova
* Dock, grid and folder sizes follow the screen instead of fixed limits (planned).
* Dock drag-and-drop inserts and shifts neighbors (planned).
* Notifications in the icon menu wrap over several lines and are dismissed with a swipe.

Requirements and status live in the fork's requirements document.

## Build
```
./gradlew assembleDebug
```
Needs JDK 17 and the Android SDK with platform 37. The debug build installs as
`com.kilonova.launcher.debug`.

## License
AGPL-3.0, as Supernova. See [LICENSE](LICENSE). The original copyright notices are kept; this fork
modifies the original work.
