package com.supernova.launcher.store

import android.util.JsonReader
import java.io.InputStream

// F-Droid's index-v2.json read as a stream, keeping only each package's
// categories: the file is tens of megabytes, and what it says of versions,
// signatures and translations is of no use here.
//
// Added for Kilonova; not part of upstream Supernova.
object FdroidIndexParser {

    fun categories(input: InputStream): Map<String, List<String>> {
        val found = HashMap<String, List<String>>()
        JsonReader(input.bufferedReader()).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "packages") packages(reader, found) else reader.skipValue()
            }
            reader.endObject()
        }
        return found
    }

    private fun packages(reader: JsonReader, into: MutableMap<String, List<String>>) {
        reader.beginObject()
        while (reader.hasNext()) {
            val id = reader.nextName()
            val cats = pack(reader)
            if (cats.isNotEmpty()) into[id] = cats
        }
        reader.endObject()
    }

    private fun pack(reader: JsonReader): List<String> {
        var cats = emptyList<String>()
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "metadata") cats = metadata(reader) else reader.skipValue()
        }
        reader.endObject()
        return cats
    }

    private fun metadata(reader: JsonReader): List<String> {
        var cats = emptyList<String>()
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() == "categories") {
                cats = buildList {
                    reader.beginArray()
                    while (reader.hasNext()) add(reader.nextString())
                    reader.endArray()
                }
            } else {
                reader.skipValue()
            }
        }
        reader.endObject()
        return cats
    }
}
