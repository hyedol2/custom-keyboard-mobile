package com.example.customkeyboard.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class LayoutRepository(context: Context) {
    private val file = File(context.filesDir, "layouts.json")
    private val gson = Gson()

    fun loadAll(): MutableList<KeyboardLayout> {
        if (!file.exists()) return mutableListOf()
        return try {
            val type = object : TypeToken<MutableList<KeyboardLayout>>() {}.type
            gson.fromJson(file.readText(), type) ?: mutableListOf()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    fun saveAll(layouts: List<KeyboardLayout>) {
        file.writeText(gson.toJson(layouts))
    }

    fun upsert(layout: KeyboardLayout) {
        val all = loadAll()
        val idx = all.indexOfFirst { it.id == layout.id }
        if (idx >= 0) all[idx] = layout else all.add(layout)
        saveAll(all)
    }

    fun delete(layoutId: String) {
        val all = loadAll()
        all.removeAll { it.id == layoutId }
        saveAll(all)
    }
}
