package com.bitbytestudio.mypersonalaiassistant.utils

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object AppDataStore {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

    private val KEY_MODEL_PATH = stringPreferencesKey("model_path")
    private val KEY_CHAT_HISTORY = stringPreferencesKey("chat_history")

    suspend fun saveModelPath(context: Context, path: String) {
        context.dataStore.edit { it[KEY_MODEL_PATH] = path }
    }

    fun getModelPath(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_MODEL_PATH] }

    suspend fun saveChatHistory(context: Context, messages: List<Pair<String, Boolean>>) {
        val serialized = messages.joinToString("||") {
            val encoded = Base64.encodeToString(it.first.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "${if (it.second) "U" else "B"}:$encoded"
        }
        context.dataStore.edit { it[KEY_CHAT_HISTORY] = serialized }
    }

    fun getChatHistory(context: Context): Flow<List<Pair<String, Boolean>>> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_CHAT_HISTORY]?.split("||")?.mapNotNull {
                val parts = it.split(":", limit = 2)
                if (parts.size == 2) {
                    val decoded = Base64.decode(parts[1], Base64.NO_WRAP).toString(Charsets.UTF_8)
                    decoded to (parts[0] == "U")
                } else null
            } ?: emptyList()
        }

    suspend fun clearChatHistory(context: Context) {
        context.dataStore.edit { it.remove(KEY_CHAT_HISTORY) }
    }
}
