package com.supernova.launcher.ui.settings.groups

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R
import com.supernova.launcher.data.DrawerGroup
import com.supernova.launcher.data.StoreCategories

// The Play / F-Droid button's picker: every category of that store as a
// tick, the group's own ticked. A category another group holds is greyed
// and says whose it is; ticking it asks first, and Done moves it here. A
// category sorts into one group only.
//
// Added for Kilonova; not part of upstream Supernova.
object StoreCategorySheet {

    enum class Store { PLAY, FDROID }

    private sealed interface Entry {
        class Header(val title: Int) : Entry
        class Item(val key: String, val label: String, val owner: String?) : Entry
    }

    // others: the groups besides this one, for who holds what.
    fun show(
        context: Context,
        store: Store,
        group: DrawerGroup,
        others: List<DrawerGroup>,
        onDone: (scope: Set<String>, picked: Set<String>) -> Unit
    ) {
        val owner = HashMap<String, String>()
        others.forEach { other -> other.storeCats.forEach { owner.putIfAbsent(it, other.name) } }
        fun item(key: String, label: String = StoreCategories.label(key)) = Entry.Item(key, label, owner[key])

        val entries = buildList {
            when (store) {
                Store.PLAY -> {
                    add(Entry.Header(R.string.store_section_apps))
                    StoreCategories.playCategories.filterNot { it.game }
                        .forEach { add(item(StoreCategories.play(it.id), it.label)) }
                    add(Entry.Header(R.string.store_section_games))
                    StoreCategories.playCategories.filter { it.game }
                        .forEach { add(item(StoreCategories.play(it.id), it.label)) }
                }
                Store.FDROID -> {
                    add(Entry.Header(R.string.store_section_narrow))
                    StoreCategories.fdroidNarrow.forEach { add(item(StoreCategories.fdroid(it))) }
                    add(Entry.Header(R.string.store_section_broad))
                    StoreCategories.fdroidBroad.sorted().forEach { add(item(StoreCategories.fdroid(it))) }
                }
            }
            add(Entry.Header(R.string.store_section_left_over))
            add(item(StoreCategories.SAMSUNG))
            add(item(StoreCategories.UNSORTED))
        }
        val items = entries.filterIsInstance<Entry.Item>()
        val scope = items.map { it.key }.toSet()
        // Ticks the user confirmed to take from another group.
        val picked = items.filter { it.key in group.storeCats }.map { it.key }.toMutableSet()

        val list = ListView(context)
        list.choiceMode = ListView.CHOICE_MODE_MULTIPLE
        list.divider = null
        lateinit var adapter: ArrayAdapter<Entry>
        adapter = object : ArrayAdapter<Entry>(context, android.R.layout.simple_list_item_multiple_choice, entries) {
            override fun getViewTypeCount() = 2
            override fun getItemViewType(position: Int) = if (getItem(position) is Entry.Header) 0 else 1
            override fun isEnabled(position: Int) = getItem(position) is Entry.Item

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as CheckedTextView
                when (val entry = getItem(position)) {
                    is Entry.Header -> {
                        view.setText(entry.title)
                        view.checkMarkDrawable = null
                        view.setTypeface(null, Typeface.BOLD)
                        view.alpha = 1f
                    }
                    is Entry.Item -> {
                        view.setTypeface(null, Typeface.NORMAL)
                        view.text = if (entry.owner == null || entry.key in picked) {
                            entry.label
                        } else {
                            entry.label + "  ·  " + context.getString(R.string.store_taken_by, entry.owner)
                        }
                        view.alpha = if (entry.owner != null && entry.key !in picked) GREYED else 1f
                    }
                    null -> Unit
                }
                return view
            }
        }
        list.adapter = adapter
        entries.forEachIndexed { index, entry ->
            if (entry is Entry.Item && entry.key in picked) list.setItemChecked(index, true)
        }
        list.setOnItemClickListener { _, _, position, _ ->
            val entry = entries[position] as? Entry.Item ?: return@setOnItemClickListener
            val checked = list.isItemChecked(position)
            when {
                !checked -> picked.remove(entry.key)
                entry.owner == null || entry.key in picked -> picked.add(entry.key)
                else -> {
                    // Held by another group: undone until confirmed.
                    list.setItemChecked(position, false)
                    MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.store_move_title, entry.owner))
                        .setMessage(
                            context.getString(R.string.store_move_message, entry.label, entry.owner, group.name)
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.store_move) { _, _ ->
                            picked.add(entry.key)
                            list.setItemChecked(position, true)
                            adapter.notifyDataSetChanged()
                        }
                        .show()
                }
            }
            adapter.notifyDataSetChanged()
        }

        val dialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setTitle(
                context.getString(
                    if (store == Store.PLAY) R.string.store_picker_play else R.string.store_picker_fdroid
                ) + " · " + group.name
            )
            .setView(list)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.store_done) { _, _ -> onDone(scope, picked.toSet()) }
            .create()
        dialog.show()
    }

    private const val GREYED = 0.4f
}
