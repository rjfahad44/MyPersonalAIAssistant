package com.bitbytestudio.mypersonalaiassistant.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bitbytestudio.mypersonalaiassistant.ChatUiMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking

object AppDataStore {
    private val Context.dataStore by preferencesDataStore("chat_prefs")
    private val CHAT_HISTORY = stringPreferencesKey("chat_history")
    private val MODEL_PATH = stringPreferencesKey("model_path")

    suspend fun saveChatHistory(context: Context, messages: List<ChatUiMessage>) {
        val json = Gson().toJson(messages)
        context.dataStore.edit { it[CHAT_HISTORY] = json }
    }

    fun getChatHistory(context: Context): Flow<List<ChatUiMessage>> = context.dataStore.data
        .map { prefs ->
            prefs[CHAT_HISTORY]?.let {
                Gson().fromJson(it, object : TypeToken<List<ChatUiMessage>>() {}.type)
            } ?: emptyList()
        }

    fun clearChatHistory(context: Context) = runBlocking {
        context.dataStore.edit { it.remove(CHAT_HISTORY) }
    }

    fun saveModelPath(context: Context, path: String) = runBlocking {
        context.dataStore.edit { it[MODEL_PATH] = path }
    }

    fun getModelPath(context: Context): Flow<String?> = context.dataStore.data
        .map { it[MODEL_PATH] }
}
