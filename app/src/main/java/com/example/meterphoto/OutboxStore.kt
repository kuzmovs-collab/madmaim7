package com.example.meterphoto

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Очередь фото, ожидающих отправки (JSON в SharedPreferences). */
class OutboxStore(context: Context) {

    data class Item(val filePath: String, val number: String, val addedAt: Long)

    private val prefs: SharedPreferences =
        context.getSharedPreferences("outbox", Context.MODE_PRIVATE)

    fun all(): List<Item> {
        val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
        val result = mutableListOf<Item>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(Item(o.getString("file"), o.getString("number"), o.getLong("added")))
        }
        return result
    }

    fun enqueue(filePath: String, number: String) {
        val arr = JSONArray(prefs.getString("items", "[]") ?: "[]")
        arr.put(JSONObject().apply {
            put("file", filePath)
            put("number", number)
            put("added", System.currentTimeMillis())
        })
        prefs.edit().putString("items", arr.toString()).apply()
    }

    fun remove(filePath: String) {
        val old = JSONArray(prefs.getString("items", "[]") ?: "[]")
        val arr = JSONArray()
        for (i in 0 until old.length()) {
            if (old.getJSONObject(i).getString("file") != filePath) arr.put(old.getJSONObject(i))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    val count: Int get() = all().size
}
