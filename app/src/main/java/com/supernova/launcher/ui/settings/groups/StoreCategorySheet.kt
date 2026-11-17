package com.supernova.launcher.ui.settings.groups

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.text.InputType
import android.view.inputmethod.EditorInfo
import androidx.core.widget.doAfterTextChanged
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

        // What the list shows: the entries whose name holds the search, a
        // section header only with something under it. Ticks live in
        // `picked`, not in the list, so they survive a search.
        var shown: List<Entry> = entries
        fun filter(query: String) {
            val words = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
            val hits = entries.filter { entry ->
                entry is Entry.Item && words.all { entry.label.lowercase().contains(it) }
            }
            shown = entries.filterIndexed { index, entry ->
                entry is Entry.Item && entry in hits ||
                    entry is Entry.Header && entries.drop(index + 1).takeWhile { it !is Entry.Header }
                        .any { it in hits }
            }
        }

        val list = ListView(context)
        list.divider = null
        lateinit var adapter: ArrayAdapter<Entry>
        adapter = object : ArrayAdapter<Entry>(context, android.R.layout.simple_list_item_multiple_choice) {
            override fun getCount() = shown.size
            override fun getItem(position: Int) = shown[position]
            override fun getViewTypeCount() = 2
            override fun getItemViewType(position: Int) = if (shown[position] is Entry.Header) 0 else 1
            override fun isEnabled(position: Int) = shown[position] is Entry.Item

            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as CheckedTextView
                when (val entry = shown[position]) {
                    is Entry.Header -> {
                        view.setText(entry.title)
                        view.checkMarkDrawable = null
                        view.setTypeface(null, Typeface.BOLD)
                        view.alpha = 1f
                    }
                    is Entry.Item -> {
                        view.setTypeface(null, Typeface.NORMAL)
                        val taken = entry.owner != null && entry.key !in picked
                        view.text = if (taken) {
                            entry.label + "  ·  " + context.getString(R.string.store_taken_by, entry.owner)
                        } else {
                            entry.label
                        }
                        view.isChecked = entry.key in picked
                        view.alpha = if (taken) GREYED else 1f
                    }
                }
                return view
            }
        }
        list.adapter = adapter
        list.setOnItemClickListener { _, _, position, _ ->
            val entry = shown[position] as? Entry.Item ?: return@setOnItemClickListener
            when {
                entry.key in picked -> picked.remove(entry.key)
                entry.owner == null -> picked.add(entry.key)
                else -> {
                    // Held by another group: only once confirmed.
                    MaterialAlertDialogBuilder(context)
                        .setTitle(context.getString(R.string.store_move_title, entry.owner))
                        .setMessage(
                            context.getString(R.string.store_move_message, entry.label, entry.owner, group.name)
                        )
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.store_move) { _, _ ->
                            picked.add(entry.key)
                            adapter.notifyDataSetChanged()
                        }
                        .show()
                }
            }
            adapter.notifyDataSetChanged()
        }

        val search = EditText(context).apply {
            setHint(R.string.store_search_hint)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            doAfterTextChanged {
                filter(it?.toString().orEmpty())
                adapter.notifyDataSetChanged()
            }
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (context.resources.displayMetrics.density * 20).toInt()
            setPadding(pad, 0, pad, 0)
            addView(search, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }

        val dialog: AlertDialog = MaterialAlertDialogBuilder(context)
            .setTitle(
                context.getString(
                    if (store == Store.PLAY) R.string.store_picker_play else R.string.store_picker_fdroid
                ) + " · " + group.name
            )
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.store_done) { _, _ -> onDone(scope, picked.toSet()) }
            .create()
        dialog.show()
    }

    private const val GREYED = 0.4f
}
