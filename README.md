# Kilonova

A personal fork of Supernova Launcher, tuned for my Samsung One UI devices (for example, Samsung Finder
integration) and for other personal requirements. Its network use is limited to the optional store-category lookup for Drawer groups (F-Droid's public index and
Play Store pages, package names only); everything else works fully offline.

## Credit

Kudos to **tanujnotes**, who wrote Supernova Launcher. Nearly everything here is his work: the
customizable desktop, dock, drawer, folders, search, gestures, icon packs and much more. Kilonova only
adds a few changes on top. If you are looking for a launcher to use, get Supernova from its own pages
(see below).

## Original project

* **Supernova Launcher** source code: [github.com/tanujnotes/Supernova](https://github.com/tanujnotes/Supernova)
* **Author:** tanujnotes, [tanujnotes.github.io](https://tanujnotes.github.io), X
  [@tanujnotes](https://x.com/tanujnotes)
* **Install Supernova:** [F-Droid](https://f-droid.org/packages/com.supernova.launcher),
  [releases](https://github.com/tanujnotes/Supernova/releases)
* **Community:** X [@SupernovaLaunch](https://x.com/supernovalaunch), Reddit
  [r/SupernovaLauncher](https://www.reddit.com/r/SupernovaLauncher)

Please report Supernova issues there, not here.

## What Kilonova changes

* **Samsung Finder:** on One UI 9 and later, Settings > Search can hand the search bar and the Open
  search action to Samsung Finder, which also finds files and notes.
* **Dock drag and drop:** dropping an icon on the dock inserts it and shifts its neighbours, with a
  live preview and slot snapping.
* **Notifications in the icon menu:** rows wrap over up to three lines and are dismissed with a swipe.
* **Own name and icons:** installs as `com.kilonova.launcher`, next to Supernova.

## Distribution

Kilonova is for personal use. It is not published on F-Droid, Google Play or any other store, and has
no releases. To try it, build it from source.

## Build

```
./gradlew assembleDebug
```

Needs JDK 17 and the Android SDK with platform 37. The debug build installs as
`com.kilonova.launcher.debug`.

## License

AGPL-3.0, like Supernova. See [LICENSE](LICENSE). The original copyright notices are kept; Kilonova is
a modified version of Supernova.
