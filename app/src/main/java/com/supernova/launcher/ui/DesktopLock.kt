package com.supernova.launcher.ui

import android.content.Context
import android.widget.Toast
import com.supernova.launcher.R

// Lock layout: nothing on the home screen is added, moved, removed or
// re-paged, by drag or by tap. Every path that would change it asks here
// first, so a refusal always says why rather than doing nothing.
object DesktopLock {

    // Lock dock: an item in the dock, or in a folder in it, stays where it is.
    fun dockLocks(
        prefs: com.supernova.launcher.data.LauncherPrefs,
        model: com.supernova.launcher.data.LauncherModel,
        item: com.supernova.launcher.data.LauncherItem?
    ): Boolean {
        if (!prefs.dockLocked || item == null) return false
        if (item.container == com.supernova.launcher.data.ContainerType.DOCK) return true
        val parent = item.folderId?.let { id -> model.items.firstOrNull { it.id == id } }
        return parent?.container == com.supernova.launcher.data.ContainerType.DOCK
    }

    // True, with the "locked" message, when the layout is locked.
    fun refused(context: Context, viewModel: LauncherViewModel): Boolean {
        if (!viewModel.state.value.prefs.desktopLocked) return false
        Toast.makeText(context, R.string.desktop_locked_toast, Toast.LENGTH_SHORT).show()
        return true
    }
}
