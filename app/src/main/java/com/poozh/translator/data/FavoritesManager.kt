package com.poozh.translator.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class FavoriteItem(
    val id: String,
    val timestamp: Long,
    val word: String,
    val reading: String = "",
    val meaning: String = "",
    val note: String = "",
    val context: String = ""
)

object FavoritesManager {
    private const val FILE_NAME = "favorites.json"
    private const val MAX_ITEMS = 500

    @Synchronized
    fun getFavorites(context: Context): List<FavoriteItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<FavoriteItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    FavoriteItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        word = obj.optString("word", ""),
                        reading = obj.optString("reading", ""),
                        meaning = obj.optString("meaning", ""),
                        note = obj.optString("note", ""),
                        context = obj.optString("context", "")
                    )
                )
            }
            list.sortByDescending { it.timestamp }
            list
        } catch (e: Exception) {
            android.util.Log.e("FavoritesManager", "Failed to load favorites", e)
            emptyList()
        }
    }

    @Synchronized
    fun addFavorite(
        context: Context,
        word: String,
        reading: String = "",
        meaning: String = "",
        note: String = "",
        sourceContext: String = ""
    ): Boolean {
        if (word.isBlank()) return false
        val list = getFavorites(context).toMutableList()
        // De-duplicate by word surface.
        if (list.any { it.word == word }) return false
        list.add(0, FavoriteItem(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            word = word,
            reading = reading,
            meaning = meaning,
            note = note,
            context = sourceContext
        ))
        val trimmed = if (list.size > MAX_ITEMS) list.subList(0, MAX_ITEMS) else list
        saveList(context, trimmed)
        return true
    }

    @Synchronized
    fun removeFavorite(context: Context, id: String) {
        val list = getFavorites(context).filter { it.id != id }
        saveList(context, list)
    }

    @Synchronized
    fun isFavorite(context: Context, word: String): Boolean {
        return getFavorites(context).any { it.word == word }
    }

    @Synchronized
    fun clearFavorites(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.delete()
    }

    private fun saveList(context: Context, list: List<FavoriteItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("word", item.word)
                    put("reading", item.reading)
                    put("meaning", item.meaning)
                    put("note", item.note)
                    put("context", item.context)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("FavoritesManager", "Failed to save favorites", e)
        }
    }
}
