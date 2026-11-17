package com.supernova.launcher.ui.settings.groups

import android.content.Context
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.BaseAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.supernova.launcher.R
import com.supernova.launcher.data.LabelOrder
import com.supernova.launcher.data.StoreCategories
import com.supernova.launcher.ui.LauncherViewModel

// "View apps metadata": every installed app with what the stores said of it
// (its Play category, its F-Droid categories) and the group it landed in,
// with a search over the name, package, categories and group.
//
// Added for Kilonova; not part of upstream Supernova.
object StoreMetadataSheet {

    private class Row(val title: String, val body: String, val haystack: String)

    fun show(context: Context, viewModel: LauncherViewModel) {
        val cache = viewModel.store.cache
        val groups = viewModel.drawerGroups()
        val apps = LabelOrder.sorted(viewModel.visibleApps()) { it.label }
        val rows = apps.distinctBy { it.packageName }.map { app ->
            val details = cache.details(app.packageName)
            val play = when {
                details.playCategory != null -> StoreCategories.label(StoreCategories.play(details.playCategory)) +
                    " (" + details.playCategory + ")"
                details.playAsked -> context.getString(R.string.store_metadata_none)
                else -> context.getString(R.string.store_metadata_not_asked)
            }
            val fdroid = details.fdroid.joinToString(", ").ifEmpty { context.getString(R.string.store_metadata_none) }
            val held = groups.filter { group ->
                apps.any { it.packageName == app.packageName && viewModel.hiddenKey(it) in group.members }
            }.joinToString(", ") { it.name }.ifEmpty { context.getString(R.string.store_metadata_ungrouped) }
            Row(
                title = app.label + "\n" + app.packageName,
                body = context.getString(R.string.store_metadata_line, play, fdroid, held),
                haystack = listOf(app.label, app.packageName, play, fdroid, held).joinToString(" ").lowercase()
            )
        }

        var shown = rows
        val list = ListView(context)
        val adapter = object : BaseAdapter() {
            override fun getCount() = shown.size
            override fun getItem(position: Int) = shown[position]
            override fun getItemId(position: Int) = position.toLong()
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView
                    ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_2, parent, false)
                val row = shown[position]
                view.findViewById<TextView>(android.R.id.text1).apply {
                    text = row.title
                    textSize = TITLE_SP
                }
                view.findViewById<TextView>(android.R.id.text2).apply {
                    text = row.body
                    textSize = BODY_SP
                }
                return view
            }
        }
        list.adapter = adapter
        list.isFastScrollEnabled = true

        val search = EditText(context).apply {
            setHint(R.string.store_metadata_search)
            setSingleLine()
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            doAfterTextChanged {
                val words = it?.toString().orEmpty().trim().lowercase().split(' ').filter { w -> w.isNotEmpty() }
                shown = rows.filter { row -> words.all { w -> w in row.haystack } }
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
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.store_metadata_title)
            .setView(content)
            .setPositiveButton(R.string.store_metadata_close, null)
            .show()
    }

    private const val TITLE_SP = 15f
    private const val BODY_SP = 13f
}
