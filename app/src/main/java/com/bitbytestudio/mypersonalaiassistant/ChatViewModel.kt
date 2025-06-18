package com.bitbytestudio.mypersonalaiassistant

import android.app.Application
import android.content.Context
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject


@HiltViewModel
class ChatViewModel @Inject constructor(
    private val appContext: Application,
) : ViewModel() {

    private val llama = LLamaAndroid.instance()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking

    private val _messages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val messages: StateFlow<List<Pair<String, Boolean>>> = _messages

    private var job: Job? = null
    private val _isLoadingModel = MutableStateFlow(false)

    val isLoadingModel: StateFlow<Boolean> = _isLoadingModel.asStateFlow()
    var modelName = mutableStateOf("")

    fun loadModelFromUri(context: Context, uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoadingModel.value = true
            try {
                withContext(Dispatchers.IO) {
                    val modelFile = File(context.cacheDir, fileName)
                    // Scoped storage safe copy
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(modelFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    llama.load(modelFile.absolutePath) // also runs on dedicated dispatcher in LLamaAndroid
                }
            } catch (e: Exception) {
                Log.e("ModelLoad", "Failed to load model", e)
            } finally {
                _isLoadingModel.value = false
            }
        }
    }


    fun loadModel() {
        viewModelScope.launch(Dispatchers.Default) {
            //val downloadUrl = "https://huggingface.co/unsloth/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-BF16.gguf?download=true"
            //val modelFile = appContext.downloadModelToFile("gemma-3-1b-it-BF16.gguf", "gemma_3_1b.gguf").absolutePath
//            val mode lFile = copyAssetToFile(
//                /*your model name*/ "Qwen3-0.6B-Q8_0.gguf",
//                /*copy model name*/"Qwen3.gguf")
//                .absolutePath
            modelPath()?.let {
                LLamaAndroid.instance().load(it)
            }?:run {
                Log.e("fahad007", "Ai Model not found")
            }
        }
    }

    fun modelPath(): String? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val modelFile = File(downloadsDir, "MODEL_NAME")
        return if (modelFile.exists()) {
            modelFile.absolutePath
        } else {
            null
        }
    }


    private val systemPrompt = """
You are Jarvis, a helpful, honest, and intelligent AI assistant created by Fahad Alam. Your job is to provide clear, thoughtful answers to any questions the user may ask. You can help with code, writing, daily tasks, and more. Be conversational and friendly while remaining informative.
""".trimIndent()

    private val formatChat = true
    private var isFirstTime = true

    fun sendMessage(prompt: String) {
        _messages.update { it + (prompt to true) }
        job?.cancel()
        _isThinking.value = true
        job = viewModelScope.launch {
            val fullPrompt = if (isFirstTime) {
                buildString {
                    append("[INST] <<SYS>>\n")
                    append(systemPrompt)
                    append("\n<</SYS>>\n\n")
                    append(prompt)
                    append(" [/INST]")
                }
            } else {
                prompt
            }

            LLamaAndroid.instance().send(fullPrompt, formatChat = formatChat).collect { token ->
                _messages.update { current ->
                    val last = current.lastOrNull()
                    if (last != null && !last.second) {
                        current.dropLast(1) + ((last.first + token) to false)
                    } else {
                        current + (token to false)
                    }
                }
            }
            isFirstTime = false
            _isThinking.value = false
        }
    }

    fun cancelGeneration() {
        job?.cancel()
        _isThinking.value = false
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            LLamaAndroid.instance().unload()
        }
    }
}
