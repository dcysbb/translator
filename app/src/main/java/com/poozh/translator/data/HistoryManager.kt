package com.poozh.translator.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class HistoryItem(
    val id: String,
    val timestamp: Long,
    val originalText: String,
    val translatedText: String,
    val providerId: String
)

object HistoryManager {
    private const val FILE_NAME = "translation_history.json"
    private const val MAX_HISTORY_ITEMS = 50

    @Synchronized
    fun getHistory(context: Context): List<HistoryItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()
        return try {
            val jsonStr = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        originalText = obj.optString("originalText", ""),
                        translatedText = obj.optString("translatedText", ""),
                        providerId = obj.optString("providerId", "")
                    )
                )
            }
            list.sortByDescending { it.timestamp }
            list
        } catch (e: Exception) {
            android.util.Log.e("HistoryManager", "Failed to load history", e)
            emptyList()
        }
    }

    @Synchronized
    fun addHistory(context: Context, original: String, translated: String, providerId: String) {
        if (original.isBlank() || translated.isBlank()) return
        val list = getHistory(context).toMutableList()
        
        // Remove if a history item with exact same original text already exists (to prevent spam/duplicates)
        val duplicateIndex = list.indexOfFirst { it.originalText.trim() == original.trim() }
        if (duplicateIndex != -1) {
            list.removeAt(duplicateIndex)
        }

        val newItem = HistoryItem(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            originalText = original,
            translatedText = translated,
            providerId = providerId
        )
        list.add(0, newItem)
        
        val trimmedList = if (list.size > MAX_HISTORY_ITEMS) {
            list.subList(0, MAX_HISTORY_ITEMS)
        } else {
            list
        }
        
        saveList(context, trimmedList)
    }

    @Synchronized
    fun deleteHistoryItem(context: Context, id: String) {
        val list = getHistory(context).filter { it.id != id }
        saveList(context, list)
    }

    @Synchronized
    fun clearHistory(context: Context) {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun saveList(context: Context, list: List<HistoryItem>) {
        try {
            val jsonArray = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("timestamp", item.timestamp)
                    put("originalText", item.originalText)
                    put("translatedText", item.translatedText)
                    put("providerId", item.providerId)
                }
                jsonArray.put(obj)
            }
            val file = File(context.filesDir, FILE_NAME)
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (e: Exception) {
            android.util.Log.e("HistoryManager", "Failed to save history", e)
        }
    }
}
