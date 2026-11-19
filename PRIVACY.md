# Privacy policy

Effective 19 November 2026. Applies to the Kilonova launcher app, a fork of
Supernova (`com.kilonova.launcher`), as installed from GitHub.

## In short

Kilonova has no analytics, ads, accounts or tracking, and sends nothing to
its developer. Its one network use is the optional store-category lookup for
Drawer groups (below). Everything else it reads stays on your phone.

## What Kilonova reads locally

Each of these is used only on the device, only for the feature named, and
most are off until you turn the feature on.

| Access | Used for | When it is asked for |
| --- | --- | --- |
| Installed apps (`QUERY_ALL_PACKAGES`) | Showing your apps in the drawer, on the home screen and in search, and the Activities picker | Granted at install; a launcher needs it to work |
| Contacts (`READ_CONTACTS`) | Contacts in search results and the feed's contacts card | Only when you turn on contact search or that card |
| Calendar (`READ_CALENDAR`) | The feed's upcoming events card | Only when you add that card |
| Notification access | Notification badges, notifications in an app's long-press menu, and the feed's media card | Only when you turn on one of these |
| Accessibility service | Locking the screen, opening Recent apps and opening Quick settings from a gesture. It reads no screen content and nothing you type | Only after you agree in Kilonova's dialog and switch it on in Android's settings |
| Device admin (force lock only) | Locking the screen from a gesture, as an alternative to the accessibility service. It can't wipe data or change passwords | Only if you choose this lock method |
| Phone (`CALL_PHONE`) | Direct dial shortcuts placed on the home screen | The first time you tap one |
| Post notifications | The "Test notification badge" row in Settings | Only when you tap that row |
| Private space apps (`ACCESS_HIDDEN_PROFILES`) | Showing Android 15+ private space apps while Kilonova is your home app | Granted at install |
| Set wallpaper, close apps, request uninstall | Setting a wallpaper you picked, the optional Close app row, and uninstalling an app you chose | Granted at install; used only when you take that action |

## What Kilonova stores locally

Your layout, settings, icons, folders, drawer groups, automatic backups and
a crash log are kept in Kilonova's private app storage on your phone.
Uninstalling Kilonova, or clearing its storage, deletes them.

## When something leaves your phone

- **Store categories (Drawer groups).** To sort apps by Google Play and
  F-Droid category, Kilonova downloads F-Droid's public app index about
  weekly and asks the Google Play web page of each installed app for its
  category, which sends that app's package name to Google. It runs on
  unmetered networks unless you tap Update now, and the results are cached on
  the phone. Don't use store categories and nothing is sent.

Otherwise only when you do it yourself:

- **Web search.** Tapping a web search result opens your browser with your
  query on the search engine you picked. From then on that search engine's
  and that browser's privacy policies apply.
- **Sharing.** Export backup and Share crash log hand a file to the app you
  choose. The crash log holds the error, the app version and the device
  model, and is sent only if you share it.
- **Report an issue** opens a new GitHub issue in your browser with the app
  version, Android version and device model filled in. You can edit or
  remove these before posting.
- **Android backup.** If Android's own backup is on, Android may include
  Kilonova's settings in your device backup (for example to your Google
  account) or copy them to a new phone. Kilonova doesn't send these
  itself; your device backup settings control it.

## Children

Kilonova is not aimed at children and collects no data from anyone.

## Changes

Changes to this policy are published in this file in the source repository,
with a new effective date. Its history is public.

## Contact

Questions: open an issue at https://github.com/tommahu-arch/Kilonova/issues.
