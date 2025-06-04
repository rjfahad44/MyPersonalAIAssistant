package com.bitbytestudio.mypersonalaiassistant

import android.app.Application
import android.llama.cpp.LLamaAndroid
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bitbytestudio.mypersonalaiassistant.utils.downloadModelToFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@HiltViewModel
class ChatViewModel @Inject constructor(
    private val appContext: Application,
) : ViewModel() {

    init {
        loadModel()
    }

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private val _messages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val messages: StateFlow<List<Pair<String, Boolean>>> = _messages

    private var job: Job? = null

    fun loadModel() {

        viewModelScope.launch(Dispatchers.Default) {
            //val downloadUrl = "https://huggingface.co/unsloth/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-BF16.gguf?download=true"
            //val modelFile = appContext.downloadModelToFile("gemma-3-1b-it-BF16.gguf", "gemma_3_1b.gguf").absolutePath
            val modelFile = copyAssetToFile(
                /*your model name*/ "Qwen3-0.6B-Q8_0.gguf",
                /*copy model name*/"Qwen3.gguf")
                .absolutePath
            Log.d("fahad007", "Model file path: ${modelFile}")
            LLamaAndroid.instance().load(modelFile)
        }
    }

    private val systemPrompt = """
You are Jarvis, a helpful, honest, and intelligent AI assistant created by Fahad Alam. Your job is to provide clear, thoughtful answers to any questions the user may ask. You can help with code, writing, daily tasks, and more. Be conversational and friendly while remaining informative.
""".trimIndent()

    private val formatChat = true

    fun sendMessage(prompt: String) {
        _messages.update { it + (prompt to true) }
        job?.cancel()
        _isThinking.value = true

        job = viewModelScope.launch {
            val chatHistory = buildChatHistoryGemma3(prompt)
            //val chatHistory = buildChatHistory(prompt)
            //val mm = "<|system|>\n$systemPrompt\n\n\n<|user|>\n$prompt"
            LLamaAndroid.instance().send(chatHistory, formatChat = formatChat).collect { token ->
                _messages.update { current ->
                    val last = current.lastOrNull()
                    if (last != null && !last.second) {
                        current.dropLast(1) + ((last.first + token) to false)
                    } else {
                        current + (token to false)
                    }
                }
            }
            _isThinking.value = false
        }
    }


//    fun sendMessage(prompt: String) {
//        _messages.update { it + (prompt to true) }
//        job?.cancel()
//        _isThinking.value = true
//
//        job = viewModelScope.launch {
//            val m = "<|system|>\n$systemPrompt\n\n\n<|user|>\n$prompt"
//            LLamaAndroid.instance().send(m, formatChat = false).collect { token ->
//                _messages.update { current ->
//                    val last = current.lastOrNull()
//                    if (last != null && !last.second) {
//                        current.dropLast(1) + ((last.first + token) to false)
//                    } else {
//                        current + (token to false)
//                    }
//                }
//            }
//            _isThinking.value = false
//        }
//    }

    private fun buildChatHistoryGemma3(currentPrompt: String): String {
        if (!formatChat) return currentPrompt
        val history = StringBuilder()
        val messages = _messages.value

        // Inject the system prompt as an internal assistant reply (not user)
        if (messages.isEmpty()) {
            history.appendLine("<start_of_turn>system")
            history.appendLine(systemPrompt)
            history.appendLine("<end_of_turn>")
            history.appendLine("<start_of_turn>model")
            history.appendLine(systemPrompt)
            history.appendLine("<end_of_turn>")
        }

        for ((message, isUser) in messages) {
            history.appendLine("<start_of_turn>${if (isUser) "user" else "model"}")
            history.appendLine(message)
            history.appendLine("<end_of_turn>")
        }

        history.appendLine("<start_of_turn>user")
        history.appendLine(currentPrompt)
        history.appendLine("<end_of_turn>")
        history.appendLine("<start_of_turn>model")

        return history.toString()
    }

    private fun buildChatHistory(currentPrompt: String): String {
        val history = StringBuilder()
        history.appendLine(systemPrompt)

        val messages = _messages.value

        for ((message, isUser) in messages) {
            if (isUser) {
                history.appendLine("<|user|>\n$message")
            } else {
                history.appendLine("<|system|>\n$message")
            }
        }

        // Add the new user message
        history.appendLine("<|user|>\n$currentPrompt")

        return history.toString()
    }


    private fun copyAssetToFile(assetName: String, targetFileName: String): File {
        val outFile = File(appContext.filesDir, targetFileName)
        if (!outFile.exists()) {
            appContext.assets.open(assetName).use { input ->
                outFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            LLamaAndroid.instance().unload()
        }
    }
}
