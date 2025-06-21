package com.bitbytestudio.mypersonalaiassistant

import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.util.Log
import androidx.annotation.Keep
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitbytestudio.mypersonalaiassistant.utils.AppDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


//@HiltViewModel
//class ChatViewModel @Inject constructor(
//    @ApplicationContext private val context: Context,
//) : ViewModel() {
//
//    private val llama = LLamaAndroid.instance()
//
//    private val _isThinking = MutableStateFlow(false)
//    val isThinking: StateFlow<Boolean> = _isThinking
//
//    private val _messages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
//    val messages: StateFlow<List<Pair<String, Boolean>>> = _messages
//
//    private var job: Job? = null
//    private val _isLoadingModel = MutableStateFlow(false)
//    val isLoadingModel: StateFlow<Boolean> = _isLoadingModel
//
//    var modelName = mutableStateOf("")
//
//    private val SYSTEM_PROMPT = """
//        You are Jarvis, a helpful and intelligent AI assistant developed by Fahad Alam.
//
//        Your tasks include:
//        - Answering questions clearly and concisely
//        - Assisting with programming (Kotlin, Java, Python, etc.)
//        - Helping with writing, research, and productivity
//        - Acting as a personal assistant: polite, honest, and direct
//
//        Guidelines:
//        - Respond in a friendly, conversational tone
//        - Be accurate, logical, and avoid making things up
//        - Use markdown for code blocks or formatting if needed
//        - Do not rely on internet access — all answers must be self-contained
//    """.trimIndent()
//
//    private var isFirstTime = true
//
//    init {
//        observeStoredData()
//    }
//
//    private fun observeStoredData() {
//        viewModelScope.launch {
//            AppDataStore.getModelPath(context).collectLatest { path ->
//                if (!path.isNullOrBlank()) {
//                    runCatching {
//                        llama.load(path)
//                        modelName.value = File(path).name
//                    }.onFailure {
//                        Log.e("ChatViewModel", "Failed to auto-load model", it)
//                    }
//                }
//            }
//        }
//
//        viewModelScope.launch {
//            AppDataStore.getChatHistory(context).collectLatest { list ->
//                _messages.value = list
//                isFirstTime = list.isEmpty()
//                Log.d("ChatViewModel", "Loaded chat history: $list")
//            }
//        }
//    }
//
//    fun loadModelFromUri(uri: Uri) {
//        viewModelScope.launch {
//            _isLoadingModel.value = true
//            try {
//                withContext(Dispatchers.IO) {
//                    llama.unload()
//                    val modelFile = File(context.cacheDir, "model.gguf")
//                    context.contentResolver.openInputStream(uri)?.use { input ->
//                        FileOutputStream(modelFile).use { output ->
//                            input.copyTo(output)
//                        }
//                    }
//                    llama.load(modelFile.absolutePath)
//                    AppDataStore.saveModelPath(context, modelFile.absolutePath)
//                    modelName.value = modelFile.name
//                }
//            } catch (e: Exception) {
//                Log.e("ModelLoad", "Error loading model", e)
//            } finally {
//                _isLoadingModel.value = false
//            }
//        }
//    }
//
//    fun sendMessage(prompt: String) {
//        _messages.update { it + (prompt to true) }
//        job?.cancel()
//        _isThinking.value = true
//        job = viewModelScope.launch {
//            val fullPrompt = buildFullPrompt(prompt)
//            llama.send(fullPrompt, formatChat = false).collect { token ->
//                _messages.update { current ->
//                    val last = current.lastOrNull()
//                    if (last != null && !last.second) {
//                        current.dropLast(1) + ((last.first + token) to false)
//                    } else {
//                        current + (token to false)
//                    }
//                }
//                AppDataStore.saveChatHistory(context, _messages.value)
//            }
//            isFirstTime = false
//            _isThinking.value = false
//        }
//    }
//
//    private fun buildFullPrompt(prompt: String): String {
//        return buildString {
//            if (isFirstTime) {
//                append("[INST] <<SYS>>\n")
//                append(SYSTEM_PROMPT)
//                append("\n<</SYS>>\n\n")
//            }
//            append("[INST] ")
//            append(prompt.trim())
//            append(" [/INST]")
//        }
//    }
//
//    fun cancelGeneration() {
//        job?.cancel()
//        _isThinking.value = false
//        llama.stopThinking()
//    }
//
//    fun clearChat() {
//        _messages.value = emptyList()
//        isFirstTime = true
//        viewModelScope.launch {
//            llama.clearHistory()
//            AppDataStore.clearChatHistory(context)
//        }
//    }
//
//    override fun onCleared() {
//        super.onCleared()
//        viewModelScope.launch {
//            llama.unload()
//        }
//    }
//}



@Keep
data class ChatUiMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val llama = LLamaAndroid.instance()
    private val SYSTEM_PROMPT = """
        You are Jarvis, an intelligent and concise AI assistant created by Fahad.
        Your job is to provide accurate answers.
    """.trimIndent()

    private val _messages = MutableStateFlow<List<ChatUiMessage>>(emptyList())
    val messages: StateFlow<List<ChatUiMessage>> = _messages

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private var _isLoadingModel = MutableStateFlow(false)
    val isLoadingModel: StateFlow<Boolean> = _isLoadingModel

    var modelName = mutableStateOf("")
    private var job: Job? = null

    init {
        viewModelScope.launch {
            AppDataStore.getModelPath(context).collectLatest { path ->
                if (!path.isNullOrBlank()) {
                    runCatching {
                        llama.load(path)
                        llama.setSystemPrompt(SYSTEM_PROMPT)
                        modelName.value = File(path).name
                    }
                }
            }
        }

        viewModelScope.launch {
            AppDataStore.getChatHistory(context).collectLatest { list ->
                _messages.value = list
            }
        }
    }

    fun sendMessage(prompt: String) {
        _messages.update { it + ChatUiMessage(prompt, true) + ChatUiMessage("", false) }
        job?.cancel()
        _isThinking.value = true

        job = viewModelScope.launch {
            val replyBuilder = StringBuilder()
            llama.send(prompt).collect { token ->
                replyBuilder.append(token)
                _messages.update { current ->
                    current.dropLast(1) + ChatUiMessage(replyBuilder.toString(), false, System.currentTimeMillis())
                }
                AppDataStore.saveChatHistory(context, _messages.value)
            }
            _isThinking.value = false
        }
    }

    fun cancelGeneration() {
        job?.cancel()
        _isThinking.value = false
        llama.stopThinking()
    }

    fun clearChat() {
        _messages.value = emptyList()
        viewModelScope.launch {
            llama.clearHistory()
            AppDataStore.clearChatHistory(context)
        }
    }

    fun loadModelFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoadingModel.value = true
            withContext(Dispatchers.IO) {
                llama.unload()

                //Delete all old .gguf files from cacheDir
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.extension == "gguf") {
                        file.delete()
                    }
                }

                //Copy new model
                val modelFile = File(context.cacheDir, fileName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }

                //Load new model
                llama.load(modelFile.absolutePath)
                llama.setSystemPrompt(SYSTEM_PROMPT)
                AppDataStore.saveModelPath(context, modelFile.absolutePath)
                modelName.value = modelFile.name
                _isLoadingModel.value = false
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            llama.unload()
        }
    }
}

